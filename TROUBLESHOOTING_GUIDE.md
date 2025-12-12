# Troubleshooting Guide - Chiby Chiby Store

**Versi:** 1.0
**Tanggal:** December 11, 2025
**Status:** Final
**Referensi Dokumen:** requirements.md, backend-design.md

## Daftar Isi
1. [Quick Diagnosis](#1-quick-diagnosis)
2. [Installation Issues](#2-installation-issues)
3. [Startup Problems](#3-startup-problems)
4. [Login Issues](#4-login-issues)
5. [Database Problems](#5-database-problems)
6. [Inventory Issues](#6-inventory-issues)
7. [Sales & POS Problems](#7-sales--pos-problems)
8. [Barcode Scanning Issues](#8-barcode-scanning-issues)
9. [Printing Problems](#9-printing-problems)
10. [Reporting Errors](#10-reporting-errors)
11. [Backup & Restore Issues](#11-backup--restore-issues)
12. [Performance Issues](#12-performance-issues)
13. [Security & Permissions](#13-security--permissions)
14. [Error Codes Reference](#14-error-codes-reference)
15. [Diagnostic Tools](#15-diagnostic-tools)
16. [Advanced Troubleshooting](#16-advanced-troubleshooting)

---

## 1. Quick Diagnosis

### 1.1 System Health Check

**Jalankan health check script:**
```bash
# Via Android Studio terminal atau ADB
adb shell pm clear com.chibychibystore  # Clear app data
adb shell am start -n com.chibychibystore/.ui.MainActivity  # Restart app
```

**Check system resources:**
- RAM: Minimum 2GB available
- Storage: Minimum 500MB free space
- Battery: Minimum 20% charge

### 1.2 App Version Check

**Verify app version:**
```kotlin
// Dalam app: Settings > About
// Expected: Version 1.0.0 (Build 1)
```

**Update app jika perlu:**
- Google Play Store: Check for updates
- Direct APK: Download latest version

### 1.3 Log Analysis

**Enable developer options:**
1. Settings > About Phone > Tap Build Number 7x
2. Settings > Developer Options > Enable USB Debugging
3. Settings > Developer Options > Enable Show Taps

**Capture logs:**
```bash
# Via ADB
adb logcat | grep chibychibystore

# Via Android Studio: View > Tool Windows > Logcat
# Filter: package:com.chibychibystore
```

---

## 2. Installation Issues

### 2.1 APK Installation Failed

**Error:** "App not installed" atau "Installation failed"

**Possible Causes:**
- Insufficient storage space
- Corrupted APK file
- Conflicting package name
- Android version incompatibility

**Solutions:**

1. **Check Storage Space**
   ```bash
   # Check available storage
   adb shell df /data
   # Should show >500MB available
   ```

2. **Verify APK Integrity**
   ```bash
   # Check APK file size
   ls -la app-release.apk
   # Should be >50MB for release build
   ```

3. **Clear Package Conflicts**
   ```bash
   # Uninstall existing version
   adb uninstall com.chibychibystore

   # Clear package cache
   adb shell pm clear com.chibychibystore
   ```

4. **Check Android Version**
   ```bash
   # Check device Android version
   adb shell getprop ro.build.version.release
   # Should be 5.0+ (API 21+)
   ```

### 2.2 Play Store Download Issues

**Error:** "Download pending" atau "Can't download app"

**Solutions:**

1. **Clear Play Store Cache**
   - Settings > Apps > Google Play Store > Storage > Clear Cache
   - Settings > Apps > Google Play Services > Storage > Clear Cache

2. **Check Network Connection**
   - Switch between WiFi and mobile data
   - Disable VPN if active

3. **Free Up Storage**
   - Delete unnecessary files/apps
   - Clear Downloads folder

### 2.3 App Won't Open After Install

**Symptoms:** App installs but crashes immediately

**Diagnostic Steps:**

1. **Check Android Version Compatibility**
   ```bash
   adb shell getprop ro.build.version.sdk
   # Should be >= 21
   ```

2. **Verify Device Architecture**
   ```bash
   adb shell getprop ro.product.cpu.abi
   # Should support armeabi-v7a atau arm64-v8a
   ```

3. **Check Required Permissions**
   ```bash
   # Grant permissions manually
   adb shell pm grant com.chibychibystore android.permission.CAMERA
   adb shell pm grant com.chibychibystore android.permission.WRITE_EXTERNAL_STORAGE
   ```

---

## 3. Startup Problems

### 3.1 App Crashes on Launch

**Error:** "Unfortunately, Chiby Chiby Store has stopped"

**Diagnostic Steps:**

1. **Check Logcat for Crash Details**
   ```bash
   adb logcat -d | grep "FATAL EXCEPTION"
   ```

2. **Common Crash Causes:**
   - Database corruption
   - Missing permissions
   - Out of memory
   - Corrupted shared preferences

**Solutions:**

1. **Clear App Data**
   ```bash
   adb shell pm clear com.chibychibystore
   # This will reset app to initial state
   ```

2. **Check Database Integrity**
   ```kotlin
   // Check if database file exists and is readable
   val dbFile = File(context.getDatabasePath("chiby_chiby.db").path)
   if (!dbFile.exists() || !dbFile.canRead()) {
       // Database corrupted, trigger restore or reset
   }
   ```

3. **Verify Permissions**
   ```bash
   # Check granted permissions
   adb shell dumpsys package com.chibychibystore | grep permission
   ```

### 3.2 White Screen on Startup

**Symptoms:** App shows white screen and doesn't respond

**Possible Causes:**
- UI thread blocked
- Infinite loading loop
- Compose rendering issues

**Solutions:**

1. **Force Stop App**
   ```bash
   adb shell am force-stop com.chibychibystore
   adb shell am start -n com.chibychibystore/.ui.MainActivity
   ```

2. **Check for ANR (Application Not Responding)**
   ```bash
   adb shell dumpsys activity activities | grep -A 10 "Chiby"
   ```

3. **Clear Cache and Restart**
   ```bash
   adb shell pm clear com.chibychibystore
   ```

### 3.3 Slow Startup

**Symptoms:** App takes >10 seconds to start

**Diagnostic Steps:**

1. **Measure Startup Time**
   ```bash
   adb shell am start -W -n com.chibychibystore/.ui.MainActivity
   # Look for "TotalTime" in output
   ```

2. **Check Database Size**
   ```bash
   adb shell ls -la /data/data/com.chibychibystore/databases/
   # Large database files indicate optimization needed
   ```

**Optimizations:**

1. **Database Indexing**
   ```sql
   -- Add indexes for frequently queried columns
   CREATE INDEX idx_products_barcode ON products(barcode);
   CREATE INDEX idx_sales_date ON sales(sale_date);
   ```

2. **Lazy Loading**
   ```kotlin
   // Implement pagination for large datasets
   val products = productService.getProducts(page = currentPage, limit = 50)
   ```

---

## 4. Login Issues

### 4.1 Invalid Credentials Error

**Error:** "Username atau password salah"

**Diagnostic Steps:**

1. **Check Default Credentials**
   ```
   Owner: username="owner", password="owner123"
   Manager: username="manager", password="manager123"
   Cashier: username="cashier", password="cashier123"
   Warehouse: username="warehouse", password="warehouse123"
   ```

2. **Verify Password Hashing**
   ```kotlin
   // Check if password is hashed correctly
   val hashedPassword = hashPassword(plainPassword)
   val storedHash = user.passwordHash
   ```

3. **Check User Status**
   ```kotlin
   // Ensure user account is active
   val user = userRepository.getUserByUsername(username)
   if (user == null || !user.isActive) {
       throw ValidationError("User tidak aktif")
   }
   ```

### 4.2 Login Loop

**Symptoms:** Login succeeds but returns to login screen

**Possible Causes:**
- Session storage corrupted
- Auth state not persisted
- Token expired

**Solutions:**

1. **Clear Session Data**
   ```kotlin
   // Clear shared preferences
   sharedPreferences.edit().clear().apply()
   ```

2. **Check Session Persistence**
   ```kotlin
   // Verify session is saved after login
   val session = UserSession(userId = user.id, loginTime = now())
   userSessionRepository.saveSession(session)
   ```

3. **Restart App**
   ```bash
   adb shell am force-stop com.chibychibystore
   adb shell am start -n com.chibychibystore/.ui.MainActivity
   ```

### 4.3 Permission Denied After Login

**Error:** "Tidak memiliki izin untuk fitur ini"

**Diagnostic Steps:**

1. **Check User Role**
   ```kotlin
   val user = authService.getCurrentUser()
   when (user?.role) {
       "OWNER" -> // Full access
       "MANAGER" -> // Limited access
       "CASHIER" -> // POS only
       "WAREHOUSE" -> // Inventory only
   }
   ```

2. **Verify Permission Mapping**
   ```kotlin
   val permissions = mapOf(
       "EDIT_PRODUCT" to listOf("OWNER", "MANAGER", "WAREHOUSE"),
       "DELETE_SALE" to listOf("OWNER"),
       "VIEW_REPORTS" to listOf("OWNER", "MANAGER")
   )
   ```

---

## 5. Database Problems

### 5.1 Database Corruption

**Symptoms:** App crashes with SQLite errors

**Diagnostic Steps:**

1. **Check Database File**
   ```bash
   adb shell ls -la /data/data/com.chibychibystore/databases/chiby_chiby.db
   # Check file size and permissions
   ```

2. **Run Integrity Check**
   ```sql
   PRAGMA integrity_check;
   -- Should return "ok" if database is healthy
   ```

**Recovery Steps:**

1. **Create Backup First**
   ```kotlin
   val backupResult = backupService.createBackup()
   ```

2. **Reset Database**
   ```kotlin
   // Delete corrupted database
   context.deleteDatabase("chiby_chiby.db")

   // Reinitialize with default data
   dataSeedingService.seedAllData()
   ```

3. **Restore from Backup**
   ```kotlin
   val restoreResult = restoreService.restoreFromBackup(backupFile)
   ```

### 5.2 Data Loss

**Symptoms:** Products/sales data missing

**Recovery Steps:**

1. **Check Backup History**
   ```kotlin
   val backups = backupService.getBackupHistory()
   ```

2. **Restore Latest Backup**
   ```kotlin
   val latestBackup = backups.firstOrNull()
   if (latestBackup != null) {
       restoreService.restoreFromBackup(latestBackup.filename)
   }
   ```

3. **Manual Data Entry**
   - Re-enter critical data
   - Use import feature if available

### 5.3 Migration Errors

**Error:** "Room cannot verify the data integrity"

**Solutions:**

1. **Check Migration Path**
   ```kotlin
   @Database(
       entities = [...],
       version = 2,
       autoMigrations = [
           AutoMigration(from = 1, to = 2)
       ]
   )
   ```

2. **Manual Migration**
   ```kotlin
   val migration_1_2 = object : Migration(1, 2) {
       override fun migrate(database: SupportSQLiteDatabase) {
           // Add migration logic
           database.execSQL("ALTER TABLE products ADD COLUMN min_stock INTEGER DEFAULT 0")
       }
   }
   ```

---

## 6. Inventory Issues

### 6.1 Products Not Showing

**Symptoms:** Product list empty atau tidak update

**Diagnostic Steps:**

1. **Check Database Content**
   ```sql
   SELECT COUNT(*) FROM products;
   -- Should return > 0 if products exist
   ```

2. **Verify Repository Query**
   ```kotlin
   val products = productRepository.getAllProducts()
   Log.d("Inventory", "Found ${products.size} products")
   ```

3. **Check UI State**
   ```kotlin
   // Verify ViewModel state
   inventoryViewModel.products.collect { products ->
       Log.d("UI", "UI received ${products.size} products")
   }
   ```

**Solutions:**

1. **Refresh Data**
   ```kotlin
   inventoryViewModel.loadProducts()
   ```

2. **Check Search Filter**
   ```kotlin
   // Clear search query
   _searchQuery.value = ""
   ```

3. **Re-seed Data**
   ```kotlin
   dataSeedingService.seedProducts()
   ```

### 6.2 Stock Quantity Wrong

**Symptoms:** Stock levels tidak akurat

**Diagnostic Steps:**

1. **Check Stock Calculations**
   ```sql
   SELECT p.name, p.stock_quantity,
          (SELECT SUM(si.quantity) FROM sale_items si WHERE si.product_id = p.id) as sold,
          (SELECT SUM(pi.quantity) FROM purchase_items pi WHERE pi.product_id = p.id) as purchased
   FROM products p;
   ```

2. **Verify Transaction Consistency**
   ```kotlin
   // Check if sales reduce stock correctly
   val sale = createSale(saleData)
   productService.updateStock(productId, -quantity)
   ```

**Corrections:**

1. **Manual Stock Adjustment**
   ```kotlin
   productService.updateStock(productId, correctQuantity)
   ```

2. **Audit Trail**
   ```kotlin
   // Log stock changes for audit
   stockAuditRepository.logChange(productId, oldStock, newStock, reason)
   ```

### 6.3 Category Issues

**Symptoms:** Products not grouped correctly

**Solutions:**

1. **Check Category Assignments**
   ```sql
   SELECT p.name, c.name as category_name
   FROM products p
   LEFT JOIN categories c ON p.category_id = c.id;
   ```

2. **Reassign Categories**
   ```kotlin
   productService.updateProduct(product.copy(categoryId = correctCategoryId))
   ```

---

## 7. Sales & POS Problems

### 7.1 POS Won't Start

**Symptoms:** POS screen tidak bisa diakses

**Diagnostic Steps:**

1. **Check User Permissions**
   ```kotlin
   if (!authService.hasPermission("CREATE_SALE")) {
       // Show permission error
   }
   ```

2. **Verify Product Data**
   ```kotlin
   val products = productService.getProducts()
   if (products.isEmpty()) {
       // Show no products error
   }
   ```

**Solutions:**

1. **Grant Permissions**
   ```kotlin
   // For cashier role
   user.permissions = listOf("CREATE_SALE", "VIEW_PRODUCTS")
   ```

2. **Load Initial Data**
   ```kotlin
   dataSeedingService.seedProducts()
   ```

### 7.2 Transaction Failed

**Error:** "Transaksi gagal diproses"

**Diagnostic Steps:**

1. **Check Stock Availability**
   ```kotlin
   val product = productService.getProduct(productId)
   if (product.stockQuantity < requestedQuantity) {
       throw BusinessLogicError("Stok tidak mencukupi")
   }
   ```

2. **Verify Payment Data**
   ```kotlin
   val totalAmount = items.sumOf { it.quantity * it.unitPrice }
   if (paymentAmount < totalAmount) {
       throw ValidationError("Pembayaran kurang")
   }
   ```

**Recovery:**

1. **Rollback Transaction**
   ```kotlin
   // Restore stock if sale failed
   productService.updateStock(productId, +quantity)
   ```

2. **Retry Transaction**
   ```kotlin
   // Clear error state and retry
   posViewModel.clearError()
   ```

### 7.3 Receipt Printing Failed

**Symptoms:** Sale completed but receipt tidak tercetak

**Diagnostic Steps:**

1. **Check Printer Connection**
   ```kotlin
   val printerStatus = printerService.getPrinterStatus()
   ```

2. **Verify Printer Configuration**
   ```kotlin
   val printerAddress = sharedPreferences.getString("printer_address", null)
   ```

**Solutions:**

1. **Reconnect Printer**
   ```kotlin
   printerService.connectPrinter(printerAddress)
   ```

2. **Manual Receipt**
   ```kotlin
   // Generate digital receipt
   val receiptData = receiptService.generateReceipt(saleId)
   ```

---

## 8. Barcode Scanning Issues

### 8.1 Camera Not Accessible

**Error:** "Camera permission denied"

**Solutions:**

1. **Grant Camera Permission**
   ```bash
   adb shell pm grant com.chibychibystore android.permission.CAMERA
   ```

2. **Check Camera Hardware**
   ```bash
   # Test camera dengan Camera app
   adb shell am start -a android.media.action.IMAGE_CAPTURE
   ```

3. **Restart App**
   ```bash
   adb shell am force-stop com.chibychibystore
   adb shell am start -n com.chibychibystore/.ui.MainActivity
   ```

### 8.2 Barcode Not Recognized

**Symptoms:** Camera works but barcode tidak terdeteksi

**Diagnostic Steps:**

1. **Check Supported Formats**
   ```kotlin
   val supportedFormats = barcodeService.getSupportedFormats()
   // Should include EAN13, CODE128, QR
   ```

2. **Verify Barcode Quality**
   - Ensure good lighting
   - Hold device steady
   - Check barcode is not damaged

**Solutions:**

1. **Manual Input Fallback**
   ```kotlin
   // Allow manual barcode entry
   barcodeScannerViewModel.enableManualInput()
   ```

2. **Adjust Scanner Settings**
   ```kotlin
   // ZXing scanner hints
   val hints = mapOf(
       DecodeHintType.TRY_HARDER to true,
       DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.EAN13)
   )
   ```

### 8.3 Invalid Barcode Format

**Error:** "Format barcode tidak didukung"

**Solutions:**

1. **Check GS1 Indonesia Compliance**
   ```kotlin
   // Indonesian barcodes should start with "899"
   if (!barcode.startsWith("899")) {
       // Show format warning
   }
   ```

2. **Validate Check Digit**
   ```kotlin
   val isValid = barcodeService.validateEAN13Checksum(barcode)
   ```

---

## 9. Printing Problems

### 9.1 Printer Not Found

**Error:** "Printer tidak ditemukan"

**Diagnostic Steps:**

1. **Check Bluetooth Status**
   ```bash
   adb shell settings get global bluetooth_on
   # Should return 1 (enabled)
   ```

2. **List Paired Devices**
   ```bash
   adb shell dumpsys bluetooth_manager | grep -A 10 "Paired devices"
   ```

3. **Test Bluetooth Connection**
   ```bash
   # Enable Bluetooth if disabled
   adb shell am start -a android.bluetooth.adapter.action.REQUEST_ENABLE
   ```

**Solutions:**

1. **Pair Printer**
   - Settings > Bluetooth > Pair new device
   - Select thermal printer
   - Enter PIN if required

2. **Configure Printer Address**
   ```kotlin
   sharedPreferences.edit()
       .putString("printer_address", "00:11:22:33:44:55")
       .apply()
   ```

### 9.2 Print Quality Issues

**Symptoms:** Receipt faded atau tidak jelas

**Solutions:**

1. **Check Paper Quality**
   - Use thermal paper with correct specifications
   - Ensure paper is not expired

2. **Adjust Print Density**
   ```kotlin
   // ESC/POS print density command
   val densityCommand = byteArrayOf(0x1B, 0x7A, 0x01) // High density
   ```

3. **Clean Print Head**
   - Follow printer manual for cleaning procedure

### 9.3 Print Job Stuck

**Symptoms:** Print command sent but nothing prints

**Solutions:**

1. **Cancel Print Job**
   ```kotlin
   printerService.cancelPrintJob()
   ```

2. **Reset Printer Connection**
   ```kotlin
   printerService.disconnectPrinter()
   printerService.connectPrinter(address)
   ```

3. **Check Printer Status**
   ```kotlin
   val status = printerService.getPrinterStatus()
   when (status) {
       PrinterStatus.OUT_OF_PAPER -> // Show paper error
       PrinterStatus.OVERHEAT -> // Show cooling message
       PrinterStatus.OK -> // Retry print
   }
   ```

---

## 10. Reporting Errors

### 10.1 Report Data Empty

**Symptoms:** Reports show no data

**Diagnostic Steps:**

1. **Check Date Range**
   ```kotlin
   val startDate = LocalDate.now().minusDays(30)
   val endDate = LocalDate.now()
   // Ensure date range includes data
   ```

2. **Verify Data Existence**
   ```sql
   SELECT COUNT(*) FROM sales WHERE sale_date BETWEEN '2025-11-11' AND '2025-12-11';
   ```

**Solutions:**

1. **Adjust Date Filters**
   ```kotlin
   // Expand date range
   reportsViewModel.setDateRange(startDate.minusDays(30), endDate)
   ```

2. **Check Data Seeding**
   ```kotlin
   dataSeedingService.seedSalesData()
   ```

### 10.2 Incorrect Calculations

**Symptoms:** Report totals don't match

**Diagnostic Steps:**

1. **Manual Calculation Verification**
   ```sql
   SELECT SUM(total_amount) FROM sales WHERE sale_date BETWEEN 'start' AND 'end';
   ```

2. **Check Calculation Logic**
   ```kotlin
   val totalRevenue = sales.sumOf { it.totalAmount }
   val totalCost = sales.sumOf { sale ->
       sale.items.sumOf { it.quantity * it.costPrice }
   }
   ```

**Corrections:**

1. **Recalculate Reports**
   ```kotlin
   reportsViewModel.refreshReports()
   ```

2. **Data Audit**
   ```kotlin
   // Log calculation discrepancies
   auditService.logCalculationMismatch(expected, actual, saleId)
   ```

### 10.3 PDF Export Failed

**Error:** "Gagal export PDF"

**Diagnostic Steps:**

1. **Check Storage Permissions**
   ```bash
   adb shell pm grant com.chibychibystore android.permission.WRITE_EXTERNAL_STORAGE
   ```

2. **Verify External Storage**
   ```bash
   adb shell df /storage/emulated/0
   # Should have sufficient space
   ```

**Solutions:**

1. **Grant Permissions**
   ```kotlin
   // Request storage permission
   ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
   ```

2. **Alternative Export**
   ```kotlin
   // Export as CSV instead
   csvExportService.exportReport(reportData, filename)
   ```

---

## 11. Backup & Restore Issues

### 11.1 Backup Creation Failed

**Error:** "Backup gagal dibuat"

**Diagnostic Steps:**

1. **Check Storage Space**
   ```bash
   adb shell df /storage/emulated/0
   # Should have > database size * 2
   ```

2. **Verify Permissions**
   ```bash
   adb shell pm grant com.chibychibystore android.permission.WRITE_EXTERNAL_STORAGE
   ```

**Solutions:**

1. **Free Up Space**
   ```bash
   # Clear cache and downloads
   adb shell pm clear com.chibychibystore
   ```

2. **Manual Backup**
   ```kotlin
   // Create backup with progress callback
   backupService.createBackupWithProgress { progress ->
       // Update UI with progress
   }
   ```

### 11.2 Restore Failed

**Error:** "Restore gagal"

**Diagnostic Steps:**

1. **Validate Backup File**
   ```kotlin
   val isValid = backupService.validateBackupFile(filename)
   ```

2. **Check File Integrity**
   ```kotlin
   // Verify file is not corrupted
   val checksum = calculateChecksum(backupFile)
   ```

**Recovery:**

1. **Use Different Backup**
   ```kotlin
   val backups = backupService.getBackupHistory()
   // Try older backup file
   ```

2. **Partial Restore**
   ```kotlin
   // Restore only critical data
   restoreService.restoreProductsOnly(backupFile)
   ```

### 11.3 Encrypted Backup Issues

**Symptoms:** Can't decrypt backup file

**Solutions:**

1. **Check Master Key**
   ```kotlin
   // Verify Android KeyStore has master key
   val masterKey = MasterKey.Builder(context)
       .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
       .build()
   ```

2. **Key Reset**
   ```kotlin
   // Clear KeyStore and recreate keys
   keyStore.deleteEntry("chiby_chiby_master_key")
   ```

---

## 12. Performance Issues

### 12.1 App Running Slow

**Symptoms:** UI lag, slow responses

**Diagnostic Steps:**

1. **Check Memory Usage**
   ```bash
   adb shell dumpsys meminfo com.chibychibystore
   # Look for high memory usage
   ```

2. **Profile CPU Usage**
   ```bash
   adb shell dumpsys cpuinfo | grep chibychibystore
   ```

**Optimizations:**

1. **Database Query Optimization**
   ```sql
   -- Add missing indexes
   CREATE INDEX idx_sales_date ON sales(sale_date);
   CREATE INDEX idx_products_category ON products(category_id);
   ```

2. **UI Performance**
   ```kotlin
   // Use remember for expensive computations
   val filteredProducts = remember(products, searchQuery) {
       products.filter { it.name.contains(searchQuery, ignoreCase = true) }
   }
   ```

### 12.2 High Battery Usage

**Symptoms:** Battery drains quickly

**Diagnostic Steps:**

1. **Check Battery Usage**
   ```bash
   adb shell dumpsys batterystats | grep chibychibystore
   ```

2. **Identify Power Consumers**
   - Continuous GPS
   - Frequent network requests
   - Background services

**Solutions:**

1. **Optimize Location Updates**
   ```kotlin
   // Use passive location provider
   locationManager.requestLocationUpdates(
       LocationManager.PASSIVE_PROVIDER,
       300000, // 5 minutes
       100f,   // 100 meters
       locationListener
   )
   ```

2. **Batch Operations**
   ```kotlin
   // Combine multiple operations
   viewModelScope.launch {
       val results = coroutineScope {
           async { operation1() }
           async { operation2() }
       }
       processResults(results)
   }
   ```

### 12.3 Storage Full

**Symptoms:** "Storage full" errors

**Solutions:**

1. **Clear Cache**
   ```bash
   adb shell pm clear com.chibychibystore
   ```

2. **Optimize Database**
   ```sql
   VACUUM;  -- Reclaim unused space
   ```

3. **Archive Old Data**
   ```kotlin
   // Move old sales to archive
   archiveService.archiveOldSales(olderThan = LocalDate.now().minusYears(1))
   ```

---

## 13. Security & Permissions

### 13.1 Permission Denied

**Error:** "Permission denied"

**Diagnostic Steps:**

1. **Check Runtime Permissions**
   ```bash
   adb shell dumpsys package com.chibychibystore | grep permission
   ```

2. **Verify Permission Requests**
   ```kotlin
   when (permission) {
       Manifest.permission.CAMERA -> requestCameraPermission()
       Manifest.permission.WRITE_EXTERNAL_STORAGE -> requestStoragePermission()
   }
   ```

**Solutions:**

1. **Grant Permissions Manually**
   ```bash
   adb shell pm grant com.chibychibystore android.permission.CAMERA
   adb shell pm grant com.chibychibystore android.permission.WRITE_EXTERNAL_STORAGE
   ```

2. **App Settings**
   - Settings > Apps > Chiby Chiby Store > Permissions
   - Enable required permissions

### 13.2 Security Warnings

**Symptoms:** Play Protect warnings

**Solutions:**

1. **Update App**
   - Install latest version from Play Store

2. **Verify APK Signature**
   ```bash
   # Check APK signature
   jarsigner -verify -verbose app-release.apk
   ```

3. **Security Scan**
   ```bash
   # Run security audit
   ./gradlew dependencyCheckAnalyze
   ```

---

## 14. Error Codes Reference

### 14.1 Validation Errors (VAL_xxx)

| Code | Message | Cause | Solution |
|------|---------|-------|----------|
| VAL_001 | Username tidak boleh kosong | Empty username field | Enter valid username |
| VAL_002 | Password minimal 6 karakter | Password too short | Use stronger password |
| VAL_003 | Barcode tidak valid | Invalid barcode format | Check barcode format |
| VAL_004 | Stok tidak mencukupi | Insufficient stock | Restock or reduce quantity |
| VAL_005 | Harga tidak boleh negatif | Negative price | Enter positive price |

### 14.2 Database Errors (DB_xxx)

| Code | Message | Cause | Solution |
|------|---------|-------|----------|
| DB_001 | Database corrupted | File corruption | Restore from backup |
| DB_002 | Foreign key constraint failed | Invalid reference | Check data relationships |
| DB_003 | Unique constraint failed | Duplicate data | Use unique values |
| DB_004 | Migration failed | Schema mismatch | Manual migration |
| DB_005 | Disk full | No storage space | Free up space |

### 14.3 Business Logic Errors (BIZ_xxx)

| Code | Message | Cause | Solution |
|------|---------|-------|----------|
| BIZ_001 | Tidak memiliki izin | Insufficient permissions | Contact administrator |
| BIZ_002 | Transaksi gagal | Business rule violation | Check transaction data |
| BIZ_003 | Produk tidak aktif | Inactive product | Activate product first |
| BIZ_004 | Gudang penuh | Warehouse capacity exceeded | Use different warehouse |
| BIZ_005 | Periode laporan invalid | Invalid date range | Select valid date range |

### 14.4 Network Errors (NET_xxx)

| Code | Message | Cause | Solution |
|------|---------|-------|----------|
| NET_001 | Printer tidak terhubung | Bluetooth disconnected | Reconnect printer |
| NET_002 | Connection timeout | Network timeout | Retry operation |
| NET_003 | Invalid printer address | Wrong Bluetooth address | Check printer settings |

### 14.5 System Errors (SYS_xxx)

| Code | Message | Cause | Solution |
|------|---------|-------|----------|
| SYS_001 | Out of memory | Memory exhausted | Close other apps |
| SYS_002 | Storage full | Disk space exhausted | Free up storage |
| SYS_003 | Camera not available | Hardware failure | Use manual input |
| SYS_004 | Permission denied | Security policy | Grant permissions |

---

## 15. Diagnostic Tools

### 15.1 Built-in Diagnostics

**App Health Check:**
```kotlin
// Access via: Settings > Diagnostics
val diagnostics = DiagnosticsService()

// Run comprehensive check
val healthReport = diagnostics.runHealthCheck()
healthReport.issues.forEach { issue ->
    Log.w("Health", "${issue.severity}: ${issue.message}")
}
```

**Database Diagnostics:**
```kotlin
val dbDiagnostics = DatabaseDiagnostics()

// Check table integrity
val integrityResult = dbDiagnostics.checkIntegrity()

// Analyze query performance
val slowQueries = dbDiagnostics.findSlowQueries()
```

### 15.2 ADB Commands

**Log Analysis:**
```bash
# Capture all logs
adb logcat -d > app_logs.txt

# Filter by package
adb logcat | grep chibychibystore

# Follow logs in real-time
adb logcat -v time | grep chibychibystore
```

**System Information:**
```bash
# Device info
adb shell getprop

# App info
adb shell dumpsys package com.chibychibystore

# Memory info
adb shell dumpsys meminfo com.chibychibystore
```

**File Operations:**
```bash
# Pull database file
adb pull /data/data/com.chibychibystore/databases/chiby_chiby.db .

# Push backup file
adb push backup.enc /storage/emulated/0/ChibyChibyBackup/
```

### 15.3 Performance Profiling

**Android Profiler:**
1. Android Studio > View > Tool Windows > Profiler
2. Select device and app process
3. Record session during issue reproduction
4. Analyze CPU, Memory, Network usage

**Memory Leak Detection:**
```kotlin
// Enable LeakCanary in debug builds
if (BuildConfig.DEBUG) {
    LeakCanary.run()
}
```

---

## 16. Advanced Troubleshooting

### 16.1 Factory Reset

**Warning:** This will delete all data

**Steps:**
1. **Backup Data First**
   ```kotlin
   val backupResult = backupService.createBackup()
   ```

2. **Clear All Data**
   ```bash
   adb shell pm clear com.chibychibystore
   ```

3. **Reinstall App**
   ```bash
   adb uninstall com.chibychibystore
   adb install app-release.apk
   ```

4. **Restore Data**
   ```kotlin
   val restoreResult = restoreService.restoreFromBackup(backupFile)
   ```

### 16.2 Debug Mode

**Enable Debug Features:**
```kotlin
// Add to build.gradle
buildConfigField "boolean", "DEBUG_MODE", "true"

// In code
if (BuildConfig.DEBUG_MODE) {
    // Enable debug features
    Timber.plant(Timber.DebugTree())
    enableStrictMode()
}
```

**StrictMode for Development:**
```kotlin
private fun enableStrictMode() {
    StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .build())

    StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .build())
}
```

### 16.3 Remote Debugging

**Via ADB over WiFi:**
```bash
# Connect device
adb tcpip 5555

# Find device IP
adb shell ip route

# Connect wirelessly
adb connect 192.168.1.100:5555
```

**Log Remote Device:**
```bash
# Stream logs from remote device
adb logcat | grep chibychibystore > remote_logs.txt
```

### 16.4 Emergency Recovery

**Complete System Reset:**
```bash
# Stop app
adb shell am force-stop com.chibychibystore

# Clear all data
adb shell pm clear com.chibychibystore

# Delete database files
adb shell rm -rf /data/data/com.chibychibystore/databases/*

# Delete shared preferences
adb shell rm -rf /data/data/com.chibychibystore/shared_prefs/*

# Restart app
adb shell am start -n com.chibychibystore/.ui.MainActivity
```

**Post-Recovery Setup:**
1. Login dengan default credentials
2. Import data dari backup jika tersedia
3. Reconfigure printer dan settings
4. Test semua fitur utama

---

## 17. Support Resources

### 17.1 Documentation
- [User Manual](PANDUAN_PENGUNA.md)
- [API Documentation](API_DOCUMENTATION.md)
- [Setup Guide](SETUP_DEPLOYMENT_GUIDE.md)

### 17.2 Community Support
- GitHub Issues: https://github.com/your-org/chiby-chiby-store/issues
- Stack Overflow: Tag `chiby-chiby-store`

### 17.3 Professional Support
- Email: support@chibychibystore.com
- Emergency Hotline: +62-XXX-XXXX-XXXX
- Business Hours: Monday-Friday, 9AM-6PM WIB

### 17.4 Debug Information

**System Information to Provide:**
```kotlin
val systemInfo = mapOf(
    "app_version" to BuildConfig.VERSION_NAME,
    "android_version" to Build.VERSION.RELEASE,
    "device_model" to Build.MODEL,
    "database_size" to getDatabaseSize(),
    "available_storage" to getAvailableStorage()
)
```

**Error Report Template:**
```
Issue: [Brief description]
Steps to reproduce:
1. [Step 1]
2. [Step 2]
3. [Expected result]
4. [Actual result]

System Info:
- App Version: [version]
- Android Version: [version]
- Device: [model]

Logs: [Attach logcat output]
Screenshots: [Attach if applicable]
```

---

*Troubleshooting Guide ini mencakup diagnosis dan solusi untuk masalah umum pada Chiby Chiby Store. Jika masalah berlanjut, hubungi support team dengan informasi debug yang lengkap.*