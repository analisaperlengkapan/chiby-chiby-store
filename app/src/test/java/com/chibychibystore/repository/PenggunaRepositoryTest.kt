package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PenggunaDao
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PenggunaRepositoryTest {

    private lateinit var repository: PenggunaRepository
    private lateinit var mockPenggunaDao: PenggunaDao

    private val testUser = Pengguna(
        id = 1,
        username = "testuser",
        passwordHash = "hashedpassword",
        role = Role.CASHIER
    )

    @Before
    fun setup() {
        mockPenggunaDao = mock()
        repository = PenggunaRepository(mockPenggunaDao)
    }

    @Test
    fun `getPenggunaById with existing user should return success`() = runTest {
        // Given
        whenever(mockPenggunaDao.getPenggunaById(1)).thenReturn(testUser)

        // When
        val result = repository.getPenggunaById(1)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
    }

    @Test
    fun `getPenggunaById with non-existing user should return failure`() = runTest {
        // Given
        whenever(mockPenggunaDao.getPenggunaById(999)).thenReturn(null)

        // When
        val result = repository.getPenggunaById(999)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ChibyChibyException.DatabaseError)
    }

    @Test
    fun `getPenggunaByUsername with existing user should return success`() = runTest {
        // Given
        whenever(mockPenggunaDao.getPenggunaByUsername("testuser")).thenReturn(testUser)

        // When
        val result = repository.getPenggunaByUsername("testuser")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
    }

    @Test
    fun `createPengguna with valid data should succeed`() = runTest {
        // Given
        val newUser = Pengguna(
            username = "newuser",
            passwordHash = "hashedpass",
            role = Role.CASHIER
        )
        whenever(mockPenggunaDao.getPenggunaByUsername("newuser")).thenReturn(null)
        whenever(mockPenggunaDao.insertPengguna(newUser)).thenReturn(2)

        // When
        val result = repository.createPengguna(newUser)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2L, result.getOrNull())
    }

    @Test
    fun `createPengguna with duplicate username should fail`() = runTest {
        // Given
        val newUser = testUser.copy(id = 0) // New user with same username
        whenever(mockPenggunaDao.getPenggunaByUsername("testuser")).thenReturn(testUser)

        // When
        val result = repository.createPengguna(newUser)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ChibyChibyException.ValidationError)
    }

    @Test
    fun `createPengguna with empty username should fail`() = runTest {
        // Given
        val invalidUser = testUser.copy(username = "")

        // When
        val result = repository.createPengguna(invalidUser)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ChibyChibyException.ValidationError)
    }

    @Test
    fun `updatePengguna with valid data should succeed`() = runTest {
        // Given
        val updatedUser = testUser.copy(username = "updateduser")
        whenever(mockPenggunaDao.getPenggunaById(1)).thenReturn(testUser)
        whenever(mockPenggunaDao.getPenggunaByUsername("updateduser")).thenReturn(null)

        // When
        val result = repository.updatePengguna(updatedUser)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `updatePengguna with non-existing user should fail`() = runTest {
        // Given
        val updatedUser = testUser.copy(id = 999)
        whenever(mockPenggunaDao.getPenggunaById(999)).thenReturn(null)

        // When
        val result = repository.updatePengguna(updatedUser)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ChibyChibyException.DatabaseError)
    }

    @Test
    fun `deletePengguna with existing user should succeed`() = runTest {
        // Given
        whenever(mockPenggunaDao.getPenggunaById(1)).thenReturn(testUser)
        whenever(mockPenggunaDao.getAllPengguna()).thenReturn(flowOf(listOf(testUser, testUser.copy(id = 2, role = Role.OWNER))))

        // When
        val result = repository.deletePengguna(1)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deletePengguna with last owner should fail`() = runTest {
        // Given - only one owner exists
        val ownerUser = testUser.copy(role = Role.OWNER)
        whenever(mockPenggunaDao.getPenggunaById(1)).thenReturn(ownerUser)
        whenever(mockPenggunaDao.getAllPengguna()).thenReturn(flowOf(listOf(ownerUser)))

        // When
        val result = repository.deletePengguna(1)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is ChibyChibyException.BusinessLogicError)
    }

    @Test
    fun `getPenggunaCount should return count`() = runTest {
        // Given
        whenever(mockPenggunaDao.getPenggunaCount()).thenReturn(5)

        // When
        val result = repository.getPenggunaCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull())
    }
}