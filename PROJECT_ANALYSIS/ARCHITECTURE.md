# ARCHITECTURE

## النمط المعماري الكامل / Full Architecture Overview

### الفلسفة التصميمية المتبعة
يتبع المشروع مزيجاً من **Clean Architecture** و **MVVM**، مع حقن اعتماديات يدوي بدلاً من Hilt. المبدأ الجوهري هو عزل المنطق التجاري (Domain) عن تفاصيل التخزين (Data) وعن واجهة المستخدم (Presentation) لتمكين الاختبار وسهولة الصيانة.

### Design Philosophy
HabitFlow employs a hybrid of **Clean Architecture** and **MVVM**, with manual dependency injection replacing Hilt. The core principle is to isolate business logic (Domain) from storage details (Data) and from the UI (Presentation) to enable robust testing and maintainability.

---

## الطبقات المعمارية / Architectural Layers

### 1. طبقة العرض (Presentation Layer)
**المسؤولية**: عرض الحالة للمستخدم واستقبال تفاعلاته وتمريرها للطبقات الداخلية.

المكونات الرئيسية:
- **Screens** (`presentation/screens/`): شاشات Jetpack Compose.
- **ViewModels**: تحتفظ بالحالة، تستدعي UseCases والمستودعات، وتبث الحالة عبر `StateFlow`.
- **Components** (`presentation/components/`): عناصر مشتركة قابلة لإعادة الاستخدام (بطاقات زجاجية، حلقات تقدم، منتقيات الوقت).
- **Navigation** (`presentation/navigation/`): مسار التنقل `AppNavigation` يربط جميع الشاشات عبر Compose Navigation.

**Presentation Layer Responsibilities**: Renders UI state and forwards interactions to inner layers.
- **Screens**: Feature-specific Compose screens.
- **ViewModels**: Retain state, bridge use cases/repositories, and emit state via `StateFlow`.
- **Components**: Reusable premium composables (glass cards, progress rings, time pickers).
- **Navigation**: `AppNavigation` connects all screens in a single Compose NavHost graph.

---

### 2. طبقة النطاق (Domain Layer)
**المسؤولية**: الكود التجاري النقي المستقل عن تفاصيل أندرويد أو قاعدة البيانات.

المكونات الرئيسية:
- **Models** (`domain/model/`): نماذج بيانات نظيفة (مثل `Habit`, `HabitLog`, `HabitCycleHistory`).
- **Repository Interfaces** (`domain/repository/`): عقد برمجي تُحددها هذه الطبقة وتُنفذها طبقة البيانات.
- **Use Cases** (`domain/usecase/`): منطق العمل المعزول والقابل للاختبار مستقلاً.
- **Utilities** (`domain/util/`): أدوات حسابية مستقلة مثل `StreakCalculator` و`NextReminderCalculator`.

**Domain Layer Responsibilities**: Pure Kotlin business rules with zero framework dependencies.
- **Models**: Clean domain objects (`Habit`, `HabitLog`, `HabitCycleHistory`).
- **Repository Interfaces**: Abstract contracts fulfilled by the Data layer.
- **Use Cases**: Isolated, independently testable business logic units.
- **Utilities**: Framework-free calculation utilities (`StreakCalculator`, `NextReminderCalculator`).

---

### 3. طبقة البيانات (Data Layer)
**المسؤولية**: تنفيذ عقود طبقة النطاق عبر قاعدة البيانات والتفضيلات ونظام أندرويد.

المكونات الرئيسية:
- **Repository Implementations** (`data/repository/`): يُنفذ `HabitRepositoryImpl` واجهة `HabitRepository` ويتولى التحويل بين كيانات Room ونماذج النطاق.
- **Room** (`data/local/`): يحتوي على تعريف القاعدة `HabitDatabase`، وواجهات الاستعلام `HabitDao` و`NotificationDao`، وكيانات الجداول.
- **DataStore** (`data/preferences/`): `UserPreferencesManager` و`PendingOverlayStore`.
- **Workers** (`data/worker/`): عمال WorkManager للمهام الخلفية الدورية.

**Data Layer Responsibilities**: Fulfills Domain contracts with concrete Android implementations.
- **Repository Implementations**: `HabitRepositoryImpl` maps Room entities to domain models and vice versa.
- **Room Database**: Schema declaration, DAOs, and entity definitions.
- **DataStore**: Preference management and pending overlay queuing.
- **Workers**: Scheduled periodic tasks via WorkManager.

---

### 4. طبقة المنصة (Platform Layer)
**المسؤولية**: التكامل مع نظام التشغيل أندرويد مباشرة.

