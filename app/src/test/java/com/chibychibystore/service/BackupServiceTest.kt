package com.chibychibystore.service

import android.content.Context
import com.chibychibystore.data.Result
import com.chibychibystore.data.model.*
import com.chibychibystore.repository.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File

class BackupServiceTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var penggunaRepository: PenggunaRepository

    @Mock
    private lateinit var kategoriRepository: KategoriRepository

    @Mock
    private lateinit var gudangRepository: GudangRepository

    @Mock
    private lateinit var produkRepository: ProdukRepository

    @Mock
    private lateinit var pemasokRepository: PemasokRepository

    @Mock
    private lateinit var penjualanRepository: PenjualanRepository

    @Mock
    private lateinit var itemPenjualanRepository: ItemPenjualanRepository

    @Mock
    private lateinit var pengeluaranRepository: PengeluaranRepository

    private lateinit var backupService: BackupService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        backupService = BackupServiceImpl(
            context,
            penggunaRepository,
            kategoriRepository,
            gudangRepository,
            produkRepository,
            pemasokRepository,
            penjualanRepository,
            itemPenjualanRepository,
            pengeluaranRepository
        )
    }

    @Test
    fun `createBackup should export all data successfully`() = runTest {
        // Given
        val mockUsers = listOf(
            Pengguna(id = 1, username = "admin", passwordHash = "hash", role = "OWNER", permissions = "all")
        )
        val mockCategories = listOf(
            Kategori(id = 1, nama = "Food", description = "Food items")
        )
        val mockWarehouses = listOf(
            Gudang(id = 1, nama = "Main", location = "Jakarta", capacity = 1000)
        )
        val mockProducts = listOf(
            Produk(id = 1, nama = "Product A", barcode = "1234567890123", kategoriId = 1,
                   hargaBeli = 30000.0, hargaJual = 50000.0, stokQuantity = 10, gudangId = 1)
        )
        val mockSuppliers = listOf(
            Pemasok(id = 1, nama = "Supplier A", contact = "08123456789", address = "Jakarta")
        )
        val mockSales = listOf(
            Penjualan(id = 1, tanggalPenjualan = java.time.LocalDate.now(), totalAmount = 50000.0,
                     metodePembayaran = "CASH", kasirId = 1)
        )
        val mockSaleItems = listOf(
            ItemPenjualan(id = 1, penjualanId = 1, produkId = 1, quantity = 1,
                         unitPrice = 50000.0, totalPrice = 50000.0)
        )
        val mockExpenses = listOf(
            Pengeluaran(id = 1, tanggalPengeluaran = java.time.LocalDate.now(), kategori = "Utilitas",
                       amount = 25000.0, description = "Listrik", approvedBy = 1, createdBy = 1)
        )

        // Mock all repository calls
        `when`(penggunaRepository.getAllPengguna()).thenReturn(mockUsers)
        `when`(kategoriRepository.getAllKategori()).thenReturn(mockCategories)
        `when`(gudangRepository.getAllGudang()).thenReturn(mockWarehouses)
        `when`(produkRepository.getAllProduk()).thenReturn(mockProducts)
        `when`(pemasokRepository.getAllPemasok()).thenReturn(mockSuppliers)
        `when`(penjualanRepository.getAllPenjualan()).thenReturn(mockSales)
        `when`(itemPenjualanRepository.getAllItemPenjualan()).thenReturn(mockSaleItems)
        `when`(pengeluaranRepository.getAllPengeluaran()).thenReturn(mockExpenses)

        // Mock file operations
        val mockFile = mock(File::class.java)
        `when`(mockFile.exists()).thenReturn(true)
        `when`(mockFile.absolutePath).thenReturn("/test/backup.enc")

        // When
        val result = backupService.createBackup()

        // Then
        assertTrue(result is Result.Success)
        val backupFile = (result as Result.Success).data
        assertTrue(backupFile.absolutePath.endsWith(".enc"))

        // Verify all repositories were called
        verify(penggunaRepository).getAllPengguna()
        verify(kategoriRepository).getAllKategori()
        verify(gudangRepository).getAllGudang()
        verify(produkRepository).getAllProduk()
        verify(pemasokRepository).getAllPemasok()
        verify(penjualanRepository).getAllPenjualan()
        verify(itemPenjualanRepository).getAllItemPenjualan()
        verify(pengeluaranRepository).getAllPengeluaran()
    }

    @Test
    fun `createBackup should handle empty data gracefully`() = runTest {
        // Given - all repositories return empty lists
        `when`(penggunaRepository.getAllPengguna()).thenReturn(emptyList())
        `when`(kategoriRepository.getAllKategori()).thenReturn(emptyList())
        `when`(gudangRepository.getAllGudang()).thenReturn(emptyList())
        `when`(produkRepository.getAllProduk()).thenReturn(emptyList())
        `when`(pemasokRepository.getAllPemasok()).thenReturn(emptyList())
        `when`(penjualanRepository.getAllPenjualan()).thenReturn(emptyList())
        `when`(itemPenjualanRepository.getAllItemPenjualan()).thenReturn(emptyList())
        `when`(pengeluaranRepository.getAllPengeluaran()).thenReturn(emptyList())

        // When
        val result = backupService.createBackup()

        // Then
        assertTrue(result is Result.Success)
        val backupFile = (result as Result.Success).data
        assertTrue(backupFile.absolutePath.endsWith(".enc"))
    }

    @Test
    fun `createBackup should return error when repository fails`() = runTest {
        // Given
        `when`(penggunaRepository.getAllPengguna()).thenThrow(RuntimeException("Database error"))

        // When
        val result = backupService.createBackup()

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals("Database error", error.exception.message)
    }

    @Test
    fun `getBackupHistory should return list of backup files`() = runTest {
        // Given
        val mockBackupDir = mock(File::class.java)
        val mockBackupFile = mock(File::class.java)

        `when`(context.getExternalFilesDir("ChibyChibyBackup")).thenReturn(mockBackupDir)
        `when`(mockBackupDir.exists()).thenReturn(true)
        `when`(mockBackupDir.listFiles()).thenReturn(arrayOf(mockBackupFile))
        `when`(mockBackupFile.name).thenReturn("backup_20250115_120000.enc")
        `when`(mockBackupFile.length()).thenReturn(1024000L)
        `when`(mockBackupFile.lastModified()).thenReturn(System.currentTimeMillis())

        // When
        val result = backupService.getBackupHistory()

        // Then
        assertTrue(result is Result.Success)
        val backups = (result as Result.Success).data
        assertEquals(1, backups.size)
        assertTrue(backups[0].fileName.contains("backup_20250115"))
        assertEquals(1024000L, backups[0].fileSize)
    }

    @Test
    fun `getBackupHistory should return empty list when no backups exist`() = runTest {
        // Given
        val mockBackupDir = mock(File::class.java)
        `when`(context.getExternalFilesDir("ChibyChibyBackup")).thenReturn(mockBackupDir)
        `when`(mockBackupDir.exists()).thenReturn(false)

        // When
        val result = backupService.getBackupHistory()

        // Then
        assertTrue(result is Result.Success)
        val backups = (result as Result.Success).data
        assertTrue(backups.isEmpty())
    }

    @Test
    fun `validateBackup should verify backup file integrity`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(true)
        `when`(mockBackupFile.canRead()).thenReturn(true)
        `when`(mockBackupFile.length()).thenReturn(1024L)

        // When
        val result = backupService.validateBackup(mockBackupFile)

        // Then
        assertTrue(result is Result.Success)
        val isValid = (result as Result.Success).data
        assertTrue(isValid)
    }

    @Test
    fun `validateBackup should return false for non-existent file`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(false)

        // When
        val result = backupService.validateBackup(mockBackupFile)

        // Then
        assertTrue(result is Result.Success)
        val isValid = (result as Result.Success).data
        assertFalse(isValid)
    }

    @Test
    fun `validateBackup should return false for unreadable file`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(true)
        `when`(mockBackupFile.canRead()).thenReturn(false)

        // When
        val result = backupService.validateBackup(mockBackupFile)

        // Then
        assertTrue(result is Result.Success)
        val isValid = (result as Result.Success).data
        assertFalse(isValid)
    }

    @Test
    fun `deleteBackup should remove backup file successfully`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(true)
        `when`(mockBackupFile.delete()).thenReturn(true)

        // When
        val result = backupService.deleteBackup(mockBackupFile)

        // Then
        assertTrue(result is Result.Success)
        val success = (result as Result.Success).data
        assertTrue(success)
        verify(mockBackupFile).delete()
    }

    @Test
    fun `deleteBackup should return false when file deletion fails`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(true)
        `when`(mockBackupFile.delete()).thenReturn(false)

        // When
        val result = backupService.deleteBackup(mockBackupFile)

        // Then
        assertTrue(result is Result.Success)
        val success = (result as Result.Success).data
        assertFalse(success)
    }

    @Test
    fun `deleteBackup should return error for non-existent file`() = runTest {
        // Given
        val mockBackupFile = mock(File::class.java)
        `when`(mockBackupFile.exists()).thenReturn(false)

        // When
        val result = backupService.deleteBackup(mockBackupFile)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("tidak ada") == true)
    }
}