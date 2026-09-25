# Setup and Deployment Guide - Chiby Chiby Store

**Versi:** 1.0
**Tanggal:** December 11, 2025
**Status:** Final
**Referensi Dokumen:** requirements.md

## Daftar Isi
1. [Prerequisites](#1-prerequisites)
2. [Development Environment Setup](#2-development-environment-setup)
3. [Project Structure](#3-project-structure)
4. [Building the Application](#4-building-the-application)
5. [Configuration](#5-configuration)
6. [Running the Application](#6-running-the-application)
7. [Testing](#7-testing)
8. [Deployment](#8-deployment)
9. [Backup & Restore](#9-backup--restore)
10. [Troubleshooting](#10-troubleshooting)
11. [Performance Optimization](#11-performance-optimization)
12. [Security Configuration](#12-security-configuration)

---

## 1. Prerequisites

**Refs:** [Req 1.4, 7.1]

### 1.1 System Requirements
- **OS**: Windows 10/11, macOS 10.15+, Linux (Ubuntu 18.04+)
- **RAM**: Minimum 8GB, Recommended 16GB
- **Storage**: 10GB free space untuk Android SDK dan project
- **CPU**: Intel/AMD x64 atau Apple Silicon (dengan Rosetta 2)

### 1.2 Software Requirements
- **Java JDK**: **Version 17 required** (Oracle JDK or OpenJDK). Note: Android Gradle Plugin compatibility and CI require Java 17 — ensure your local `JAVA_HOME` points to a Java 17 installation.
- **Android Studio**: Arctic Fox (2020.3.1) atau lebih baru
- **Android SDK**: API Level 21+ (Android 5.0)
- **Git**: Version 2.25+ untuk version control

### 1.3 Hardware Requirements
- **Android Device**: Minimum Android 5.0 (API 21) untuk testing
- **Camera**: Untuk barcode scanning functionality
- **Bluetooth**: Untuk thermal printer connectivity
- **Storage**: External storage access untuk backup/restore

### 1.4 Network Requirements
- **Internet**: Required untuk download dependencies dan updates
- **No cloud dependency**: Aplikasi berjalan offline setelah install

---

## 2. Development Environment Setup

**Refs:** [Req 1.4]

### 2.1 Install Java JDK

#### Windows
```bash
# Download JDK dari Oracle atau Adoptium
# Set JAVA_HOME environment variable
setx JAVA_HOME "C:\Program Files\Java\jdk-11.x.x"
setx PATH "%PATH%;%JAVA_HOME%\bin"
```

#### macOS
```bash
# Install via Homebrew
brew install openjdk@11

# Set JAVA_HOME
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 11)' >> ~/.zshrc
source ~/.zshrc
```

#### Linux (Ubuntu)
```bash
# Install OpenJDK
sudo apt update
sudo apt install openjdk-11-jdk

# Verify installation
java -version
javac -version

#### Ensure JAVA_HOME points to Java 17
For compatibility with Android Gradle Plugin and this project's CI, set JAVA_HOME to a Java 17 installation (examples):

```bash
# Linux (example path)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# macOS (Homebrew Temurin example)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Windows (PowerShell example)
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.0.x"
```
```

### 2.2 Install Android Studio

1. **Download Android Studio**
   - Kunjungi: https://developer.android.com/studio
   - Download versi terbaru untuk platform Anda

2. **Install Android Studio**
   - Jalankan installer
   - Pilih "Standard" installation
   - Tunggu sampai SDK components terinstall

3. **Configure Android SDK**
   - Buka Android Studio
   - Pergi ke `File > Settings > Appearance & Behavior > System Settings > Android SDK`
   - Install SDK Platforms: API 21+ (Android 5.0+)
   - Install SDK Tools: Android SDK Build-Tools, Android Emulator

4. **Configure Environment Variables**
   ```bash
   # Windows
   setx ANDROID_HOME "%LOCALAPPDATA%\Android\Sdk"
   setx PATH "%PATH%;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools"

   # macOS/Linux
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
   ```

### 2.3 Clone Project

```bash
# Clone repository
git clone https://github.com/your-org/chiby-chiby-store.git
cd chiby-chiby-store

# Open in Android Studio
studio .
```

### 2.4 Initial Project Setup

1. **Sync Gradle Files**
   - Android Studio akan otomatis sync Gradle files
   - Tunggu sampai semua dependencies terdownload

2. **Accept SDK Licenses**
   ```bash
   # Accept Android SDK licenses
   yes | $ANDROID_HOME/tools/bin/sdkmanager --licenses
   ```

3. **Build Project**
   ```bash
   # Via Android Studio: Build > Make Project
   # Via command line
   ./gradlew assembleDebug
   ```

---

## 3. Project Structure

**Refs:** [Req 1.5]

### 3.1 Root Directory Structure
```
chiby-chiby-store/
├── app/                          # Main application module
│   ├── build.gradle             # App-level build configuration
│   ├── proguard-rules.pro       # ProGuard rules untuk release build
│   └── src/
│       ├── androidTest/         # Instrumentation tests
│       ├── androidTest_disabled/# Instrumentation tests (dinonaktifkan)
│       ├── main/                # Main source code
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/chibychibystore/
│       │   │   ├── constant/    # Konstanta aplikasi (TAX_RATE, dll.)
│       │   │   ├── data/        # Data layer (Room entities, DAOs, repositories)
│       │   │   ├── di/          # Dependency injection (Hilt modules)
│       │   │   ├── error/       # Error handling classes
│       │   │   ├── repository/  # Repository layer
│       │   │   ├── service/     # Business logic services
│       │   │   ├── ui/          # UI layer (Compose screens, ViewModels)
│       │   │   └── util/        # Utility helpers
│       │   └── res/             # Android resources
│       └── test/                # Unit tests (termasuk screenshot harness)
│           ├── java/com/chibychibystore/screenshots/  # Screenshot generator
│           └── resources/robolectric.properties       # sdk=34, graphics.mode=NATIVE
├── docs/screenshots/            # Gambar untuk README
├── build.gradle                 # Project-level build configuration
├── gradle.properties           # Gradle properties
├── local.properties            # Local configuration (SDK path)
├── settings.gradle             # Project settings
└── gradle/                     # Gradle wrapper
```

### 3.2 Key Components

#### Data Layer
- **Entities**: Room database entities (Pengguna, Produk, Penjualan, etc.)
- **DAOs**: Data Access Objects untuk database operations
- **Database**: Room database configuration

#### Service Layer
- **Services**: Business logic services (AuthService, ProductService, etc.)
- **Repositories**: Data repositories dengan abstraction
- **Use Cases**: Application-specific business logic

#### UI Layer
- **Screens**: Jetpack Compose screens
- **ViewModels**: State management dengan StateFlow
- **Components**: Reusable UI components

#### Dependency Injection
- **Hilt Modules**: Service modules, database modules
- **Component**: Application component untuk DI

---

## 4. Building the Application

**Refs:** [Req 1.4]

### 4.1 Build Variants

#### Debug Build
```bash
# Via command line
./gradlew assembleDebug

# Via Android Studio
Build > Build Bundle(s)/APK(s) > Build APK(s)
```

#### Release Build
```bash
# Via command line
./gradlew assembleRelease

# Via Android Studio
Build > Generate Signed Bundle/APK
```

### 4.2 Build Configuration

#### gradle.properties
```properties
# Memory settings
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8

# Android settings
android.useAndroidX=true
kotlin.code.style=official
```

#### app/build.gradle Key Settings
```groovy
android {
    compileSdk 34
    defaultConfig {
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
}
```

### 4.3 Build Outputs

#### APK Files
- `app/build/outputs/apk/debug/app-debug.apk` - Debug APK
- `app/build/outputs/apk/release/app-release.apk` - Release APK

#### Bundle Files (Recommended)
- `app/build/outputs/bundle/release/app-release.aab` - Android App Bundle

---

## 5. Configuration

**Refs:** [Req 4.2, 4.3]

### 5.1 Application Configuration

#### AndroidManifest.xml
```xml
<!-- Permissions -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- Features -->
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.bluetooth" android:required="false" />
```

### 5.2 Database Configuration

#### Room Database Setup
```kotlin
@Database(
    entities = [
        Pengguna::class,
        Kategori::class,
        Gudang::class,
        Produk::class,
        // ... other entities
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    // DAOs
}
```

### 5.3 Dependency Injection

#### Hilt Application Class
```kotlin
@HiltAndroidApp
class ChibyChibyApplication : Application()
```

#### Service Module
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideAuthService(
        userRepository: PenggunaRepository
    ): AuthService = AuthServiceImpl(userRepository)
}
```

### 5.4 Runtime Permissions

#### Permission Handling
```kotlin
// Camera permission untuk barcode scanning
val cameraPermission = Manifest.permission.CAMERA

// Storage permission untuk backup
val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE

// Bluetooth permission untuk printing
val bluetoothPermission = Manifest.permission.BLUETOOTH
```

---

## 6. Running the Application

**Refs:** [Req 4.1]

### 6.1 Development Mode

#### Via Android Studio
1. **Connect Device atau Start Emulator**
   - Real device: Enable USB debugging
   - Emulator: AVD Manager > Create Virtual Device

2. **Run Application**
   - Run > Run 'app'
   - Pilih target device
   - Aplikasi akan otomatis install dan launch

#### Via Command Line
```bash
# Install dan run pada connected device
./gradlew installDebug
adb shell am start -n com.chibychibystore/.ui.MainActivity
```

### 6.2 Data Seeding

#### Initial Data Setup
```kotlin
// Pada application startup
val dataSeedingService: DataSeedingService = // inject via Hilt

// Seed default data
dataSeedingService.seedAllData()
```

#### Default Users
- **Owner**: username: "owner", password: "owner123"
- **Manager**: username: "manager", password: "manager123"
- **Cashier**: username: "cashier", password: "cashier123"
- **Warehouse**: username: "warehouse", password: "warehouse123"

### 6.3 Testing Features

#### Barcode Scanning
- **Camera Permission**: Required untuk scan barcode
- **Supported Formats**: EAN-13, Code 128, QR Code
- **Manual Input**: Fallback jika camera tidak tersedia

#### Bluetooth Printing
- **Printer Compatibility**: ESC/POS thermal printers
- **Connection**: Automatic discovery dan pairing
- **Receipt Format**: Customizable receipt layout

---

## 7. Testing

**Refs:** [Req 8.4]

### 7.1 Unit Testing

#### Run Unit Tests
```bash
# Via command line
./gradlew testDebugUnitTest

# Via Android Studio
Run > Run 'All Tests'
```

#### Test Structure
```
app/src/test/java/com/chibychibystore/
├── service/          # Service layer tests
├── repository/       # Repository layer tests
├── ui/              # ViewModel tests
└── data/            # Data layer tests
```

### 7.2 Integration Testing

#### Run Integration Tests
```bash
# Via command line
./gradlew connectedDebugAndroidTest

# Via Android Studio
Run > Run 'All Tests' (with device connected)
```

#### Test Data Setup
```kotlin
// Test database dengan in-memory Room
@get:Rule
val instantTaskExecutorRule = InstantTaskExecutorRule()

private lateinit var database: AppDatabase

@Before
fun setup() {
    database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    ).build()
}
```

### 7.3 UI Testing

#### Compose UI Tests
```kotlin
@Test
fun testLoginScreen() {
    composeTestRule.setContent {
        LoginScreen(viewModel = loginViewModel)
    }

    composeTestRule.onNodeWithText("Username")
        .performTextInput("admin")

    composeTestRule.onNodeWithText("Login")
        .performClick()
}
```

### 7.4 Test Coverage

#### Generate Coverage Report
```bash
./gradlew createDebugCoverageReport
# Report: app/build/reports/coverage/debug/index.html
```

#### Coverage Goals
- **Unit Tests**: 70%+ coverage
- **Integration Tests**: 60%+ coverage
- **UI Tests**: Critical paths 100% coverage

---

## 8. Deployment

**Refs:** [Req 8.1]

### 8.1 Release Build Preparation

#### Generate Signed APK/AAB
```bash
# Generate keystore (first time)
keytool -genkey -v -keystore chiby-chiby-store.jks \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -alias chiby-chiby-store

# Build signed bundle
./gradlew bundleRelease

# Build signed APK
./gradlew assembleRelease
```

#### Signing Configuration
```groovy
android {
    signingConfigs {
        release {
            storeFile file('chiby-chiby-store.jks')
            storePassword 'your_store_password'
            keyAlias 'chiby-chiby-store'
            keyPassword 'your_key_password'
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### 8.2 Google Play Store Deployment

#### Prepare Store Listing
1. **App Details**
   - Title: Chiby Chiby Store
   - Short Description: Aplikasi POS retail offline-first
   - Full Description: Detailed feature list
   - Screenshots: 2-8 screenshots (phone and tablet)

2. **Graphic Assets**
   - Icon: 512x512 PNG
   - Feature Graphic: 1024x500 PNG
   - Promo Graphic: 180x120 PNG (optional)

3. **Content Rating**
   - Rate app content untuk Play Store
   - Target audience: Everyone

#### Upload to Play Console
```bash
# Upload bundle via Play Console web interface
# Or use fastlane for automated deployment
```

### 8.3 Direct APK Distribution

#### For Enterprise/Beta Testing
```bash
# Generate APK untuk distribution
./gradlew assembleRelease

# Distribute APK file:
# - Email attachment
# - Download link
# - MDM solution
```

### 8.4 Version Management

#### Version Code/Name Strategy
```groovy
defaultConfig {
    versionCode 1        // Increment untuk setiap release
    versionName "1.0.0"  // Semantic versioning
}
```

#### Release Notes Template
```
Version 1.0.0 (2025-12-11)
- Initial release
- Inventory management
- POS functionality
- Barcode scanning
- Reporting system

Bug Fixes:
- Fixed login validation
- Improved error handling
```

---

## 9. Backup & Restore

**Refs:** [Req 4.4]

### 9.1 Backup Configuration

#### Automatic Backup
```kotlin
// Schedule weekly backup
val backupScheduler = BackupScheduler(context)
backupScheduler.scheduleWeeklyBackup()
```

#### Manual Backup
```kotlin
val backupService: BackupService = // inject

val result = backupService.createBackup()
when (result) {
    is Result.Success -> {
        // Backup berhasil
        val backupFile = result.data
    }
    is Result.Error -> {
        // Handle error
    }
}
```

### 9.2 Restore Process

#### From Backup File
```kotlin
val restoreService: RestoreService = // inject

// Preview backup content
val preview = restoreService.previewBackup("backup_2025-12-11.enc")

// Restore if confirmed
val result = restoreService.restoreFromBackup("backup_2025-12-11.enc")
```

### 9.3 Backup Security

#### Encryption
- **Algorithm**: AES256-GCM
- **Key Storage**: Android KeyStore
- **File Extension**: .enc (encrypted)

#### Backup Location
- **Directory**: Downloads/ChibyChibyBackup/
- **Naming**: backup_YYYY-MM-DD_HH-MM-SS.enc

---

## 10. Troubleshooting

**Refs:** [Req 4.4]

### 10.1 Build Issues

#### Gradle Sync Failed
```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches/

# Invalidate Android Studio cache
File > Invalidate Caches / Restart
```

#### Out of Memory Error
```bash
# Increase Gradle memory
# gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=1024m
```

#### Dependency Resolution
```bash
# Force refresh dependencies
./gradlew build --refresh-dependencies
```

### 10.2 Runtime Issues

#### App Crashes on Startup
**Possible Causes:**
- Database migration issues
- Missing permissions
- Corrupted data

**Solutions:**
```kotlin
// Check database integrity
val databaseHealth = databaseHealthChecker.checkIntegrity()

// Reset app data
// Settings > Apps > Chiby Chiby Store > Storage > Clear Data
```

#### Camera Not Working
**Symptoms:** Barcode scanner tidak bisa akses camera
**Solutions:**
1. Check camera permission di app settings
2. Restart device
3. Check camera hardware dengan Camera app

#### Bluetooth Printer Issues
**Symptoms:** Printer tidak terdeteksi
**Solutions:**
1. Enable Bluetooth di device settings
2. Pair printer dengan device
3. Check printer power dan paper supply
4. Restart printer dan device

### 10.3 Database Issues

#### Database Corruption
```kotlin
// Backup current data first
val backupResult = backupService.createBackup()

// Clear app data and reinstall
// Or restore from backup
val restoreResult = restoreService.restoreFromBackup(backupFile)
```

#### Migration Errors
```kotlin
// Room automatic migration
@Database(
    entities = [...],
    version = 2,  // Increment version
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
```

### 10.4 Performance Issues

#### Slow App Startup
**Optimization:**
- Reduce initial data loading
- Implement lazy loading
- Optimize database queries dengan indexes

#### High Memory Usage
**Solutions:**
- Monitor memory dengan Android Profiler
- Fix memory leaks di ViewModels
- Optimize image loading

#### Battery Drain
**Causes:**
- Continuous GPS untuk location
- Frequent network requests
- Background services

**Solutions:**
- Optimize location updates
- Batch network requests
- Use WorkManager untuk background tasks

### 10.5 Testing Issues

#### Tests Failing Randomly
```bash
# Run tests with retry
./gradlew test --rerun-tasks

# Check test isolation
# Ensure each test cleans up after itself
```

#### Emulator Issues
```bash
# Cold boot emulator
emulator -avd <avd_name> -no-snapshot-load

# Wipe emulator data
emulator -avd <avd_name> -wipe-data
```

---

## 11. Performance Optimization

**Refs:** [Req 4.1]

### 11.1 Build Optimization

#### Gradle Configuration
```gradle
// gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

#### ProGuard/R8 Configuration
```proguard
# Enable optimization
-optimizationpasses 5
-allowaccessmodification

# Keep Compose classes
-keep class androidx.compose.** { *; }
```

### 11.2 Runtime Optimization

#### Database Optimization
```kotlin
// Add indexes untuk frequently queried columns
@Entity(indices = [
    Index(value = ["barcode"]),
    Index(value = ["kategori_id"]),
    Index(value = ["tanggal_penjualan"])
])
```

#### Memory Optimization
```kotlin
// Use ViewModel for data caching
class ProductViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    // Cache data selama lifecycle
}
```

#### UI Optimization
```kotlin
// Use LazyColumn untuk large lists
LazyColumn {
    items(products) { product ->
        ProductItem(product = product)
    }
}
```

### 11.3 Network Optimization (Future)
```kotlin
// Batch API calls
val deferredResults = products.map { async { api.getProduct(it.id) } }
val results = deferredResults.awaitAll()
```

---

## 12. Security Configuration

**Refs:** [Req 4.2]

### 12.1 Data Encryption

#### Database Encryption
```kotlin
// SQLCipher untuk encrypted database (future enhancement)
val database = Room.databaseBuilder(context, AppDatabase::class.java, "chiby.db")
    .openHelperFactory(SupportFactory(passphrase))
    .build()
```

#### Backup Encryption
```kotlin
// AES256-GCM encryption
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedFile = EncryptedFile.Builder(
    context,
    backupFile,
    masterKey,
    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
).build()
```

### 12.2 Authentication Security

#### Password Hashing
```kotlin
// SHA-256 password hashing
val digest = MessageDigest.getInstance("SHA-256")
val hash = digest.digest(password.toByteArray())
val hashedPassword = hash.joinToString("") { "%02x".format(it) }
```

#### Session Management
```kotlin
// Secure session storage
val sharedPrefs = EncryptedSharedPreferences.create(
    "session_prefs",
    masterKey,
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

### 12.3 Permission Management

#### Runtime Permissions
```kotlin
// Request critical permissions
val permissions = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.BLUETOOTH
)

ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE)
```

#### Permission Checks
```kotlin
// Check permissions sebelum sensitive operations
if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    != PackageManager.PERMISSION_GRANTED) {
    // Request permission
}
```

### 12.4 Code Security

#### ProGuard Rules
```proguard
# Obfuscate code
-dontobfuscate

# Keep sensitive classes
-keep class com.chibychibystore.service.AuthService { *; }
-keep class com.chibychibystore.data.local.entity.Pengguna { *; }
```

#### API Security (Future)
```kotlin
// HTTPS only
android:usesCleartextTraffic="false"

// Certificate pinning (future)
```

---

## 13. Monitoring & Maintenance

### 13.1 Logging Configuration

#### Timber Logging
```kotlin
// Initialize Timber
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
} else {
    Timber.plant(CrashReportingTree())
}
```

#### Log Levels
```kotlin
Timber.d("Debug message")    // Development only
Timber.i("Info message")     // General information
Timber.w("Warning message")  // Warnings
Timber.e("Error message")    // Errors
```

### 13.2 Crash Reporting

#### Firebase Crashlytics (Future)
```kotlin
// Initialize Crashlytics
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

// Log custom events
FirebaseCrashlytics.getInstance().log("Custom event")
```

### 13.3 Analytics (Future)

#### Firebase Analytics
```kotlin
// Track user interactions
firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
    param(FirebaseAnalytics.Param.ITEM_NAME, "Product Scanned")
}
```

---

## 14. Support Resources

### 14.1 Documentation
- [User Manual](PANDUAN_PENGUNA.md) - End-user guide
- [API Documentation](API_DOCUMENTATION.md) - Developer API reference
- [Database Design](database-design.md) - Database schema documentation
- [Backend Design](backend-design.md) - Architecture documentation

### 14.2 Community Support
- GitHub Issues: Bug reports dan feature requests
- Documentation Wiki: Extended guides dan tutorials
- Stack Overflow: Tag `chiby-chiby-store`

### 14.3 Professional Support
- Email: support@chibychibystore.com
- Business Hours: Monday-Friday, 9AM-5PM WIB
- Response Time: 24 hours untuk critical issues

---

## 15. Checklist Deployment

### Pre-Deployment
- [ ] All tests passing (unit, integration, UI)
- [ ] Code review completed
- [ ] Security audit passed
- [ ] Performance benchmarks met
- [ ] Documentation updated

### Deployment Steps
- [ ] Generate signed APK/AAB
- [ ] Upload to Play Store / distribute APK
- [ ] Update release notes
- [ ] Notify stakeholders
- [ ] Monitor crash reports

### Post-Deployment
- [ ] Monitor user feedback
- [ ] Track crash reports
- [ ] Plan next release
- [ ] Update documentation

---

*Setup and Deployment Guide ini menyediakan panduan lengkap untuk development, testing, dan deployment aplikasi Chiby Chiby Store. Pastikan mengikuti semua langkah dengan urut untuk hasil yang optimal.*