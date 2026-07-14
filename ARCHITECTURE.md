# Architecture Overview

## High-Level Structure

HabitFlow follows a layered architecture that keeps UI concerns separate from persistence and domain rules.

- Presentation layer: Compose screens, view models, navigation, and reusable UI components.
- Domain layer: use cases and domain models for habit lifecycle rules.
- Data layer: repositories, Room DAO/entity implementations, DataStore preferences, and background workers.
- Platform layer: Android services, broadcast receivers, widgets, and notification handling.

## Main Runtime Flow

1. The application starts through the custom Application class.
2. The app initializes preferences, the Room database, repositories, and use cases.
3. MainActivity hosts the navigation graph and theme/language context.
4. Screens interact with ViewModels, which delegate to use cases and repository methods.
5. Background workers and services handle rollover, reminders, and widget refreshes.

## Core Components

### Application Bootstrap

The custom application class wires the app together and schedules background work after startup.

### Navigation

MainActivity contains the root navigation graph and deep-link handling for screens such as add habit and habit detail.

### Screens

The main screens include:

- Home
- Add/Edit Habit
- Habit Detail
- All Habits
- Summary
- Settings
- Onboarding and Splash

### Data Management

- Room is the primary persistence engine for habits, logs, cycle history, and notifications.
- DataStore stores user preferences such as language, onboarding state, and display settings.

### Background Processing

- WorkManager handles scheduled periodic tasks.
- Foreground services support overlay reminders and keep-alive behavior.
- Broadcast receivers respond to boot events and overlay actions.

## Notable Design Choices

- The app is intentionally local-first and does not rely on a remote API in the current implementation.
- Habit lifecycle rules are concentrated in domain use cases to keep the UI simple.
- Widgets are updated both by worker scheduling and by direct refresh logic to keep data in sync quickly.

## Suggested Mental Model

If you are working on a feature, follow this pattern:

1. Update the UI state in the screen/ViewModel.
2. Invoke the relevant domain use case.
3. Let the repository write to Room or DataStore.
4. Trigger any needed reminder/widget sync through the background layer.
