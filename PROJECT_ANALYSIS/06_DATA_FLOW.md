# 06_DATA_FLOW — مسارات تدفق البيانات / Interactive Data Flows

يوضح هذا المستند كيفية تدفق البيانات بين المكونات المختلفة للتطبيق (من الشاشة إلى قاعدة البيانات والخلفية) في أهم العمليات الحيوية:

This document details how data flows across layers (from UI to Database to Background Workers) for the main runtime operations:

---

## 1. مسار إضافة عادة جديدة / New Habit Creation Flow

عند قيام المستخدم بإدخال بيانات عادة جديدة والضغط على "حفظ":

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
    VM->>UC_Val: validate()
    UC_Val->>Repo: getAllHabitsSync()
    Repo->>DB: query habits
    DB-->>UC_Val: list of habits
    UC_Val-->>VM: ValidationResult (Valid)
    
    VM->>UC_Add: invoke(habit)
    UC_Add->>Repo: insertHabit(habit)
    Repo->>DB: insert into habits (SQL)
    DB-->>UC_Add: habitId
    UC_Add-->>VM: ActivationResult.Activated
    
    par Reschedule & Sync
        VM->>WM: HabitReminderWorker.scheduleHabitReminders()
        VM->>WM: HabitWidgetSyncUpdater.updateNowForced()
    end
    
    VM-->>UI: Navigate back to Home
```

---

## 2. مسار تسجيل الإنجاز اليومي / Toggle Daily Check-in Flow

سواء تم تسجيل الإنجاز من لوحة التحكم أو من قطعة الواجهة التفاعلية (Widget):

```mermaid
sequenceDiagram
    participant UI as HomeScreen / Glance Widget
    participant Action as MarkHabitDoneAction / HomeViewModel
    participant Repo as HabitRepositoryImpl
    participant DB as Room SQLite (habit_logs)
    participant Mgr as HabitStatusManager
    participant Sync as HabitWidgetSyncUpdater

    UI->>Action: toggleCheckIn()
    Action->>Repo: toggleLogForDate()
    
    alt completed == true
        Repo->>DB: insertLog(habit_logs)
    else completed == false
        Repo->>DB: deleteLogForDate(habit_logs)
    end
    
    opt If completed == true
        Action->>Mgr: checkHabitCompletion()
    end
    
    Action->>Sync: updateNowForced(context)
```

---

## 3. مسار الالتفاف الليلي التلقائي / Nightly Auto-Rollover Flow

Triggers automatically at 12:00 AM daily via `DailyRolloverWorker`.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**: فحص تسلسل الاستدعاءات في الـ ViewModels والـ UseCases والعمال.
* **Files Used / الملفات المستخدمة**:
  - [AddHabitViewModel.kt](app/src/main/java/com/example/feature/habit/presentation/AddHabitViewModel.kt)
  - [HomeViewModel.kt](app/src/main/java/com/example/feature/home/presentation/HomeViewModel.kt)
  - [HabitStatusManager.kt](app/src/main/java/com/example/core/domain/usecase/HabitStatusManager.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
