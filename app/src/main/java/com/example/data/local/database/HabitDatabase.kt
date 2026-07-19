package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.HabitDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.HabitLogEntity
import com.example.data.local.entity.HabitCycleHistoryEntity
import com.example.data.local.entity.NotificationEntity

@Database(
    entities = [
        HabitEntity::class, 
        HabitLogEntity::class, 
        HabitCycleHistoryEntity::class,
        NotificationEntity::class
    ],
    version = 12,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: HabitDatabase? = null

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Refresh identity hash
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN reminderVoice TEXT NOT NULL DEFAULT 'DEFAULT'")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habits_startedAt ON habits (startedAt)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        type TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN inactiveSinceTimestamp INTEGER")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // habits table updates
                db.execSQL("ALTER TABLE habits ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
                db.execSQL("ALTER TABLE habits ADD COLUMN cycleStartDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habits ADD COLUMN cycleEndDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habits ADD COLUMN inactiveDaysCount INTEGER NOT NULL DEFAULT 0")
                
                // Populate default values for existing habits
                db.execSQL("UPDATE habits SET cycleStartDate = CASE WHEN startedAt IS NOT NULL THEN startedAt ELSE createdAt END")
                db.execSQL("UPDATE habits SET cycleEndDate = cycleStartDate + durationDays * 86400000")
                
                // habit_logs table updates
                db.execSQL("ALTER TABLE habit_logs ADD COLUMN state TEXT NOT NULL DEFAULT 'DONE'")
                db.execSQL("UPDATE habit_logs SET state = CASE WHEN completed = 1 THEN 'DONE' ELSE 'MISS' END")
                
                // create habit_cycle_history table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_cycle_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        habitId INTEGER NOT NULL,
                        cycleStartDate INTEGER NOT NULL,
                        cycleEndDate INTEGER NOT NULL,
                        completionPercentage REAL NOT NULL,
                        result TEXT NOT NULL,
                        logsSnapshot TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Clean up any orphaned log entries or history entries for deleted habits
                db.execSQL("DELETE FROM habit_logs WHERE habitId NOT IN (SELECT id FROM habits)")
                db.execSQL("DELETE FROM habit_cycle_history WHERE habitId NOT IN (SELECT id FROM habits)")

                // 2. Recreate habit_logs table with ForeignKey constraint
                db.execSQL("ALTER TABLE habit_logs RENAME TO habit_logs_old")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        habitId INTEGER NOT NULL,
                        logDate TEXT NOT NULL,
                        completed INTEGER NOT NULL,
                        state TEXT NOT NULL DEFAULT 'DONE',
                        FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO habit_logs (id, habitId, logDate, completed, state)
                    SELECT id, habitId, logDate, completed, state FROM habit_logs_old
                """.trimIndent())
                db.execSQL("DROP TABLE habit_logs_old")

                // Create indices for habit_logs
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_logs_habitId ON habit_logs (habitId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habit_logs_habitId_logDate ON habit_logs (habitId, logDate)")

                // 3. Recreate habit_cycle_history table with ForeignKey constraint
                db.execSQL("ALTER TABLE habit_cycle_history RENAME TO habit_cycle_history_old")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_cycle_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        habitId INTEGER NOT NULL,
                        cycleStartDate INTEGER NOT NULL,
                        cycleEndDate INTEGER NOT NULL,
                        completionPercentage REAL NOT NULL,
                        result TEXT NOT NULL,
                        logsSnapshot TEXT NOT NULL,
                        FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO habit_cycle_history (id, habitId, cycleStartDate, cycleEndDate, completionPercentage, result, logsSnapshot)
                    SELECT id, habitId, cycleStartDate, cycleEndDate, completionPercentage, result, logsSnapshot FROM habit_cycle_history_old
                """.trimIndent())
                db.execSQL("DROP TABLE habit_cycle_history_old")

                // Create index for habit_cycle_history
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_cycle_history_habitId ON habit_cycle_history (habitId)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habits_isActive ON habits (isActive)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habits_createdAt ON habits (createdAt)")
            }
        }

        /** Converts existing JSON-encoded reminderTimes to comma-separated format.
         *  Input:  '["09:00","14:00"]' or '[]' or NULL
         *  Output: '09:00,14:00' or NULL
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN activeDays TEXT NOT NULL DEFAULT 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    UPDATE habits 
                    SET reminderTimes = 
                        CASE 
                            WHEN reminderTimes IS NULL 
                                 OR reminderTimes = '[]' 
                                 OR reminderTimes = '' 
                            THEN NULL
                            ELSE REPLACE(REPLACE(REPLACE(reminderTimes, '[', ''), ']', ''), '"', '')
                        END
                """)
            }
        }

        fun getDatabase(context: Context): HabitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habit_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("PRAGMA auto_vacuum = INCREMENTAL")
                    }
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // WAL is typically handled by Room, explicit PRAGMA can cause issues if it returns results
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