المكونات الرئيسية:
- **Services**: `HabitBackgroundService` (keepalive) و`HabitOverlayService` (عرض النافذة العائمة).
- **Broadcast Receivers**: `BootReceiver` و`PendingOverlayReceiver` و`HabitOverlayReceiver`.
- **Widgets** (`widget/`): `AllHabitsWidget` و`InactiveHabitsWidget` بتقنية Glance.
- **Notifications**: تكوين قنوات الإشعارات وبثها عبر `HabitNotificationManager`.

**Platform Layer Responsibilities**: Direct integration with Android OS features.
- **Services**: Background keepalive and overlay window management.
- **Broadcast Receivers**: Boot, unlock, and overlay cycle management.
- **Widgets**: Glance AppWidgets with direct update bypass for responsiveness.
- **Notifications**: Channel setup and reminder broadcasting.

---

## مخطط تدفق البيانات الكامل / Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────┐
│               PRESENTATION LAYER                    │
│  ┌───────────┐  StateFlow  ┌─────────────────────┐  │
│  │  Screens  │◄────────────│    ViewModels        │  │
│  │ (Compose) │             │  (HomeViewModel,     │  │
│  └───────────┘             │   SettingsViewModel) │  │
└─────────────────────────────────────────────────────┘
           │ calls UseCases & Repository directly
           ▼
┌─────────────────────────────────────────────────────┐
│                 DOMAIN LAYER                        │
│  ┌───────────────┐    ┌──────────────────────────┐  │
│  │   Use Cases   │    │   Repository Interface   │  │
│  │ (AddHabit,    │    │   (HabitRepository)      │  │
│  │  DeleteHabit) │    └──────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                              │ implemented by
                              ▼
┌─────────────────────────────────────────────────────┐
│                  DATA LAYER                         │
│  ┌──────────────────┐   ┌──────────────────────┐    │
│  │ HabitRepositoryImpl│  │ HabitDatabase (Room) │    │
│  │  (mapping layer) │◄──│  HabitDao            │    │
│  └──────────────────┘   └──────────────────────┘    │
│  ┌──────────────────┐   ┌──────────────────────┐    │
│  │  UserPreferences │   │    Workers (WM)       │    │
│  │    Manager       │   │  DailyRolloverWorker  │    │
│  └──────────────────┘   └──────────────────────┘    │
└─────────────────────────────────────────────────────┘
                              │ triggers
                              ▼
┌─────────────────────────────────────────────────────┐
│                PLATFORM LAYER                       │
│  ┌──────────────────┐   ┌──────────────────────┐    │
│  │  HabitOverlay    │   │  Glance Widgets       │    │
│  │  Service         │   │  AllHabitsWidget      │    │
│  └──────────────────┘   └──────────────────────┘    │
│  ┌──────────────────┐   ┌──────────────────────┐    │
│  │  BootReceiver    │   │  NotificationManager  │    │
│  └──────────────────┘   └──────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

---

## الانحراف المعماري الموثق / Documented Architecture Drift

### الانحراف المُلاحظ: الوصول المباشر للمستودع من نماذج العرض
بعض نماذج العرض تتجاوز طبقة حالات الاستخدام وتستدعي المستودع مباشرة:
```kotlin
// HomeViewModel.kt — تجاوز UseCase للعمليات البسيطة
val repository = (application as HabitApplication).repository
repository.getActiveHabitsForDate(today) // مباشرة بدون UseCase
```

هذا الانحراف مقبول للعمليات البسيطة (قراءة، تبديل الحالة) لكن يُوصى بتوحيد القناة عبر UseCases للتحسينات المستقبلية.

### Observed Drift: Direct Repository Access in ViewModels
Several ViewModels (`HomeViewModel`, `AllHabitsViewModel`, `HabitDetailViewModel`) bypass the UseCase layer for simple read/toggle operations:
```kotlin
// Pattern observed in HomeViewModel.kt
val app = application as HabitApplication
val repository = app.repository
// Called directly without an intermediate UseCase
repository.toggleHabitCompletion(habitId, date)
```
This is an acceptable simplification for CRUD operations but inconsistent with strict Clean Architecture. Recommended: gradually route all VM-to-data calls through dedicated UseCases.

---

## دورة حياة نموذج العرض / ViewModel Lifecycle

تستخدم جميع نماذج العرض `AndroidViewModel` لأنها تحتاج إلى `Application` للوصول لكائنات الاعتماديات:
```kotlin
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HabitApplication
    private val repository = app.repository
}
```
All ViewModels extend `AndroidViewModel` to gain access to the manually injected dependencies held in the global Application instance.
