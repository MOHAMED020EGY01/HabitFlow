# 01_STRUCTURE

## هيكل المجلدات / Directory Tree

### البنية الحالية للمشروع / Current Project Structure
```text
habitflow/
├── app/
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml (ملف مانيفست التطبيق لتسجيل المكونات والصلاحيات / App manifest for registering components and permissions)
│           ├── java/com/example/
│           │   ├── HabitApplication.kt (مرفأ تشغيل التطبيق وحاوية حقن الاعتماديات اليدوية / Application class and manual DI container)
│           │   ├── MainActivity.kt (النشاط الرئيسي ونقطة انطلاق واجهة المستخدم / Entry Activity and UI launch point)
│           │   ├── data/ (طبقة البيانات - قواعد البيانات والمستودعات والعمال الخلفيين / Data layer - databases, repositories, workers)
│           │   │   ├── local/
│           │   │   │   ├── dao/ (واجهات الاستعلام عن البيانات لقواعد البيانات Room / Room Database DAO interfaces)
│           │   │   │   ├── database/ (قاعدة البيانات وإعدادات الهجرة والتحويل / Database definitions, migrations, converters)
│           │   │   │   └── entity/ (نماذج الجداول المخزنة في قاعدة البيانات / Local Room entities mapping to SQL tables)
│           │   │   ├── preferences/ (إدارة الإعدادات والخيارات عبر DataStore / Managing user preferences via DataStore)
│           │   │   ├── receiver/ (مستقبلات البث لنظام أندرويد مثل الإقلاع / Android Broadcast Receivers like boot-completed)
│           │   │   ├── repository/ (التنفيذ الفعلي لمستودعات البيانات للوصول لقاعدة البيانات / Concrete repository implementations)
│           │   │   └── worker/ (مهام عمال الخلفية لإدارة التذكيرات والالتفاف اليومي / WorkManager background workers)
│           │   ├── domain/ (طبقة المنطق والعمل - النماذج وحالات الاستخدام / Domain layer - entities and clean use-cases)
│           │   │   ├── model/ (النماذج العامة لبيانات التطبيق المستقلة / Pure domain models representing core logic)
│           │   │   ├── repository/ (واجهات تعريف مستودعات البيانات / Repository interface abstractions)
│           │   │   ├── usecase/ (منطق العمل الخاص بكل ميزة وتطبيق القواعد / Clean architecture use cases)
│           │   │   └── util/ (أدوات مساعدة لحساب الفترات والتذكيرات / Domain helper utilities like streak calculators)
│           │   ├── overlay/ (إدارة النوافذ العائمة والتذكيرات فوق التطبيقات الأخرى / Floating reminder overlays logic)
│           │   │   ├── composable/ (مكونات واجهة المستخدم للنافذة العائمة / Compose UI for overlays)
│           │   │   └── [Receivers/Services] (خدمات ومستقبلات لتشغيل التذكير العائم / Services/Receivers for managing overlay cycles)
│           │   ├── presentation/ (طبقة العرض والواجهات ونماذج العرض / Presentation layer - UI screens and ViewModels)
│           │   │   ├── components/ (عناصر الواجهة المخصصة مثل البطاقات والألوان / Custom premium UI components)
│           │   │   ├── navigation/ (إدارة التنقل والتحريكات بين الشاشات / Compose Navigation graph and animations)
│           │   │   └── screens/ (الشاشات الفردية مع نماذج العرض الخاصة بها / Individual feature screens and view models)
│           │   ├── service/ (الخدمات الخلفية المستمرة لضمان موثوقية التذكيرات / Foreground keep-alive services)
│           │   ├── ui/theme/ (إعدادات المظهر الموحد والألوان للخطوط / Theme colors, typography, shapes)
│           │   └── util/ (أدوات مساعدة عامة مثل التنسيق واللغات / General helpers for formatting, RTL, and language)
│           │   └── widget/ (قطع الواجهة التفاعلية للشاشة الرئيسية Glance / Android Glance home screen widgets)
│           └── res/ (ملفات الموارد المترجمة والصور والتخطيطات / App resources including layouts and Arabic translations)
```

---

## النمط المعماري الفعلي / Architecture Pattern

