# 06_DATA_FLOW — مسارات تدفق البيانات / Interactive Data Flows

يوضح هذا المستند كيفية تدفق البيانات بين المكونات المختلفة للتطبيق (من الشاشة إلى قاعدة البيانات والخلفية) في أهم العمليات الحيوية:

This document details how data flows across layers (from UI to Database to Background Workers) for the main runtime operations:

---

## 1. مسار إضافة عادة جديدة / New Habit Creation Flow

عند قيام المستخدم بإدخال بيانات عادة جديدة والضغط على "حفظ":

When a user inputs new habit details and taps "Save":

```mermaid
sequenceDiagram
    participant UI as AddHabitScreen (Compose)
    participant VM as AddHabitViewModel
    participant UC_Val as ValidateReminderTimeUseCase
    participant UC_Add as AddHabitUseCase
    participant Repo as HabitRepositoryImpl
    participant DB as HabitDao (Room)
    participant WM as WorkManager (Workers)

    UI->>VM: saveHabit()
    VM->>UC_Val: validate(proposedTimes, days)
    UC_Val->>Repo: getAllHabitsSync()
    Repo->>DB: query habits
    DB-->>UC_Val: list of habits
    Note over UC_Val: Verify 10-minute gap circular check
    UC_Val-->>VM: ValidationResult (Valid)
    
    VM->>UC_Add: invoke(habit)
    UC_Add->>Repo: getActiveHabitsCount()
    Note over UC_Add: Check if active < 6 limit
    UC_Add->>Repo: insertHabit(habit)
    Repo->>DB: insert into habits (SQL)
    DB-->>UC_Add: habitId
    UC_Add-->>VM: ActivationResult.Activated
    
    par Reschedule & Sync in applicationScope
        VM->>WM: HabitReminderWorker.scheduleHabitReminders()
        VM->>WM: HabitWidgetSyncUpdater.updateNowForced()
    end
    
    VM-->>UI: Navigate back to Home
```

---

## 2. مسار تسجيل الإنجاز اليومي بنقرة سريعة / Toggle Daily Check-in Flow

سواء تم تسجيل الإنجاز من لوحة التحكم أو من قطعة الواجهة التفاعلية (Widget):

Whether triggered from the Home Dashboard UI or a Glance Home Screen Widget:

```mermaid
sequenceDiagram
    participant UI as HomeScreen / Glance Widget
    participant Action as MarkHabitDoneAction / HomeViewModel
    participant Repo as HabitRepositoryImpl
    participant DB as Room SQLite (habit_logs)
    participant Mgr as HabitStatusManager
    participant Sync as HabitWidgetSyncUpdater

    UI->>Action: toggleCheckIn(habitId, date, completed)
    Action->>Repo: toggleLogForDate(habitId, date, completed)
    
    alt completed == true
        Repo->>DB: insertLog(habit_logs)
    else completed == false
        Repo->>DB: deleteLogForDate(habit_logs)
    end
    
    opt If completed == true
        Action->>Mgr: checkHabitCompletion(habitId)
        Note over Mgr: Check if last day and complete cycle
    end
    
    Action->>Sync: updateNowForced(context)
    Note over Sync: Bypasses Glance limits & updates RemoteViews
```

---

## 3. مسار الالتفاف الليلي التلقائي / Nightly Auto-Rollover Flow

يحدث تلقائياً عند منتصف الليل لإعادة تعيين الحالة ومعالجة الغيابات والتوقف التلقائي:

Triggers automatically at 12:00 AM daily to log missing days, check auto-pauses, and archive completed cycles:

```mermaid
sequenceDiagram
    participant WM as WorkManager (DailyRolloverWorker)
    participant Mgr as HabitStatusManager
    participant Repo as HabitRepositoryImpl
    participant DB as Room SQLite (habits & logs)
    participant OS as NotificationManager

    WM->>Mgr: performDailyRollover(Context, Repository)
    Mgr->>Repo: getAllHabitsSync()
    Repo->>DB: SELECT * FROM habits
    DB-->>Mgr: list of habits

    loop for each Active/Inactive habit
        Mgr->>Repo: getLogsForHabitSync(habitId)
        Note over Mgr: Fill missing MISS (active) or INACTIVE_SKIPPED (inactive)
        Mgr->>Repo: insertLogsBulk(missingLogs)
        DB->>DB: INSERT INTO habit_logs
        
        opt If 3 consecutive MISS logs (ACTIVE habits)
            Note over Mgr: Change state to INACTIVE, set isActive = false
            Mgr->>Repo: updateHabit(habit)
            Mgr->>OS: sendInactivityNotification()
            Mgr->>Repo: insertNotification(PAUSE log)
        end
        
        opt If cycleEndDate reached
            Note over Mgr: Compute rate. If >= 90% COMPLETE, else FAILURE
            Mgr->>Repo: updateHabit(habit)
            Mgr->>Repo: insertCycleHistory(cycleSummary)
        end
    end
    
    WM->>Repo: saveLastRolloverDate(today)
```

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم التحقق من تتابع المكالمات البرمجية والتحريكات من فئات ViewModels وحالات الاستعمال ومستندات العمال.
* **Files Used / الملفات المستخدمة**:
  - [AddHabitViewModel.kt](app/src/main/java/com/example/presentation/screens/add/AddHabitViewModel.kt#L198-L283)
  - [HomeViewModel.kt](app/src/main/java/com/example/presentation/screens/home/HomeViewModel.kt#L157-L182)
  - [HabitStatusManager.kt](app/src/main/java/com/example/domain/usecase/HabitStatusManager.kt#L27-L103)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
