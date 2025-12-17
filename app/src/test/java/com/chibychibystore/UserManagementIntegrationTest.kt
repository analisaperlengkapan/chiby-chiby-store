package com.chibychibystore

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.repository.PenggunaRepository
import com.chibychibystore.repository.UserSessionRepository
import com.chibychibystore.service.AuthServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest
import java.util.*
import com.chibychibystore.testutils.BaseTest

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
class UserManagementIntegrationTest : BaseTest() {

    private lateinit var db: ChibyChibyDatabase
    private lateinit var penggunaRepo: PenggunaRepository
    private lateinit var sessionRepo: UserSessionRepository
    private lateinit var authService: AuthServiceImpl

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, ChibyChibyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        penggunaRepo = PenggunaRepository(db.penggunaDao())
        sessionRepo = UserSessionRepository(db.userSessionDao())
        authService = AuthServiceImpl(db.penggunaDao(), sessionRepo)
    }

    @After
    fun teardown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    @Test
    fun create_user_and_login_success() = runTest {
        val password = "password123"
        val user = Pengguna(username = "owner", passwordHash = hashPassword(password), role = Role.OWNER)
        val createRes = penggunaRepo.createPengguna(user)
        assertTrue(createRes.isSuccess)

        val loginRes = authService.login("owner", password)
        assertTrue(loginRes.isSuccess)
        val logged = loginRes.getOrNull()
        assertNotNull(logged)
        assertEquals(Role.OWNER, logged?.role)
    }

    @Test
    fun change_password_allows_new_login() = runTest {
        val password = "oldpass"
        val user = Pengguna(username = "bob", passwordHash = hashPassword(password), role = Role.MANAGER)
        penggunaRepo.createPengguna(user)

        val loginRes = authService.login("bob", password)
        assertTrue(loginRes.isSuccess)

        val changeRes = authService.changePassword(password, "newpass123")
        assertTrue(changeRes.isSuccess)

        // Logout and login with new pass
        authService.logout()
        val relogin = authService.login("bob", "newpass123")
        assertTrue(relogin.isSuccess)
    }

    @Test
    fun cannot_delete_last_owner() = runTest {
        val pw = "pw"
        val owner = Pengguna(username = "soleowner", passwordHash = hashPassword(pw), role = Role.OWNER)
        val res = penggunaRepo.createPengguna(owner)
        assertTrue(res.isSuccess)
        val id = res.getOrNull()!!

        val delRes = penggunaRepo.deletePengguna(id)
        assertTrue(delRes.isFailure)
    }
}
