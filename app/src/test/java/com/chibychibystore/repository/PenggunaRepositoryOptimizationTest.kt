package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PenggunaDao
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PenggunaRepositoryOptimizationTest {

    private lateinit var repository: PenggunaRepository
    private lateinit var mockPenggunaDao: PenggunaDao

    @Before
    fun setup() {
        mockPenggunaDao = mock()
        repository = PenggunaRepository(mockPenggunaDao)
    }

    @Test
    fun `createPenggunaList should filter invalid users and insert valid ones`() = runTest {
        // Given
        val validUser1 = Pengguna(username = "user1", passwordHash = "pass1", role = Role.CASHIER)
        val validUser2 = Pengguna(username = "user2", passwordHash = "pass2", role = Role.CASHIER)
        val invalidUserEmptyUsername = Pengguna(username = "", passwordHash = "pass", role = Role.CASHIER)
        val invalidUserShortUsername = Pengguna(username = "ab", passwordHash = "pass", role = Role.CASHIER)

        val users = listOf(validUser1, invalidUserEmptyUsername, validUser2, invalidUserShortUsername)

        // Mock DAO response: user1 inserted (id 1), user2 inserted (id 2)
        whenever(mockPenggunaDao.insertPenggunaListIgnoreConflict(any())).thenReturn(listOf(1L, 2L))

        // When
        val result = repository.createPenggunaList(users)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())

        // Verify only valid users were passed to DAO
        val captor = argumentCaptor<List<Pengguna>>()
        verify(mockPenggunaDao).insertPenggunaListIgnoreConflict(captor.capture())

        val capturedList = captor.firstValue
        assertEquals(2, capturedList.size)
        assertTrue(capturedList.any { it.username == "user1" })
        assertTrue(capturedList.any { it.username == "user2" })
    }

    @Test
    fun `createPenggunaList should count only successful inserts`() = runTest {
        // Given
        val user1 = Pengguna(username = "user1", passwordHash = "pass1", role = Role.CASHIER)
        val user2 = Pengguna(username = "user2", passwordHash = "pass2", role = Role.CASHIER)

        val users = listOf(user1, user2)

        // Mock DAO response: user1 inserted (id 1), user2 ignored/conflict (id -1)
        whenever(mockPenggunaDao.insertPenggunaListIgnoreConflict(any())).thenReturn(listOf(1L, -1L))

        // When
        val result = repository.createPenggunaList(users)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun `createPenggunaList with empty list returns zero`() = runTest {
        // Given
        val users = emptyList<Pengguna>()

        // When
        val result = repository.createPenggunaList(users)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }
}
