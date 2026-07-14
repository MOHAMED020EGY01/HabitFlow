# Technology Stack

## Platform

- Android application targeting API 24+.
- Built with Kotlin and Jetpack Compose.

## Core Libraries

- Jetpack Compose Material 3
- Navigation Compose
- Lifecycle and ViewModel
- Room for persistence
- DataStore for preferences
- WorkManager for background scheduling
- Glance for home-screen widgets
- Splash Screen API

## Build and Tooling

- Gradle with Kotlin DSL
- AGP 9.1.1
- Kotlin 2.2.10
- KSP for annotation processing
- Secrets Gradle Plugin for environment-based config
- Google Services plugin integration

## Testing

- JUnit
- Robolectric
- Compose UI testing
- MockK
- Turbine
- Android instrumentation testing

## Notes

The project also includes Retrofit, OkHttp, Moshi, and Firebase-related dependencies, but the current verified implementation uses local persistence and platform features rather than a remote backend.
