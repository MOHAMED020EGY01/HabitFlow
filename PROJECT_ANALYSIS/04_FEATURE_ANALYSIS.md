# 04_FEATURE_ANALYSIS — التحليل الفصلي للميزات / Deep Feature Analysis

يحتوي هذا المستند على تحليل هندسي عميق ومفصل لكل ميزة في تطبيق **HabitFlow**، مع تتبع نقاط الدخول ومخطط المكونات والوظائف التشغيلية.

This document presents a deep architectural breakdown of all core features in **HabitFlow**, tracking entry points, composables, views, and data pathways.

---

## 1. لوحة التحكم الرئيسية / Home Dashboard

* **الغرض / Purpose**: تعرض بطاقات العادات المجدولة لليوم الحالي مع حلقات تقدم زجاجية، وتسمح للمستخدم بتتبع حالته الالتزامية وتأكيد الإنجاز بلمسة واحدة.
* **نقطة الدخول / Entry Point**: مسار `Routes.HOME` المدار عبر `AppNavigation`.
* **مخطط المكونات / Composable Hierarchy**:
  ```text
  HomeScreen
  └── Scaffold
      ├── GlassDashboardShowcase (ملخص التقدم وحلقات التقدم والنسب المئوية)
      └── LazyColumn (قائمة العادات لليوم)
          └── HabitCard (بطاقة العادة الزجاجية التفاعلية)
              └── ProgressRing (حلقة التقدم المستقلة الملونة)
  ```
* **فئات العرض والمنطق / ViewModel**: فئة `HomeViewModel.kt`.
  * يتم قراءة وعرض البيانات الحالية للعادات المجدولة اليوم عبر تدفق `app.repository.getActiveHabitsWithCompletion(todayStr)`.
* **تخزين البيانات والـ DAO / Database**: جدول `habits` وجدول سجل الإنجاز اليومي `habit_logs`.
* **سير المعالجة / Execution Flow**:
  1. يلمس المستخدم مربع الاختيار (Check-in Box) في بطاقة عادة ما.
  2. تستدعي واجهة Compose دالة `HomeViewModel.toggleCheckIn(habitId, completed)`.
  3. يتحقق نموذج العرض أولاً من صلاحية العادة لليوم الحالي (`habit.isActiveToday()`).
  4. تستدعي الدالة المستودع `app.repository.toggleLogForDate(habitId, todayStr, completed)`.
  5. يقوم المستودع باستدعاء `HabitDao.insertLog` أو `deleteLogForDate`.
  6. يتم استدعاء `HabitStatusManager.checkHabitCompletion` للتحقق من بلوغ نهاية الدورة وإغلاقها.
  7. يتم إطلاق `HabitWidgetSyncUpdater.updateNowForced(context)` لتحديث قطع الشاشة الرئيسية Glance فوراً وبشكل متجاوز.
* **الوضع الحالي والتحسين / Status & Future Opportunities**:
  * *الوضع*: يعمل بشكل ممتاز ومحسن بمعدلات recomposition محدودة لفرز الأسطر عبر استخدام `mutableStateListOf`.
  * *التحسين*: إضافة رسوم متحركة تفصيلية (Glow effects) عند إكمال المهام وتجاوز المستودع المباشر بإدراج UseCases للتطابق البنيوي.

---

## 2. منشئ ومحرر العادات / Habit Editor

* **الغرض / Purpose**: يتيح إنشاء عادة جديدة أو تعديل عادة سابقة؛ تحديد الاسم، الوصف، الأيام النشطة، مدة الدورة، اللون، وأوقات التذكيرات المتعددة.
* **نقطة الدخول / Entry Point**: مسار `Routes.ADD_HABIT` مع وسيط اختياري `habitId` (إذا كان `0` يعني ميزة إضافة، وإذا كان أكبر من صفر يعني تعديل).
* **مخطط المكونات / Composable Hierarchy**:
  ```text
  AddHabitScreen
  └── Scaffold
      └── Column (Scrollable)
          ├── DaysOfWeekSelector (اختيار أيام تفعيل العادة)
          ├── ColorPicker (منتقي الألوان السداسية الدائري)
          ├── WheelTimePicker (منتقي الأوقات الدوار المخصص)
          └── Buttons (حفظ / إلغاء)
  ```
* **فئات العرض والمنطق / ViewModel**: فئة `AddHabitViewModel.kt`.
  * تستخدم `AddHabitUseCase` و `UpdateHabitUseCase` و `ValidateReminderTimeUseCase`.
* **قوانين العمل والتحقق / UseCases**:
  * **AddHabitUseCase**: يتحقق من عدم تجاوز الحد الأقصى المسموح به للعادات النشطة (6 عادات). في حال تجاوز الحد، يتم حفظ العادة كعادة متوقفة `INACTIVE` وإخطار المستخدم بفتور.
  * **ValidateReminderTimeUseCase**: يتحقق من وجود فاصل زمني لا يقل عن 10 دقائق (`MIN_GAP_MINUTES = 10`) لمنع تداخل وتشويش التنبيهات في نفس اليوم للعادة نفسها أو مع عادات أخرى نشطة.