### النمط المطبق والانحرافات الفنية
يعتمد المشروع أساسًا على نمط **الهندسة النظيفة (Clean Architecture)** مع **MVVM** وتجزئة المسؤوليات إلى ثلاث طبقات رئيسية: `presentation` و `domain` و `data`. 
ولكن، لوحظ وجود **انحراف معماري (Architecture Drift)** في بعض الأجزاء البرمجية:
1. **الوصول المباشر للمستودعات**: تقوم بعض نماذج العرض (مثل `HomeViewModel` و `AllHabitsViewModel` و `HabitDetailViewModel`) باستدعاء المستودع `HabitRepository` بشكل مباشر للقيام بعمليات القراءة والكتابة بدلاً من المرور عبر طبقة حالات الاستخدام `usecase` (مثال: جلب العادات النشطة وتحديث حالة الإنجاز).
2. **استخدام محدود لحالات الاستخدام**: تنحصر طبقة حالات الاستخدام في العمليات المعقدة مثل إضافة عادة جديدة (`AddHabitUseCase`) وتأكيد عدم تجاوز الحد الأقصى للعادات، أو تفعيل العادات وتجنب تعارض مواعيد التذكيرات.
3. **غياب إطار حقن الاعتماديات**: تم إزالة مكتبة Hilt/Dagger كليًا لصالح حاوية حقن يدوية بسيطة ومحكمة تعتمد على خصائص lateinit معرّفة داخل فئة `HabitApplication` يتم بناؤها بشكل غير متزامن عند الإقلاع.

### Implemented Architecture & Drift
The codebase implements **Clean Architecture** paired with **MVVM**. The project is split into `presentation`, `domain`, and `data` packages.
However, a distinct **Architecture Drift** is visible in active usage:
1. **Direct Repository Access**: ViewModels (specifically `HomeViewModel`, `AllHabitsViewModel`, and `HabitDetailViewModel`) bypass the UseCase layer for simple queries and toggle operations, calling functions on `app.repository` directly.
2. **Targeted UseCases**: The UseCase layer is selectively utilized only for complex business rules (e.g., limit-checking in `AddHabitUseCase`, reminder gap validation in `ValidateReminderTimeUseCase`, and toggle logic in `ToggleHabitActiveUseCase`).
3. **Manual Dependency Injection**: Instead of Hilt/Dagger, dependency injection is managed manually. The dependencies are initialized asynchronously on startup and held as `lateinit` properties inside the global `HabitApplication` instance.

---

## نقاط الدخول الرئيسية / Entry Points

### المكونات الأساسية لبدء تشغيل التطبيق
يتم تسجيل وإطلاق التطبيق والخدمات الخلفية عبر المكونات التالية في نظام أندرويد:
- **`HabitApplication`**: فئة التطبيق التي تبدأ بتسجيل استثناءات الانهيار الشامل، وتطبيق إعدادات اللغة، وتهيئة مستودعات البيانات وقواعد البيانات غير المتزامنة على خيط خلفي.
- **`MainActivity`**: نقطة الانطلاق الرئيسية للواجهة الرسومية، والتي تقوم بتهيئة نافذة الشاشة الترحيبية وتثبيت شاشات العرض كاملة الحواف (Edge-to-Edge) وإدارة التنقل (`AppNavigation`).
- **`Routes` & `AppNavigation`**: يمثلان الهيكل البرمجي للتنقل بين 10 مسارات رئيسية بما في ذلك شاشة إحصائيات التقويم وتفضيلات المستخدم والإشعارات والتذكيرات.
- **مستقبلات البث (Broadcast Receivers)**:
  - `BootReceiver`: يعيد جدولة التذكيرات العادية ويطلق العمال الخلفيين عند إقلاع الجهاز.
  - `PendingOverlayReceiver`: يطلق النوافذ العائلة المؤجلة بمجرد فتح قفل الهاتف (`USER_PRESENT`).
  - `HabitOverlayReceiver`: يستقبل بث عمال التذكير العائم لتحضير طابور العرض.
- **الخدمات الخلفية (Foreground Services)**:
  - `HabitBackgroundService`: خدمة مستمرة تحافظ على التنبيهات في الخلفية وتقوم بجدولة التذكيرات الفائتة فور فك قفل الجهاز.
  - `HabitOverlayService`: خدمة عرض النافذة العائمة فوق التطبيقات الأخرى لعرض تذكير العادة وسماع نغمة التنبيه.
