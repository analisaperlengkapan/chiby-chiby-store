# Chiby Chiby Store POS

A modern, native Android Point of Sale (POS) and Inventory Management system designed for retail businesses. Built with Jetpack Compose and adhering to Clean Architecture principles, Chiby Chiby Store offers a robust offline-first solution for managing sales, stock, and finances.

## Key Features

*   **Point of Sale (POS)**: Fast transaction processing with integrated barcode scanning (via CameraX/ZXing) and receipt printing (Thermal ESC/POS).
*   **Inventory Management**: Comprehensive tracking of products, categories, and warehouses with real-time stock updates.
*   **Financial Management**:
    *   **Expense Tracking**: Monitor operating and capital expenses.
    *   **Cash Flow**: Analyze operating, investing, and financing cash flows.
    *   **Balance Sheet**: Real-time generation of assets, liabilities, and equity reports.
*   **Reporting & Analytics**: Visual insights into sales trends, gross profit, and product performance with PDF export capabilities.
*   **User Management**: Role-Based Access Control (RBAC) to secure sensitive operations.
*   **Data Security**: Encrypted backup and restore functionality using AES-256-GCM.
*   **Offline-First**: Reliable operation without constant internet connectivity using local Room database persistence.

## Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material 3)
*   **Architecture**: MVVM (Model-View-ViewModel) with Service Pattern
*   **Dependency Injection**: Dagger Hilt
*   **Database**: Room (SQLite)
*   **Asynchronous Programming**: Coroutines & Flow
*   **Hardware Integration**:
    *   **Camera**: CameraX (Barcode Scanning)
    *   **Printing**: ESC/POS Thermal Printer Library
*   **Security**: AndroidX Security Crypto
*   **Testing**: JUnit, Mockito, Robolectric

## Architecture Overview

The application follows a strictly layered architecture to ensure separation of concerns and testability:

1.  **UI Layer (Presentation)**: Composable functions and ViewModels. ViewModels expose state via `StateFlow` and interact with the Service Layer.
2.  **Service Layer (Domain)**: Encapsulates all business logic and transaction management. It acts as the bridge between the UI and Data layers.
3.  **Data Layer**: Responsible for data persistence and retrieval, consisting of Repositories, Room DAOs, and Entities.

## Prerequisites

*   **Android Studio**: Ladybug | 2024.2.1 or newer recommended.
*   **JDK**: Version 11 or 17.
*   **Android SDK**: API Level 36 (Compile), API Level 35 (Target).

## Setup & Build

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/your-username/chiby-chiby-store.git
    ```
2.  **Open in Android Studio**:
    Select "Open" and navigate to the project root. Allow Gradle to sync.
3.  **Build the project**:
    ```bash
    ./gradlew assembleDebug
    ```
4.  **Run**:
    Select an emulator or connected device and click the **Run** button (Shift+F10).

## Testing

The project includes a comprehensive suite of unit tests covering ViewModels, Services, and Repositories.

Run unit tests via Gradle:
```bash
./gradlew testDebugUnitTest
```

## License

This project is proprietary software.

---
*Generated based on codebase analysis - December 2025*
