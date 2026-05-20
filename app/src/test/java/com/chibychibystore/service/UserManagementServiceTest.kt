package com.chibychibystore.service
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PenggunaRepository
import com.chibychibystore.service.impl.UserManagementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.mockito.kotlin.*
class UserManagementServiceTest {
    @Test
    fun getUserByIdSuccess() = runBlocking {
        val repo: PenggunaRepository = mock()
        val auth: AuthService = mock()
        val service = UserManagementServiceImpl(repo, auth)
        val user = Pengguna(id = 1L, username = "u", passwordHash = "h", role = Role.OWNER)
        whenever(auth.hasPermission(any())).thenReturn(true)
        whenever(repo.getUserById(1L)).thenReturn(Result.success(user))
        val res = service.getUserById(1L)
        Assert.assertEquals(user, (res as Result.Success).data)
    }
}