- **العمال الخلفيون (WorkManager Workers)**:
  - `DailyRolloverWorker`: يعمل في منتصف الليل لتحديث التقويم التراكمي وتدوين غيابات الأمس والتأكد من عدم إهمال العادات.
  - `DbVacuumWorker`: يعمل أسبوعيًا لتحسين مساحة قاعدة البيانات SQLite.
  - `HabitReminderWorker` & `HabitOverlayWorker`: يقومان بجدولة وإطلاق التنبيهات الصوتية والمرئية يوميًا.

### Key Application Entry Points
The application integrates with the Android OS via these core entry components:
- **`HabitApplication`**: Initializes manual dependency injection, configures global crash handling, applies saved language configurations synchronously, and kicks off background tasks.
- **`MainActivity`**: The single-activity GUI launcher. Installs the splash screen, handles edge-to-edge window insets, controls immersive nav bar styling, and runs the Compose navigation host (`AppNavigation`).
- **Navigation Graph (`Routes`)**: Coordinates routing for 10 screens (Splash, Onboarding, Home, Add Habit, Detail, All Habits, Summary, Settings, Calendar, and Notifications).
- **Broadcast Receivers**:
  - `BootReceiver`: Re-registers scheduled alarms and fires up services when the device boots.
  - `PendingOverlayReceiver`: Listens for `ACTION_USER_PRESENT` to fire cached reminder overlays when the user unlocks their device.
  - `HabitOverlayReceiver`: Receives alarms to queue and present new overlay windows.
- **Services (Foreground)**:
  - `HabitBackgroundService`: A persistent reliability foreground service checking for missed habits during device unlocks.
  - `HabitOverlayService`: Displays the custom interactive Compose-based overlay window above other apps and plays alarms.
- **WorkManager Workers**:
  - `DailyRolloverWorker`: Triggers nightly at 12:00 AM to process habit status rollovers, record missed logs, and auto-pause inactive flows.
  - `DbVacuumWorker`: Weekly task executing database auto-vacuuming/compaction.
  - `HabitReminderWorker` & `HabitOverlayWorker`: Coordinate alarm notifications and overlay triggers daily based on custom schedules.

---

## مخطط اعتماديات الطبقات / Layer Dependency Diagram

### الرسم التخطيطي لاعتماديات التطبيق الفعلي
الاعتماديات تسير في اتجاه واحد من الخارج إلى الداخل مع وجود التفاف مباشر من نموذج العرض إلى المستودع:

```text
[ Presentation Layer (ViewModels / Screens / Components) ]
          │                           │ (انحراف للوصول للمستودع مباشرة / Direct Repository Access Drift)
          ▼                           ▼
[ Domain Layer (UseCases) ] ───► [ Domain Layer (Interfaces / Repository Contract) ]
                                              ▲
                                              │ (يحقق الواجهة / Implements Contract)
                                              │
                               [ Data Layer (Room / Preferences / RepositoryImpl) ]
```

- **طبقة العرض (Presentation)**: تعتمد على واجهات طبقة النطاق (Domain) وحالات الاستخدام، ولكنها تتصل مباشرة بالمستودع `HabitRepository` للعمليات البسيطة.
- **طبقة النطاق (Domain)**: تمثل قلب التطبيق المستقل تمامًا عن تفاصيل أندرويد وقواعد البيانات، وتعرّف نماذج العادات وعقود الوصول للبيانات.
- **طبقة البيانات (Data)**: تعتمد على طبقة النطاق لتنفيذ العقود البرمجية وتتعامل مع تفاصيل قواعد البيانات المحلية والمفضلة ونظام أندرويد.

### Dependency Flow and Layering
Dependencies flow in a clean unidirectional sequence, except for the ViewModel direct repository access shortcut:
- **Presentation**: Depends on Domain models and UseCases, but possesses direct imports of `HabitRepository` to execute core CRUD operations.
- **Domain**: Self-contained business logic. Has zero external dependencies on frameworks, databases, or UI. Declares interfaces that the data layer must fulfill.
- **Data**: Implements Domain repository contracts, interacting with Android system APIs, Room Database, and Preferences DataStore.
