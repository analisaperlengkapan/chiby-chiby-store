package com.chibychibystore.service

import com.chibychibystore.data.local.dao.PenggunaDao
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.PenggunaSession
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PenggunaSessionRepository
import com.chibychibystore.service.impl.AuthServiceImpl
import kotlinx.coroutines.test.runTest
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

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
