package com.chibychibystore
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PenggunaRepository
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.impl.UserManagementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.mockito.kotlin.*
class UserManagementIntegrationTest {
    @Test
    fun testUserCreation() = runBlocking {
        val repo: PenggunaRepository = mock()
        val auth: AuthService = mock()
        val service = UserManagementServiceImpl(repo, auth)
        whenever(auth.hasPermission(any())).thenReturn(true)
        whenever(repo.getUserByUsername(any())).thenReturn(Result.failure(Exception()))
        whenever(repo.createPengguna(any())).thenReturn(Result.success(1L))
        val res = service.createUser("newuser", "password123", Role.CASHIER, 1L)
        Assert.assertTrue(res is Result.Success)
    }
}
