# HabitFlow

HabitFlow is a feature-rich Android habit-tracking app built with Kotlin and Jetpack Compose. The current implementation is local-first and focuses on daily habit completion, reminders, progress tracking, cycle management, widgets, and personalized settings.

## What the App Includes

- Habit creation and editing with reminders, duration, colors, and weekly scheduling
- Daily log tracking with completion, miss, and cycle-state handling
- Auto-pause and cycle-completion logic for long-running habits
- Reminder overlays, notification channels, and background scheduling
- Home-screen widgets for active and inactive habits
- Localization, onboarding, settings, and reset flows

## Architecture Snapshot

The project uses a layered architecture:

- Presentation: Compose screens and ViewModels
- Domain: use cases and habit lifecycle rules
- Data: Room persistence, repository implementations, and DataStore preferences
- Platform: WorkManager, foreground services, widgets, and broadcast receivers

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- Room
- DataStore
- WorkManager
- Glance
- Gradle with Kotlin DSL

## Project Structure

- app/src/main/java/com/example: application entry points, screens, viewmodels, and platform integrations
- app/src/main/java/com/example/data: repositories, database, workers, and preferences
- app/src/main/java/com/example/domain: models and use cases
- app/src/test: unit tests
- app/src/androidTest: instrumentation tests and benchmark coverage

## Getting Started

Prerequisites:

- Android Studio
- JDK 11+
- A connected emulator or physical device

Steps:

- Open the project in Android Studio.
- Sync Gradle and let Android Studio resolve dependencies.
- Create a local .env file if you need environment-based secrets for local builds.
- Run the app from the project root or the app module.

## Documentation

Additional project documentation is available in:

- PROJECT_ANALYSIS.md
- ARCHITECTURE.md
- DATABASE.md
- SECURITY_AUDIT.md
- PERFORMANCE_AUDIT.md
- TECH_STACK.md
- ROADMAP.md

## Notes

The app currently uses local persistence and device features rather than a remote backend. Some Android permissions and reminders are intentionally feature-rich and should be reviewed carefully in release builds.
