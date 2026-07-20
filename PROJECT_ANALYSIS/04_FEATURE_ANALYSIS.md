# 04_FEATURE_ANALYSIS — التحليل الفصلي للميزات / Deep Feature Analysis

يحتوي هذا المستند على تحليل هندسي عميق ومفصل لكل ميزة في تطبيق **HabitFlow**، مع تتبع نقاط الدخول ومخطط المكونات والوظائف التشغيلية في الهيكل المعتمد على الميزات (Feature-Based).

This document presents a deep architectural breakdown of all core features in **HabitFlow**, tracking entry points, composables, views, and data pathways within the Feature-Based structure.

---

## 1. لوحة التحكم الرئيسية / Home Dashboard

* **الغرض / Purpose**: تعرض بطاقات العادات المجدولة لليوم الحالي وتسمح للمستخدم بتتبع حالته الالتزامية.
* **نقطة الدخول / Entry Point**: مسار `Routes.HOME` المدار عبر `MainActivity`.
* **موقع الملفات / Location**: `com.example.feature.home.presentation`.
* **مخطط المكونات / Composable Hierarchy**:
  ```text
  HomeScreen
  └── Scaffold
      ├── GlassDashboardShowcase (ملخص التقدم)
      └── LazyColumn (قائمة العادات لليوم)
          └── HabitCard (بطاقة العادة الزجاجية)
              └── ProgressRing (حلقة التقدم)
  ```
* **سير المعالجة / Execution Flow**:
  1. يلمس المستخدم مربع الإنجاز في بطاقة العادة.
  2. تستدعي واجهة Compose دالة `HomeViewModel.toggleCheckIn`.
  3. يتم استدعاء المستودع `app.repository.toggleLogForDate`.
  4. يتم إطلاق `HabitWidgetSyncUpdater` لتحديث قطع الشاشة الرئيسية Glance فوراً.

---

## 2. إدارة العادات / Habit Management (Add, Edit, Detail)

* **الغرض / Purpose**: إنشاء وتعديل واستعراض تفاصيل العادات الفردية، بما في ذلك سجل الانتظام (Streaks).
* **موقع الملفات / Location**: `com.example.feature.habit`.
* **المكونات الفرعية / Sub-components**:
  - **Add/Edit**: تتيح إدخال الاسم، اللون، الأيام النشطة، وأوقات التذكير.
  - **Detail**: تعرض الرسوم البيانية للانتظام (Heatmap) وتاريخ الدورات السابقة.
* **التحقق (Use Cases)**:
  - `AddHabitUseCase`: يضمن عدم تجاوز 6 عادات نشطة.
  - `ValidateReminderTimeUseCase`: يضمن وجود فاصل 10 دقائق بين التذكيرات.
  - `GetHabitDetailsUseCase`: يجمع بيانات العادة مع السجلات وسلسلة الإنجاز (Streak).

---

## 3. نظام التذكيرات الفائق / Super Reminder System

* **الغرض / Purpose**: ضمان وصول التذكير للمستخدم عبر الصوت (Alarm/TTS) والنافذة العائمة (Overlay).
* **المكونات / Components**:
  - **Worker**: `HabitReminderWorker` يستيقظ في الوقت المجدول.
  - **Service**: `HabitOverlayService` يعرض النافذة فوق التطبيقات الأخرى.
  - **Audio**: `ReminderAudioRepository` يدير تشغيل الأجراس أو نطق النصوص.
* **المسار البرمجي**:
  1. `HabitReminderWorker` (Infrastructure) -> `ReminderAudioRepository` (Core) -> `HabitOverlayService` (Infrastructure).

---

## 4. قطع الشاشة التفاعلية / Glance Home Widgets

* **الغرض / Purpose**: توفير 3 قطع تفاعلية: All Habits, Inactive, Summary.
* **الموقع / Location**: `com.example.core.infrastructure.widget`.
* **آلية التحديث**: يستخدم `WidgetDirectUpdater` للرسم المباشر وتجاوز تأخير Glance المعتاد، مما يوفر استجابة فورية لنقرات المستخدم من الشاشة الرئيسية.

---

## 5. التفاف التقويم اليومي / Midnight Rollover Flow

* **الغرض / Purpose**: معالجة الحالات تلقائياً عند منتصف الليل (كتابة MISS، إيقاف تلقائي، إغلاق دورات).
* **الموقع / Location**: `com.example.core.infrastructure.worker.DailyRolloverWorker`.
* **المنطق المركزي**: `com.example.core.domain.usecase.HabitStatusManager`.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم فحص الأكواد فعلياً بعد عملية إعادة الهيكلة والتأكد من روابط الاستدعاء.
* **Files Used / الملفات المستخدمة**:
  - [HabitStatusManager.kt](app/src/main/java/com/example/core/domain/usecase/HabitStatusManager.kt)
  - [HabitReminderWorker.kt](app/src/main/java/com/example/core/infrastructure/worker/HabitReminderWorker.kt)
  - [MainActivity.kt](app/src/main/java/com/example/app/MainActivity.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
