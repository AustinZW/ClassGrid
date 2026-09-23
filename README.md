# ClassGrid

[简体中文](README.zh-CN.md)

A Material 3 timetable app for Android, built with Kotlin and Jetpack Compose.

ClassGrid is a local-first schedule manager with flexible semester, week, section, and course-slot settings. It can also import courses from Donghua University's undergraduate academic system.

## Features

- Weekly timetable with horizontal swipe navigation
- Weekday, date, section, and customizable class-time display
- Custom class days, sections per day, course-cell height, and font sizes
- Multiple rooms, weekdays, sections, and teaching-week ranges per course
- Add, edit, copy, drag, and resize courses
- Conflict detection with interactive conflict resolution during imports
- RGB, HSB, color-wheel, full-gamut, and Hex course-color controls
- Sign in to Donghua University's academic system and selectively import courses
- Create a new timetable with an option to export the current one first
- Import, export, and migrate timetable data using `.cgf` files
- Material 3 dynamic colors and dark mode
- Local persistence with Room and Preferences DataStore

## Academic System Import

Open the settings menu in the top-right corner and select **Import from Academic System**:

1. Complete authentication inside the app.
2. Wait for ClassGrid to open the timetable automatically, or tap the fallback navigation button.
3. Tap the recognition button in the lower-right corner and select the courses to import.
4. Import them into the current timetable or create a new timetable.

The embedded import page is restricted to Donghua University domains. ClassGrid does not read or store authentication passwords.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Preferences DataStore
- ViewModel, StateFlow, and unidirectional data flow

## Requirements

- Android Studio
- JDK 17
- Android SDK 35
- Android 8.0 (API 26) or later

## Build and Test

Open the project in Android Studio, wait for Gradle Sync to complete, and run the `app` configuration. On Windows, you can also use PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Data Format

`.cgf` (ClassGrid File) is ClassGrid's portable data format. It contains timetable settings and course data. Files are validated for format, version, and data ranges before import.

## Privacy

Courses and settings are stored locally on the device by default. ClassGrid does not provide cloud sync and does not store academic-system credentials in its database.

## License

This project is licensed under the [MIT License](LICENSE).
