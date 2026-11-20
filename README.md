# BECHIR FILES PRO — prototype 0.1.0
Native Android Kotlin / Jetpack Compose starter. Arabic-first UI, light/dark toggle, 3-column grid, folder picker through Storage Access Framework, navigation, search, and open-with intent. No internet permission. No deletion or modification yet.

## Build
Open folder in Android Studio with JDK 17 and Android SDK 35. Sync Gradle, then Build > Build APK(s). Internet is needed for first Gradle dependency download.

## Known limits
Only selected folders and accessible descendants. Android/data and other protected paths are unavailable. This source has not been compiled in this environment (Android SDK/Gradle not installed). Test on an actual device before distribution.

## GitHub Actions APK build (no computer required)
Upload all project files and the `.github/workflows/build-apk.yml` file to a GitHub repository's `main` branch. The workflow runs on push or via Actions > Build Android APK > Run workflow. On success, download `BECHIR-FILES-PRO-debug-APK` from the workflow run's Artifacts section. The downloaded artifact is a ZIP containing `app-debug.apk`. This project has not yet been compiled or tested on a device.