* **سير المعالجة / Execution Flow**:
  1. يضغط المستخدم على "حفظ العادة".
  2. يقوم `AddHabitViewModel` بالتحقق من المدخلات (الاسم غير فارغ، المدة أكبر من صفر، وجود تنبيه واحد على الأقل).
  3. يتم استدعاء `ValidateReminderTimeUseCase.validate()` لكل وقت تنبيه مدخل.
  4. في حال النجاح، يستدعي دالة الحفظ التي تطلق `AddHabitUseCase` أو `UpdateHabitUseCase`.
  5. تقوم كتل الاستخدام بكتابة البيانات في `HabitDao` والتحكم بنقل الاعتماديات.
  6. يطلق الكود في الخلفية مهام `HabitReminderWorker.scheduleHabitReminders()` لتحديث Alarms في الخلفية وجدولة عمال التنبيه، ومزامنة قطع الشاشة `updateNowForced()`.
* **الوضع الحالي والتحسين / Status & Future Opportunities**:
  * *الوضع*: مستقر ويوفر واجهة إدخال قوية.
  * *التحسين*: السماح باختيار نغمات تنبيه مخصصة لكل عادة وتسجيل نطق TTS بصوت مسجل يدوي.

---

## 3. التذكيرات الفائقة والنافذة المنبثقة / Audio Reminders & Overlay Alarms

* **الغرض / Purpose**: إطلاق شاشة تنبيه منبثقة تفاعلية زجاجية فوق التطبيقات الأخرى عند بلوغ وقت التذكير بالعادة، مصحوبة بنغمة منبه متكررة أو نطق صوتي TTS باسم العادة.
* **نقطة الدخول / Entry Point**: يتم إشعالها عبر بث داخلي لـ `HabitOverlayReceiver`.
* **مخطط المكونات / Composable Hierarchy**:
  ```text
  WindowManager (Root context overlay)
  └── ComposeView
      └── HabitOverlayContent (البطاقة المنبثقة المضيئة)
  ```
* **الخدمات الفعالة / Services**:
  * **`HabitOverlayService`**: خدمة أمامية مؤقتة تدير إضافة وعرض الواجهة فوق شاشة WindowManager وتشغيل نغمة التنبيه عبر `ReminderAudioRepository`.
* **الصلاحيات المطلوبة / Permissions**:
  * `android.permission.SYSTEM_ALERT_WINDOW` (للرسم فوق التطبيقات الأخرى).
  * `android.permission.POST_NOTIFICATIONS` (لعرض الإشعار الإلزامي للخدمة الأمامية).
* **سير المعالجة / Execution Flow**:
  1. يستيقظ `HabitReminderWorker` أو المنبه عند وقت التذكير المجدول.
  2. يتحقق `HabitOverlayWorker` من صلاحية التنبيه اليومي وسماح الرسم فوق الشاشة وصلاحيات النظام.
  3. إذا كان الهاتف مغلقاً (`isKeyguardLocked`): يتم حفظ التنبيه في `PendingOverlayStore` لتأجيله.
  4. إذا كان الهاتف مفتوحاً: يرسل بثاً لـ `HabitOverlayReceiver` الذي يستدعي `HabitOverlayService` كخدمة أمامية.
  5. تقوم الخدمة الأمامية ببناء نافذة عائمة مخصصة مع سياق لايف-سايكل مستقل `ServiceLifecycleOwner` وتثبيتها في `WindowManager`.
  6. تطلق الخدمة تشغيل نغمة المنبه أو النطق TTS عبر `ReminderAudioRepository.playReminder()`.
  7. تتيح النافذة المنبثقة التجريد والحركة (Drag options)؛ وإذا نقر المستخدم على "تم الإنجاز"، يتم حفظ السجل "DONE" وتحديث الويدجت Glance وإيقاف الصوت وإزالة النافذة.
* **الوضع الحالي والتحسين / Status & Future Opportunities**:
  * *الوضع*: مكتمل وذو موثوقية عالية جداً، ويتحكم جيداً بنقص التهيئة أو تعارض التشغيل.
  * *التحسين*: منع تداخل النطق في حال تشغيل منبهات لعدة عادات في نفس اللحظة عبر تطبيق نظام طابور صوتي متقدم.

---

## 4. قطع الشاشة التفاعلية / Glance Home Widgets

* **الغرض / Purpose**: توفير 3 قطع تفاعلية على الشاشة الرئيسية للهاتف:
  1. **AllHabitsWidget**: لعرض قائمة العادات النشطة ومستوى الإنجاز لكل منها مع إمكانية تأكيد الإنجاز السريع بنقرة من الواجهة.
  2. **InactiveHabitsWidget**: لعرض آخر العادات المتوقفة وتواريخ ومدة التوقف.
  3. **HabitStatsSummaryWidget**: لعرض ملخص إحصائي سريع لأداء والتزام المستخدم.
