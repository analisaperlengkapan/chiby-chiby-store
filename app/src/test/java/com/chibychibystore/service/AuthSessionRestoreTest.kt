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
import org.junit.Assert.assertEquals
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
    fun `sessionStatus is VALID for a freshly used session`() = runTest {
        val session = PenggunaSession(id = 1, userId = activeUser.id, lastActivityTime = Date())
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(listOf(session)))
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)
        whenever(sessionRepository.getActiveSessionForUser(activeUser.id))
            .thenReturn(Result.success(session))
        service.initializeSession()

        assertEquals(AuthService.SessionStatus.VALID, service.sessionStatus())
    }

    @Test
    fun `sessionStatus is EXPIRED once the stored session passes the timeout`() = runTest {
        val fresh = PenggunaSession(id = 1, userId = activeUser.id, lastActivityTime = Date())
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(listOf(fresh)))
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)
        service.initializeSession()

        // The stored row is now stale even though nothing re-read it.
        val stale = fresh.copy(lastActivityTime = Date(System.currentTimeMillis() - AuthServiceImpl.SESSION_TIMEOUT_MS - 1))
        whenever(sessionRepository.getActiveSessionForUser(activeUser.id)).thenReturn(Result.success(stale))

        assertEquals(AuthService.SessionStatus.EXPIRED, service.sessionStatus())
        assertNull(service.getCurrentUser())
        verify(sessionRepository).deactivateUserSessions(activeUser.id)
    }

    @Test
    fun `sessionStatus is EXPIRED when there is no session row`() = runTest {
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(emptyList()))
        service.initializeSession()

        assertEquals(AuthService.SessionStatus.EXPIRED, service.sessionStatus())
    }

    @Test
    fun `a transient session read failure is UNKNOWN and keeps the user signed in`() = runTest {
        // A failed read is not an expiry: signing the user out on a temporary
        // database error would drop a still-valid session.
        val session = PenggunaSession(id = 1, userId = activeUser.id, lastActivityTime = Date())
        whenever(sessionRepository.getActiveSessions()).thenReturn(Result.success(listOf(session)))
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)
        service.initializeSession()

        whenever(sessionRepository.getActiveSessionForUser(activeUser.id))
            .thenReturn(Result.failure(Exception("db down")))

        assertEquals(AuthService.SessionStatus.UNKNOWN, service.sessionStatus())
        assertSame(activeUser, service.getCurrentUser())
        verify(sessionRepository, never()).deactivateUserSessions(any())
    }

    @Test
    fun `a stale check does not revoke a user who logged in meanwhile`() = runTest {
        // A's session lookup is still in flight when B signs in. The revocation
        // must be identity-aware (and the stale check cancelled), or it would
        // deactivate B and force B to sign in again.
        val dao = FakeSessionDao()
        val repo = PenggunaSessionRepository(dao)
        val svc = AuthServiceImpl(penggunaDao, repo)

        dao.insertSession(PenggunaSession(userId = activeUser.id, lastActivityTime = Date()))
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)
        svc.initializeSession()
        assertSame(activeUser, svc.getCurrentUser())

        // Hold A's session read so it is still in flight when B signs in.
        val gate = CompletableDeferred<Unit>()
        dao.gate = gate
        dao.gatedUserId = activeUser.id

        val job = launch { svc.observeCurrentUser().collect {} }
        runCurrent() // A's check is now suspended at the gate

        // A newer login lands while A's check is pending.
        val other = Pengguna(id = 8, username = "manager", passwordHash = "hash", role = Role.MANAGER, isActive = true)
        whenever(penggunaDao.getPenggunaById(other.id)).thenReturn(other)
        dao.insertSession(PenggunaSession(userId = other.id, lastActivityTime = Date()))
        svc.initializeSession()
        assertSame(other, svc.getCurrentUser())

        // A's read completes; it must not revoke B.
        dao.gate = null
        gate.complete(Unit)
        runCurrent()

        assertSame("B must stay signed in", other, svc.getCurrentUser())
        assertNotNull("B's session must stay active", dao.getActiveSessionForUser(other.id))
        job.cancel()
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
