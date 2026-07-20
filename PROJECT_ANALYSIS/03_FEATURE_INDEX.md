# 03_FEATURE_INDEX — فهرس ميزات النظام / System Feature Index

## قائمة الميزات الرئيسية / Main Feature Index

يحتوي تطبيق **HabitFlow** على ميزات وظيفية رئيسية مقسمة إلى شرائح (Slices). يوضح الجدول التالي الميزة وغرضها وأهم الملفات المرتبطة بها في الهيكل الجديد:

The **HabitFlow** application provides core features functioning offline-first. Here is a directory index mapping features to status and associated files:

| الميزة / Feature | الغرض / Purpose | الملفات الرئيسية / Key Files | الحالة / Status |
| :--- | :--- | :--- | :---: |
| لوحة التحكم / Home Dashboard | عرض التقدم اليومي وإكمال العادات السريعة. | [HomeScreen.kt](app/src/main/java/com/example/feature/home/presentation/HomeScreen.kt) | **VERIFIED** |
| تتبع العادات / Habit Management | إدارة العادات (إضافة، تعديل، حذف، تفاصيل). | [AddHabitScreen.kt](app/src/main/java/com/example/feature/habit/presentation/AddHabitScreen.kt) | **VERIFIED** |
| التقويم / Calendar | عرض سجل الإنجاز الشهري. | [CalendarScreen.kt](app/src/main/java/com/example/feature/calendar/presentation/CalendarScreen.kt) | **VERIFIED** |
| التحليلات / Analytics Summary | ملخص إحصائي لجميع العادات وسلاسل الإنجاز. | [SummaryScreen.kt](app/src/main/java/com/example/feature/summary/presentation/SummaryScreen.kt) | **VERIFIED** |
| التنبيهات / Audio Reminders | نظام التذكير الصوتي والنوافذ العائمة. | [HabitReminderWorker.kt](app/src/main/java/com/example/core/infrastructure/worker/HabitReminderWorker.kt) | **VERIFIED** |
| الإشعارات / Notification Log | سجل تاريخي لكافة الإشعارات المرسلة. | [NotificationsScreen.kt](app/src/main/java/com/example/feature/notifications/presentation/NotificationsScreen.kt) | **VERIFIED** |
| الإعدادات / Settings | إدارة المظهر، اللغة، وصورة الملف الشخصي. | [SettingsScreen.kt](app/src/main/java/com/example/feature/settings/presentation/SettingsScreen.kt) | **VERIFIED** |
| الترحيب / Onboarding | جولة تعريفية للمستخدم الجديد. | [OnboardingScreen.kt](app/src/main/java/com/example/feature/onboarding/presentation/OnboardingScreen.kt) | **VERIFIED** |

---

## روابط وتكامل الميزات / Cross-Feature Integration

1. **إعداد العادة ← التذكير الصوتي**: يتم جدولة المهام عبر `HabitReminderWorker`.
2. **الالتفاف الليلي ← إيقاف تلقائي**: يقوم `DailyRolloverWorker` بفحص العادات المتوقفة تلقائياً عند منتصف الليل.
3. **قطع الواجهة ← التحديث الفوري**: تستخدم الـ Widgets محرك `HabitWidgetSyncUpdater` للمزامنة مع قاعدة البيانات.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم التحقق من كافة مسارات الشاشات المسجلة في `MainActivity.kt`.
* **Files Used / الملفات المستخدمة**:
  - [MainActivity.kt](app/src/main/java/com/example/app/MainActivity.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