* **الوضع التقني / Technical Stack**: مبنية باستخدام Glance المعتمد على Compose والمدعوم بـ RemoteViews.
* **الالتفاف التشغيلي وتحديث الواجهات / Refresh Loop**:
  * لتفادي قيود الحظر التي يفرضها نظام أندرويد على تحديث ويدجت Glance (والتي قد تؤخر الرسم لمدة 45 ثانية)، يعتمد التطبيق على فئة `WidgetDirectUpdater.pushDirectUpdate` التي تقوم بالرسم غير المتزامن لـ Compose view محلياً وحفظه، ومن ثم دفعه مباشرة إلى `AppWidgetManager.updateAppWidget()`.
  * تجميع التعديلات وحماية البطارية عبر فئة `HabitWidgetSyncUpdater` التي تفرز وتجمع نقرات الإكمال المتتالية وتنتظر 3 ثوانٍ قبل التحديث البرمجي.
* **سير المعالجة / Execution Flow**:
  1. ينقر المستخدم على زر "DONE" من ويدجت الشاشة الرئيسية.
  2. يتم إشعال إجراء `MarkHabitDoneAction.onRun()`.
  3. يستدعي الكود `app.repository.logHabitCompletion()` ويكمل السجل.
  4. يتم استدعاء تحديث قطع الواجهة فوراً وعرض النتائج المنجزة محلياً وبشكل آني.

---

## 5. تقويم الإنجاز السنوي والشهري / Calendar Grid

* **الغرض / Purpose**: شاشة عرض تفاعلية تتيح للمستخدم استعراض حالة التزامه طوال الأيام السابقة وتتبع التواريخ التي أنجز فيها عاداته أو تغيب عنها.
* **نقطة الدخول / Entry Point**: مسار `Routes.CALENDAR` في التطبيق.
* **فئة العرض والمنطق / ViewModel**: فئة `CalendarViewModel.kt`.
  * يتم تحميل سجل التواريخ المكتملة عبر استعلام `app.repository.getCompletedLogDates()`.
* **تخزين البيانات والـ DAO / Database**: جدول سجل الإنجاز اليومي `habit_logs`.
* **الوضع الحالي والتحسين / Status & Future Opportunities**:
  * *الوضع*: يعمل بشكل مستقر ويعرض شبكة تقويم ملونة.
  * *التحسين*: إضافة مرشحات لتصفية التواريخ بحسب فئات العادات النشطة أو الألوان لتوضيح البيانات بدقة.

---

## 6. التفاف التقويم اليومي الليلي / Midnight Rollover Flow

* **الغرض / Purpose**: معالجة حالات العادات تلقائياً في الخلفية عند منتصف الليل: كتابة سجلات الغياب "MISS" للأيام التي لم تؤكد فيها العادات، معالجة فترات غياب العادات المتوقفة، إرسال إشعارات الإيقاف المؤقت التلقائي بعد 3 غيابات متتالية، وإغلاق الدورات التاريخية للعادة.
* **آلية الإطلاق / Trigger**: يتم تشغيلها دورياً عبر عامل العمل `DailyRolloverWorker` المجدول في WorkManager ليعمل في منتصف الليل.
* **سير معالجة الالتفاف والتوقف التلقائي**:
  ```mermaid
  sequenceDiagram
      participant Worker as DailyRolloverWorker
      participant Mgr as HabitStatusManager
      participant DB as Room Database / DAO
      participant OS as NotificationManager

      Worker->>Mgr: performDailyRollover(Context, Repository)
      Note over Mgr: Loop through all habits (ACTIVE & INACTIVE)
      Mgr->>DB: getLogsForHabitSync(habitId)
      Note over Mgr: Fill in missing MISS/INACTIVE_SKIPPED logs (last 30 days)
      Mgr->>DB: insertLogsBulk(logsToInsert)
      
      opt If Active habit has 3 consecutive MISS logs
          Note over Mgr: Change status to INACTIVE, set isActive = false
          Mgr->>DB: updateHabit(pausedHabit)
          Mgr->>OS: notify(Inactivity Notification)
          Mgr->>DB: insertNotification(PAUSE log)
      end

      opt If Active habit reaches Cycle End Date
          Note over Mgr: Compute Completion Rate
          Note over Mgr: Determine status (COMPLETE if >=90%, else FAILURE)
          Mgr->>DB: updateHabit(completedHabit)
          Mgr->>DB: insertCycleHistory(cycleSummary)
      end
  ```

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم التحقق من سلوك الميزات البرمجية ودورة حياتها ومصفوفات اتخاذ القرار برمجياً من كود المعالجة ومستندات الاستعلام.
* **Files Used / الملفات المستخدمة**:
  - [HabitStatusManager.kt](app/src/main/java/com/example/domain/usecase/HabitStatusManager.kt#L27-L191)
  - [AddHabitViewModel.kt](app/src/main/java/com/example/presentation/screens/add/AddHabitViewModel.kt#L198-L283)
  - [HabitOverlayService.kt](app/src/main/java/com/example/overlay/HabitOverlayService.kt#L106-L248)
  - [WidgetDirectUpdater.kt](app/src/main/java/com/example/widget/WidgetDirectUpdater.kt#L19-L59)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
