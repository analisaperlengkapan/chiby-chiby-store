package com.chibychibystore.service

import android.content.Context
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.chibychibystore.testutils.BaseTest
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.chibychibystore.service.impl.BackupServiceImpl
import com.chibychibystore.service.BackupInfo
import com.chibychibystore.service.BackupValidationResult
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BackupServiceIntegrationTest : BaseTest() {

    private lateinit var db: ChibyChibyDatabase
    private lateinit var backupService: BackupServiceImpl

    /** Temp roots registered by [useTempBackupDir], cleaned recursively in teardown. */
    private val tempBackupDirs = mutableListOf<File>()

    private fun useTempBackupDir(prefix: String): File {
        val root = File(System.getProperty("java.io.tmpdir"), "$prefix-${System.nanoTime()}")
        val dir = File(root, "ChibyChibyBackup")
        dir.mkdirs()
        tempBackupDirs += root
        backupService.setBackupDirectoryForTest(dir)
        return dir
    }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ChibyChibyDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        backupService = BackupServiceImpl(
            context,
            Dispatchers.Unconfined,
            PenggunaRepository(db.penggunaDao()),
            KategoriRepository(db.kategoriDao(), db.produkDao()),
            GudangRepository(db.gudangDao(), db.produkDao()),
            ProdukRepository(db.produkDao()),
            PemasokRepository(db.pemasokDao(), db.pembelianDao()),
            PenjualanRepository(db.penjualanDao(), db.itemPenjualanDao()),
            ItemPenjualanRepository(db.itemPenjualanDao()),
            PembelianRepository(db.pembelianDao()),
            ItemPembelianRepository(db.itemPembelianDao()),
            PengeluaranRepository(db.pengeluaranDao()),
            ShiftRepository(db.shiftDao()),
            PelangganRepository(db.pelangganDao()),
            StokGudangRepository(db.stokGudangDao(), db.produkDao()),
            InventoryAuditRepository(db.inventoryAuditDao())
        )
    }

    @After
    fun teardown() {
        // Clean backup directory artifacts created by tests (best-effort)
        try {
            val backupDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "ChibyChibyBackup")
            if (backupDir.exists()) {
                backupDir.listFiles()?.forEach { it.delete() }
                backupDir.delete()
            }
        } catch (e: Exception) {
            // Some Android Environment methods are not available in the test runtime; ignore cleanup failure
        }
        // Remove every temp directory a test redirected the service into, so
        // repeated runs do not accumulate backup artifacts.
        tempBackupDirs.forEach { root -> root.deleteRecursively() }
        tempBackupDirs.clear()
        db.close()
    }

    @Test
    fun createBackup_and_validateBackup_success() = runBlocking {
        // Seed minimal data
        val penggunaId = db.penggunaDao().insertPengguna(com.chibychibystore.data.local.entity.Pengguna(username = "buser", passwordHash = "x", role = com.chibychibystore.data.local.entity.Role.OWNER))
        val kategoriId = db.kategoriDao().insertKategori(com.chibychibystore.data.local.entity.Kategori(name = "BK1"))
        // Ensure backup directory exists (Robolectric may not create it by default)
        try {
            val backupDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "ChibyChibyBackup")
            if (!backupDir.exists()) backupDir.mkdirs()
            // Point service to use the same directory to avoid any environment differences
            backupService.setBackupDirectoryForTest(backupDir)
        } catch (_: Exception) { /* ignore */ }

        val res = backupService.createBackup()
        // Debug: print full result to capture any messages in test output
        println("[DEBUG] createBackup result: isSuccess=${res.isSuccess}, exception=${res.exceptionOrNull()}")
        if (!res.isSuccess) {
            // Re-throw the underlying exception so the test report contains the full stack trace
            throw (res.exceptionOrNull() ?: Exception("createBackup returned failure with no exception"))
        }
        assertTrue("createBackup failed: ${res.exceptionOrNull()}", res.isSuccess)
        val info = res.getOrNull()!!
        println("[DEBUG] backup info: filePath=${info.filePath}, fileName=${info.fileName}, size=${info.sizeBytes}")
        val createdFile = File(info.filePath)
        println("[DEBUG] file exists: ${createdFile.exists()}, abs=${createdFile.absolutePath}, canon=${createdFile.canonicalPath}")
        println("[DEBUG] parent listing: ${createdFile.parentFile?.listFiles()?.map { it.name } }")
        assertTrue("Backup file was not created at expected path: ${info.filePath}", createdFile.exists())

        val valRes = backupService.validateBackup(info.filePath)
        println("[DEBUG] validate result: isSuccess=${valRes.isSuccess}, exception=${valRes.exceptionOrNull()}")
        if (!valRes.isSuccess) throw (valRes.exceptionOrNull() ?: Exception("validateBackup returned failure without exception"))
        val validation = valRes.getOrNull()!!
        println("[DEBUG] validation: isValid=${validation.isValid}, version=${validation.version}, counts=${validation.recordCounts}, errors=${validation.errors}")
        assertTrue("Validation failed for backup: ${validation.errors}", validation.isValid)
        assertEquals(1, validation.recordCounts?.get("users"))

        // Now corrupt the file and ensure validation fails
        createdFile.writeText("corrupted-data")
        val valRes2 = backupService.validateBackup(info.filePath)
        // validateBackup returns a result with isValid=false for corrupted data (no exception)
        assertTrue(valRes2.isSuccess)
        val v2 = valRes2.getOrNull()!!
        assertFalse("Corrupted backup should be invalid", v2.isValid)

    }

    @Test
    fun backupHistory_lists_created_backups() = runBlocking {
        // Restores the `getBackupHistory` cases the deleted `BackupServiceTest`
        // covered.
        useTempBackupDir("chiby-backup-history")

        val created = backupService.createBackup()
        assertTrue("createBackup failed: ${created.exceptionOrNull()}", created.isSuccess)

        val history = backupService.getBackupHistory()
        assertTrue(history.isSuccess)
        val files = history.getOrNull()!!
        assertEquals(1, files.size)
        assertEquals(created.getOrNull()!!.fileName, files.first().fileName)
    }

    @Test
    fun deleteBackup_removes_the_file_and_errors_when_missing() = runBlocking {
        useTempBackupDir("chiby-backup-delete")

        val created = backupService.createBackup().getOrNull()!!
        val history = backupService.getBackupHistory().getOrNull()!!
        val backupId = history.first().id

        assertTrue(backupService.deleteBackup(backupId).isSuccess)
        assertFalse("the file must be gone after delete", File(created.filePath).exists())
        assertTrue(backupService.getBackupHistory().getOrNull()!!.isEmpty())

        assertTrue("deleting a missing backup must fail", backupService.deleteBackup(backupId).isFailure)
    }
}
