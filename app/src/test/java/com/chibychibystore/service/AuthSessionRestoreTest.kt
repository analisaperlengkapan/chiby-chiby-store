package com.chibychibystore.service

import com.chibychibystore.data.local.dao.PenggunaDao
import com.chibychibystore.data.local.dao.PenggunaSessionDao
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.PenggunaSession
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PenggunaSessionRepository
import com.chibychibystore.service.impl.AuthServiceImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

/**
 * Covers [AuthServiceImpl.initializeSession], the cold-start restore that
 * `AppNavigation` now relies on. It must bring back only a session that is still
 * within the idle timeout and whose user is still active; anything else has to
 * stay logged out so the login screen is shown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthSessionRestoreTest {

    private val penggunaDao = mock<PenggunaDao>()
    private val sessionRepository = mock<PenggunaSessionRepository>()
    private val service = AuthServiceImpl(penggunaDao, sessionRepository)

    private val activeUser = Pengguna(
        id = 7,
        username = "cashier",
        passwordHash = "hash",
        role = Role.CASHIER,
        isActive = true
    )

    @Test
    fun `restores a recent active session`() = runTest {
        val session = PenggunaSession(
            id = 1,
            userId = activeUser.id,
            lastActivityTime = Date(System.currentTimeMillis() - 60_000)
        )
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(listOf(session)))
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)

        val result = service.initializeSession()

        assertTrue(result.isSuccess)
        assertSame(activeUser, service.getCurrentUser())
        verify(sessionRepository).activateSession(session.id)
        verify(sessionRepository, never()).deactivateUserSessions(activeUser.id)
    }

    @Test
    fun `restores the freshest session when an older login has gone idle`() = runTest {
        // User B's newer login is idle; user A's older row was used more
        // recently. The previous query returned only the newest login (B) and
        // discarded it without ever looking at A, so the user had to log in again.
        val userA = activeUser.copy(id = 1, username = "a")
        val userB = activeUser.copy(id = 2, username = "b")
        val now = System.currentTimeMillis()
        val freshSession = PenggunaSession(
            id = 10,
            userId = userA.id,
            loginTime = Date(now - 5 * DAY),
            lastActivityTime = Date(now - 60_000)
        )
        val idleSession = PenggunaSession(
            id = 11,
            userId = userB.id,
            loginTime = Date(now - 1 * DAY),
            lastActivityTime = Date(now - AuthServiceImpl.SESSION_TIMEOUT_MS - 1_000)
        )
        // DAO returns rows ordered by lastActivityTime DESC, so the fresh row is first.
        whenever(sessionRepository.getActiveSessions())
            .thenReturn(Result.success(listOf(freshSession, idleSession)))
        whenever(penggunaDao.getPenggunaById(userA.id)).thenReturn(userA)
        whenever(penggunaDao.getPenggunaById(userB.id)).thenReturn(userB)

        service.initializeSession()

        assertSame(userA, service.getCurrentUser())
        verify(sessionRepository).activateSession(freshSession.id)
        // The stale row is older than the restored one, so it is left for a later
        // launch rather than touching another user's session.
        verify(sessionRepository, never()).deactivateSession(any())
    }

    @Test
    fun `closes a rejected row that precedes the restored session`() = runTest {
        // A deleted user's row can still have the most recent activity; it must be
        // closed, not restored, and the next fresh row must be picked instead.
        val now = System.currentTimeMillis()
        val orphanSession = PenggunaSession(
            id = 20,
            userId = 99,
            lastActivityTime = Date(now - 10_000)
        )
        val validSession = PenggunaSession(
            id = 21,
            userId = activeUser.id,
            lastActivityTime = Date(now - 60_000)
        )
        whenever(sessionRepository.getActiveSessions())
            .thenReturn(Result.success(listOf(orphanSession, validSession)))
        whenever(penggunaDao.getPenggunaById(99)).thenReturn(null)
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)

        service.initializeSession()

        assertSame(activeUser, service.getCurrentUser())
        verify(sessionRepository).deactivateSession(orphanSession.id)
        verify(sessionRepository).activateSession(validSession.id)
    }

    @Test
    fun `drops a session idle past the timeout`() = runTest {
        val session = PenggunaSession(
            id = 2,
            userId = activeUser.id,
            lastActivityTime = Date(System.currentTimeMillis() - AuthServiceImpl.SESSION_TIMEOUT_MS - 1_000)
        )
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(listOf(session)))

        service.initializeSession()

        assertNull(service.getCurrentUser())
        verify(sessionRepository).deactivateSession(session.id)
        verify(sessionRepository, never()).activateSession(any())
    }

    @Test
    fun `drops a session whose user was deactivated`() = runTest {
        val session = PenggunaSession(id = 3, userId = activeUser.id)
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(listOf(session)))
        whenever(penggunaDao.getPenggunaById(activeUser.id))
            .thenReturn(activeUser.copy(isActive = false))

        service.initializeSession()

        assertNull(service.getCurrentUser())
        verify(sessionRepository).deactivateSession(session.id)
    }

    @Test
    fun `stays logged out when there is no session`() = runTest {
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(emptyList()))

        service.initializeSession()

        assertNull(service.getCurrentUser())
    }

    @Test
    fun `reports failure when the session read fails`() = runTest {
        whenever(sessionRepository.getActiveSessions())
            .thenReturn(Result.failure(Exception("db down")))

        val result = service.initializeSession()

        assertTrue(result.isFailure)
        assertNull(service.getCurrentUser())
    }

    // --- idle timeout while the app stays open (CWE-613) --------------------

    @Test
    fun `a freshly used session passes the idle check`() = runTest {
        val session = PenggunaSession(id = 1, userId = activeUser.id, lastActivityTime = Date())
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(listOf(session)))
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)
        whenever(sessionRepository.getActiveSessionForUser(activeUser.id))
            .thenReturn(Result.success(session))
        service.initializeSession()

        assertTrue(service.enforceIdleTimeout())
        assertSame(activeUser, service.getCurrentUser())
    }

    @Test
    fun `an idle session is revoked once the stored row passes the timeout`() = runTest {
        val fresh = PenggunaSession(id = 1, userId = activeUser.id, lastActivityTime = Date())
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(listOf(fresh)))
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)
        service.initializeSession()

        // The stored row is now stale even though nothing re-read it.
        val stale = fresh.copy(lastActivityTime = Date(System.currentTimeMillis() - AuthServiceImpl.SESSION_TIMEOUT_MS - 1))
        whenever(sessionRepository.getActiveSessionForUser(activeUser.id)).thenReturn(Result.success(stale))

        assertFalse(service.enforceIdleTimeout())
        assertNull(service.getCurrentUser())
        verify(sessionRepository).deactivateUserSessions(activeUser.id)
    }

    @Test
    fun `an absent session row is revoked`() = runTest {
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(emptyList()))
        service.initializeSession()

        assertFalse(service.enforceIdleTimeout())
    }

    @Test
    fun `a transient session read failure keeps the user signed in`() = runTest {
        // A failed read is not an expiry: signing the user out on a temporary
        // database error would drop a still-valid session.
        val session = PenggunaSession(id = 1, userId = activeUser.id, lastActivityTime = Date())
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(listOf(session)))
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)
        service.initializeSession()

        whenever(sessionRepository.getActiveSessionForUser(activeUser.id))
            .thenReturn(Result.failure(Exception("db down")))

        assertTrue(service.enforceIdleTimeout())
        assertSame(activeUser, service.getCurrentUser())
        verify(sessionRepository, never()).deactivateUserSessions(any())
    }

    @Test
    fun `a transient session read failure is tolerated, but a sustained one fails closed`() = runTest {
        // A one-off read error must not sign the user out, but a database that
        // stays unreadable past the idle timeout can no longer prove the session
        // is valid, so access is revoked instead of living forever.
        val session = PenggunaSession(id = 1, userId = activeUser.id, lastActivityTime = Date())
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(listOf(session)))
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)
        service.initializeSession()
        assertSame(activeUser, service.getCurrentUser())

        whenever(sessionRepository.getActiveSessionForUser(activeUser.id))
            .thenReturn(Result.failure(Exception("db down")))

        val loginAt = System.currentTimeMillis()
        service.now = { loginAt }
        assertTrue("a single failure is tolerated", service.enforceIdleTimeout())
        assertSame(activeUser, service.getCurrentUser())

        // The same failure, past the timeout, must not keep the session alive.
        service.now = { loginAt + AuthServiceImpl.SESSION_TIMEOUT_MS + 1 }
        assertFalse(service.enforceIdleTimeout())
        assertNull(service.getCurrentUser())
        verify(sessionRepository).deactivateUserSessions(activeUser.id)
    }

    @Test
    fun `a successful check refreshes the window for later read failures`() = runTest {
        val session = PenggunaSession(id = 1, userId = activeUser.id, lastActivityTime = Date())
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(listOf(session)))
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)
        whenever(sessionRepository.getActiveSessionForUser(activeUser.id))
            .thenReturn(Result.success(session))
        service.initializeSession()

        val loginAt = System.currentTimeMillis()
        service.now = { loginAt }
        assertTrue(service.enforceIdleTimeout())

        // A read failure a while later still has the rest of the window to recover.
        whenever(sessionRepository.getActiveSessionForUser(activeUser.id))
            .thenReturn(Result.failure(Exception("db down")))
        service.now = { loginAt + AuthServiceImpl.SESSION_TIMEOUT_MS - 1_000 }
        assertTrue(service.enforceIdleTimeout())
        assertSame(activeUser, service.getCurrentUser())
    }

    @Test
    fun `a stale check does not revoke a user who logged in meanwhile`() = runTest {
        // A's session read is still in flight — and would resolve as idle — when
        // B signs in and replaces the current user. A's result is bound to A's
        // revision, so it must be discarded rather than tearing down B.
        val dao = FakeSessionDao()
        val repo = PenggunaSessionRepository(dao)
        val svc = AuthServiceImpl(penggunaDao, repo)

        val sessionId = dao.insertSession(
            PenggunaSession(userId = activeUser.id, lastActivityTime = Date())
        )
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)
        svc.initializeSession()
        assertSame(activeUser, svc.getCurrentUser())

        // Make A's stored row idle, then hold its read in flight.
        dao.insertSession(
            PenggunaSession(
                id = sessionId,
                userId = activeUser.id,
                lastActivityTime = Date(System.currentTimeMillis() - AuthServiceImpl.SESSION_TIMEOUT_MS - 1_000)
            )
        )
        val gate = CompletableDeferred<Unit>()
        dao.gate = gate
        dao.gatedUserId = activeUser.id

        val job = launch { svc.enforceIdleTimeout() }
        runCurrent() // A's read is now suspended at the gate

        // A newer login lands while A's check is pending.
        val other = Pengguna(id = 8, username = "manager", passwordHash = sha256("mgr-pw"), role = Role.MANAGER, isActive = true)
        whenever(penggunaDao.getPenggunaByUsername("manager")).thenReturn(other)
        svc.login("manager", "mgr-pw")
        assertSame(other, svc.getCurrentUser())

        // A's read completes; it must not revoke B.
        dao.gate = null
        gate.complete(Unit)
        job.join()

        assertSame("B must stay signed in", other, svc.getCurrentUser())
        assertNotNull("B's session must stay active", dao.getActiveSessionForUser(other.id))
    }

    @Test
    fun `observing keeps the user when the session read fails`() = runTest {
        // The flow must not treat a transient read failure as an expiry; otherwise
        // AuthGuard/the drawer would flip to the login screen on a DB hiccup.
        val session = PenggunaSession(id = 1, userId = activeUser.id, lastActivityTime = Date())
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(listOf(session)))
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)
        service.initializeSession()

        whenever(sessionRepository.getActiveSessionForUser(activeUser.id))
            .thenReturn(Result.failure(Exception("db down")))

        assertSame(activeUser, service.observeCurrentUser().first())
        assertSame(activeUser, service.getCurrentUser())
    }

    @Test
    fun `observing an expired session revokes it instead of emitting the user`() = runTest {
        val session = PenggunaSession(id = 1, userId = activeUser.id, lastActivityTime = Date())
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(listOf(session)))
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)
        service.initializeSession()
        assertSame(activeUser, service.getCurrentUser())

        whenever(sessionRepository.getActiveSessionForUser(activeUser.id)).thenReturn(
            Result.success(session.copy(lastActivityTime = Date(System.currentTimeMillis() - AuthServiceImpl.SESSION_TIMEOUT_MS - 1)))
        )

        val observed = service.observeCurrentUser().first()

        assertNull(observed)
        assertNull(service.getCurrentUser())
        verify(sessionRepository).deactivateUserSessions(activeUser.id)
    }

    private fun sha256(value: String): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L
    }
}

/**
 * Minimal in-memory [PenggunaSessionDao] with a one-shot gate: while [gate] is
 * set, a read for [gatedUserId] suspends until the gate completes. This lets a
 * test hold a session check "in flight" to exercise the login race.
 */
