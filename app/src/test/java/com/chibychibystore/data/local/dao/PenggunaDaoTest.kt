package com.chibychibystore.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date

class PenggunaDaoTest {

    private lateinit var database: ChibyChibyDatabase
    private lateinit var penggunaDao: PenggunaDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChibyChibyDatabase::class.java
        ).build()
        penggunaDao = database.penggunaDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetPengguna() = runBlocking {
        val pengguna = Pengguna(
            username = "testuser",
            passwordHash = "hashedpassword",
            role = Role.CASHIER
        )

        val id = penggunaDao.insertPengguna(pengguna)
        val retrieved = penggunaDao.getPenggunaById(id)

        assertNotNull(retrieved)
        assertEquals("testuser", retrieved?.username)
        assertEquals(Role.CASHIER, retrieved?.role)
    }

    @Test
    fun getPenggunaByUsername() = runBlocking {
        val pengguna = Pengguna(
            username = "testuser",
            passwordHash = "hashedpassword",
            role = Role.CASHIER
        )

        penggunaDao.insertPengguna(pengguna)
        val retrieved = penggunaDao.getPenggunaByUsername("testuser")

        assertNotNull(retrieved)
        assertEquals("testuser", retrieved?.username)
    }

    @Test
    fun getAllPengguna() = runBlocking {
        val pengguna1 = Pengguna(username = "user1", passwordHash = "pass1", role = Role.CASHIER)
        val pengguna2 = Pengguna(username = "user2", passwordHash = "pass2", role = Role.MANAGER)

        penggunaDao.insertPengguna(pengguna1)
        penggunaDao.insertPengguna(pengguna2)

        val allPengguna = penggunaDao.getAllPengguna().first()

        assertEquals(2, allPengguna.size)
    }

    @Test
    fun getPenggunaByRole() = runBlocking {
        val cashier = Pengguna(username = "cashier", passwordHash = "pass", role = Role.CASHIER)
        val manager = Pengguna(username = "manager", passwordHash = "pass", role = Role.MANAGER)

        penggunaDao.insertPengguna(cashier)
        penggunaDao.insertPengguna(manager)

        val cashiers = penggunaDao.getPenggunaByRole(Role.CASHIER).first()

        assertEquals(1, cashiers.size)
        assertEquals("cashier", cashiers[0].username)
    }

    @Test
    fun updatePengguna() = runBlocking {
        val pengguna = Pengguna(username = "user", passwordHash = "pass", role = Role.CASHIER)
        val id = penggunaDao.insertPengguna(pengguna)

        val updatedPengguna = pengguna.copy(id = id, username = "updateduser")
        penggunaDao.updatePengguna(updatedPengguna)

        val retrieved = penggunaDao.getPenggunaById(id)
        assertEquals("updateduser", retrieved?.username)
    }

    @Test
    fun deletePengguna() = runBlocking {
        val pengguna = Pengguna(username = "user", passwordHash = "pass", role = Role.CASHIER)
        val id = penggunaDao.insertPengguna(pengguna)

        penggunaDao.deletePenggunaById(id)

        val retrieved = penggunaDao.getPenggunaById(id)
        assertNull(retrieved)
    }

    @Test
    fun getPenggunaCount() = runBlocking {
        val countBefore = penggunaDao.getPenggunaCount()
        assertEquals(0, countBefore)

        penggunaDao.insertPengguna(Pengguna(username = "user", passwordHash = "pass", role = Role.CASHIER))

        val countAfter = penggunaDao.getPenggunaCount()
        assertEquals(1, countAfter)
    }
}