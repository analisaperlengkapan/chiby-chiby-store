package com.chibychibystore.service
import com.chibychibystore.data.local.dao.PenggunaDao
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PenggunaSessionRepository
import com.chibychibystore.service.impl.AuthServiceImpl
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.mockito.kotlin.*
import java.security.MessageDigest
class AuthServiceTest {
    private lateinit var penggunaDao: PenggunaDao
    private lateinit var penggunaSessionRepository: PenggunaSessionRepository
    private lateinit var authService: AuthService
    @Before
    fun setup() {
        penggunaDao = mock()
        penggunaSessionRepository = mock()
        authService = AuthServiceImpl(penggunaDao, penggunaSessionRepository)
    }
    @Test
    fun loginSuccess() = runTest {
        val pw = "p"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(pw.toByteArray()).fold("") { str, it -> str + "%02x".format(it) }
        val user = Pengguna(id = 1L, username = "u", passwordHash = hash, role = Role.CASHIER, isActive = true)
        whenever(penggunaDao.getPenggunaByUsername("u")).thenReturn(user)
        whenever(penggunaSessionRepository.createSession(any())).thenReturn(Result.success(1L))
        val res = authService.login("u", pw)
        Assert.assertTrue(res is Result.Success)
    }
}