private class FakeSessionDao : PenggunaSessionDao {
    private val sessions = mutableListOf<PenggunaSession>()
    private var nextId = 1L
    var gate: CompletableDeferred<Unit>? = null
    var gatedUserId: Long = -1

    override suspend fun getActiveSessions(): List<PenggunaSession> =
        sessions.filter { it.isActive }.sortedByDescending { it.lastActivityTime }

    override suspend fun getActiveSessionForUser(userId: Long): PenggunaSession? {
        if (gate != null && userId == gatedUserId) gate!!.await()
        return sessions
            .filter { it.userId == userId && it.isActive }
            .maxByOrNull { it.lastActivityTime }
    }

    override suspend fun insertSession(session: PenggunaSession): Long {
        val id = if (session.id == 0L) nextId++ else session.id
        sessions.removeAll { it.id == id }
        sessions.add(session.copy(id = id))
        return id
    }

    override suspend fun deactivateUserSessions(userId: Long) {
        sessions.replaceAll { if (it.userId == userId) it.copy(isActive = false) else it }
    }

    override suspend fun deactivateSession(sessionId: Long) {
        sessions.replaceAll { if (it.id == sessionId) it.copy(isActive = false) else it }
    }

    override suspend fun activateSession(sessionId: Long, time: Date) {
        sessions.replaceAll { if (it.id == sessionId) it.copy(isActive = true, lastActivityTime = time) else it }
    }

    override suspend fun deleteOldSessions(cutoffDate: Date) {
        sessions.removeAll { it.loginTime.before(cutoffDate) }
    }
}
