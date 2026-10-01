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
import org.junit.Test
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
        whenever(sessionRepository.getActiveSession()).thenReturn(Result.success(session))
        whenever(penggunaDao.getPenggunaById(activeUser.id)).thenReturn(activeUser)

        val result = service.initializeSession()

        assertSame(activeUser, service.getCurrentUser())
        verify(sessionRepository).updateLastActivityTime(session.id)
        verify(sessionRepository, never()).deactivateUserSessions(activeUser.id)
    }

    @Test
    fun `drops a session idle past the timeout`() = runTest {
        val session = PenggunaSession(
            id = 2,
            userId = activeUser.id,
            lastActivityTime = Date(System.currentTimeMillis() - AuthServiceImpl.SESSION_TIMEOUT_MS - 1_000)
        )
        whenever(sessionRepository.getActiveSession()).thenReturn(Result.success(session))

        service.initializeSession()

        assertNull(service.getCurrentUser())
        verify(sessionRepository).deactivateUserSessions(activeUser.id)
    }

    @Test
    fun `drops a session whose user was deactivated`() = runTest {
        val session = PenggunaSession(id = 3, userId = activeUser.id)
        whenever(sessionRepository.getActiveSession()).thenReturn(Result.success(session))
        whenever(penggunaDao.getPenggunaById(activeUser.id))
            .thenReturn(activeUser.copy(isActive = false))

        service.initializeSession()

        assertNull(service.getCurrentUser())
        verify(sessionRepository).deactivateUserSessions(activeUser.id)
    }

    @Test
    fun `stays logged out when there is no session`() = runTest {
        whenever(sessionRepository.getActiveSession()).thenReturn(Result.success(null))

        service.initializeSession()

        assertNull(service.getCurrentUser())
    }
}
