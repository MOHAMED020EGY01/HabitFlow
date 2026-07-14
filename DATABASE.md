# Database and Persistence Design

## Primary Storage

HabitFlow uses a local SQLite database via Room for core app data and Android DataStore for settings.

## Room Database

The main database is named habit_database and is defined by HabitDatabase.

### Core Entities

- HabitEntity: stores the main habit definition, scheduling properties, status, and lifecycle metadata.
- HabitLogEntity: stores daily completion logs for habits.
- HabitCycleHistoryEntity: stores completed or failed cycle snapshots.
- NotificationEntity: stores notification records for reminder and alert history.

### Relationships and Behavior

- Habit logs and cycle history are tied to habits.
- The database uses migrations to evolve the schema across versions.
- WAL mode and incremental vacuum are enabled for better write performance and storage reclamation.

## Persistence Responsibilities

### Room

Room handles:

- Habit CRUD operations
- Daily log insertion and toggling
- Cycle history storage
- Reminder notification storage

### DataStore

DataStore handles:

- User name and photo
- Onboarding status
- Theme/language preferences
- Background service state
- Glass effects and navbar preferences
- Last rollover marker

## Schema Notes

The habit entity includes fields for:

- Name, description, and color
- Duration and active/inactive status
- Reminder times and active days
- Cycle start/end timestamps
- Inactive days count
- Inactive timestamp

## Migration History

The current database version is 10. The migration set includes schema changes for:

- Index creation
- Notification table creation
- Inactive timestamp support
- Cycle-related fields and table restructuring
- Reminder time format changes

## Operational Considerations

- The app uses a local-first design and therefore avoids a remote persistence layer.
- Backup behavior is enabled through Android backup rules, so data portability should be reviewed carefully for privacy and retention.
