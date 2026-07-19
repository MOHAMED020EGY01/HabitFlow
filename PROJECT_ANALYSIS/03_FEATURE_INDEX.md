# 03_FEATURE_INDEX — فهرس ميزات النظام / System Feature Index

## قائمة الميزات الرئيسية / Main Feature Index

يحتوي تطبيق **HabitFlow** على 10 ميزات وظيفية رئيسية مبنية محلياً بالكامل. يوضح الجدول التالي الميزة وغرضها وحالتها التشغيلية وأهم الملفات المرتبطة بها:

The **HabitFlow** application provides 10 core features functioning offline-first. Here is a directory index mapping features to status and associated files:

| الرقم / ID | الميزة / Feature | الغرض / Purpose | الملفات الرئيسية / Key Files | الحالة / Status |
| :---: | :--- | :--- | :--- | :---: |
| 01 | لوحة التحكم الرئيسية / Home Dashboard | عرض التقدم اليومي وإكمال العادات التفاعلية السريعة.<br>Render daily tasks, progress rings, and quick check-ins. | [HomeScreen.kt](app/src/main/java/com/example/presentation/screens/home/HomeScreen.kt)<br>[HomeViewModel.kt](app/src/main/java/com/example/presentation/screens/home/HomeViewModel.kt) | **VERIFIED** |
| 02 | محرر العادات / Habit Editor | إضافة وتعديل العادات، الألوان، الأيام النشطة، وتحديد التنبيهات.<br>Add or edit habits, color hex, active days, and alarms. | [AddHabitScreen.kt](app/src/main/java/com/example/presentation/screens/add/AddHabitScreen.kt)<br>[AddHabitViewModel.kt](app/src/main/java/com/example/presentation/screens/add/AddHabitViewModel.kt) | **VERIFIED** |
| 03 | إدارة التذكيرات / Alarms & Overlays | إطلاق شاشة التذكير العائمة بالتزامن مع منبه ونطق TTS.<br>Trigger floating window overlay and play alarms/TTS speech. | [HabitOverlayService.kt](app/src/main/java/com/example/overlay/HabitOverlayService.kt)<br>[ReminderAudioRepositoryImpl.kt](app/src/main/java/com/example/data/repository/ReminderAudioRepositoryImpl.kt) | **VERIFIED** |
| 04 | قطع الواجهة / Glance Home Widgets | مزامنة 3 قطع تفاعلية على الشاشة لجميع العادات والمتوقفة والملخص.<br>Synchronize 3 widgets showing habits, stopped stats, and progress. | [AllHabitsWidget.kt](app/src/main/java/com/example/widget/AllHabitsWidget.kt)<br>[InactiveHabitsWidget.kt](app/src/main/java/com/example/widget/InactiveHabitsWidget.kt) | **VERIFIED** |
| 05 | تقويم الإنجاز / Calendar Grid | عرض التفاعل والأيام المكتملة شهرياً بألوان مخصصة.<br>Visualize monthly grid with color-coded daily completions. | [CalendarScreen.kt](app/src/main/java/com/example/presentation/screens/calendar/CalendarScreen.kt)<br>[CalendarViewModel.kt](app/src/main/java/com/example/presentation/screens/calendar/CalendarViewModel.kt) | **VERIFIED** |
| 06 | تفاصيل العادة والدورات / Habit Details & Cycles | مراجعة نسب الالتزام، الانتظام، وتتبع أرشيف الدورات السابقة.<br>View progress rates, streak logs, and cycle history. | [HabitDetailScreen.kt](app/src/main/java/com/example/presentation/screens/detail/HabitDetailScreen.kt)<br>[HabitDetailViewModel.kt](app/src/main/java/com/example/presentation/screens/detail/HabitDetailViewModel.kt) | **VERIFIED** |
| 07 | التوجيه والترحيب / Splash & Onboarding | معالجة الإعداد الأولي وعرض الميزات وشاشات الانتقال لمرة واحدة.<br>Process first-launch onboarding walkthrough and splash screen checks. | [OnboardingScreen.kt](app/src/main/java/com/example/presentation/screens/onboarding/OnboardingScreen.kt)<br>[SplashScreen.kt](app/src/main/java/com/example/presentation/screens/splash/SplashScreen.kt) | **VERIFIED** |
| 08 | إدارة التفضيلات / Settings Panel | تحرير صورة المستخدم ومظهره، واللغة، ومسح كافة البيانات محلياً.<br>Modify profile avatar, user name, UI themes, and wipe database. | [SettingsScreen.kt](app/src/main/java/com/example/presentation/screens/settings/SettingsScreen.kt)<br>[SettingsViewModel.kt](app/src/main/java/com/example/presentation/screens/settings/SettingsViewModel.kt) | **VERIFIED** |
| 09 | ملخص وإحصائيات التقدم / Summary Dashboard | إحصائيات مجمعة وتوزيع ألوان العادات ورسم بياني لمستويات النجاح.<br>Summarized analytics, habit coloring distributions, and success charts. | [SummaryScreen.kt](app/src/main/java/com/example/presentation/screens/summary/SummaryScreen.kt)<br>[SummaryViewModel.kt](app/src/main/java/com/example/presentation/screens/summary/SummaryViewModel.kt) | **VERIFIED** |
| 10 | سجل التنبيهات / Notifications Log | حصر الإشعارات والتنبيهات المرسلة للمستخدم وإتاحة تفريغ السجل.<br>Review pushed alert history logs and clean individual or all logs. | [NotificationsScreen.kt](app/src/main/java/com/example/presentation/screens/notifications/NotificationsScreen.kt)<br>[NotificationsViewModel.kt](app/src/main/java/com/example/presentation/screens/notifications/NotificationsViewModel.kt) | **VERIFIED** |

---

## روابط وتكامل الميزات / Cross-Feature Integration

1. **إعداد العادة ← التذكير الصوتي**: يؤدي حفظ العادة بنجاح في `AddHabitViewModel` إلى استدعاء `HabitReminderWorker` لجدولة المهام الخلفية للتنبيهات.
2. **الإنذار العائم ← تحديث قاعدة البيانات وقطعة الشاشة**: تؤدي نقرة إكمال العادة من النافذة العائمة `HabitOverlayService` إلى كتابة سجل إنجاز "DONE" في `HabitRepository` ثم تحديث فوري للشاشة والويدجت `HabitWidgetSyncUpdater`.
3. **الالتفاف الليلي ← إيقاف تلقائي للعادات المتروكة**: يتحقق `DailyRolloverWorker` عند منتصف الليل من غياب 3 أيام متتالية، وفي حال تحقق الشرط، ينقل حالة العادة لـ `INACTIVE` ويلغي جميع الإنذارات التابعة لها ويرسل إشعار إيقاف للمستخدم.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم التحقق من كافة مسارات الشاشات المسجلة في `AppNavigation` وربطها بالملفات البرمجية المقابلة المذكورة في الجدول.
* **Files Used / الملفات المستخدمة**:
  - [MainActivity.kt](app/src/main/java/com/example/MainActivity.kt#L250-L342)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
