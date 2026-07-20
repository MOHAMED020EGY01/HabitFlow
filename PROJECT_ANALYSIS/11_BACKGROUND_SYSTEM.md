# 11_BACKGROUND_SYSTEM — نظام العمليات الخلفية / Background Operations System

## نظرة عامة / Overview

يعتمد تطبيق **HabitFlow** على نظام معقد ومتكامل من العمليات الخلفية لضمان عمل التذكيرات، تحديث البيانات، ومزامنة القطع التفاعلية دون تدخل المستخدم. يتم توزيع المهام بين `WorkManager` للمهام المجدولة و `Foreground Services` للمهام التي تتطلب موثوقية عالية.

**HabitFlow** utilizes a multi-layered background system. It combines `WorkManager` for scheduled/periodic tasks with `Foreground Services` for high-reliability features like real-time reminders and device-unlock catch-up logic.

---

## 1. عمال جدولة المهام / WorkManager Workers

| العامل / Worker | الوظيفة / Purpose | التكرار / Schedule |
| :--- | :--- | :--- |
| `HabitReminderWorker` | إطلاق التذكيرات الصوتية والإشعارات. | يومي (Periodic 24h) |
| `HabitOverlayWorker` | إطلاق النوافذ العائمة التفاعلية. | يومي (Periodic 24h) |
| `DailyRolloverWorker` | معالجة الانتقال لليوم الجديد وتسجيل الغيابات. | يومي عند منتصف الليل |
| `HabitWidgetUpdateWorker`| تحديث بيانات القطع التفاعلية (Glance). | كل 15 دقيقة / يومي |
| `DbVacuumWorker` | تنظيف قاعدة البيانات وضغط حجمها. | أسبوعي |

---

## 2. الخدمات الأمامية / Foreground Services

### `HabitBackgroundService`
* **الهدف**: الخدمة "العمود الفقري" للموثوقية.
* **المهام**:
    * البقاء نشطة لمنع النظام من قتل عمليات التطبيق.
    * الاستماع لحدث `ACTION_USER_PRESENT` (فتح القفل) لإطلاق التذكيرات الفائتة.
    * إدارة "قائمة الانتظار" للتنبيهات لضمان عدم ضياع أي منها.

### `HabitOverlayService`
* **الهدف**: عرض واجهة المستخدم العائمة (Overlay UI).
* **المهام**:
    * عرض نافذة شفافة باستخدام `WindowManager`.
    * تشغيل محرك الصوت (TTS/Alarm).
    * توفير واجهة سريعة لتسجيل الإنجاز دون فتح التطبيق.

---

## 3. مستقبلي البث / Broadcast Receivers

* **`BootReceiver`**: يستمع لإعادة تشغيل الجهاز (`ACTION_BOOT_COMPLETED`) لإعادة جدولة كافة المهام في `WorkManager` وتشغيل الخدمة الخلفية.
* **`HabitOverlayReceiver`**: يستقبل إشارات من العمال لبدء تشغيل `HabitOverlayService`.
* **`PendingOverlayReceiver`**: يتعامل مع التنبيهات المؤجلة التي تنتظر فتح قفل الجهاز.

---

## استراتيجية الموثوقية / Reliability Strategy

1. **الاستمرارية (Persistence)**: استخدام `START_STICKY` في الخدمات لضمان إعادة تشغيلها من قبل النظام.
2. **تجاوز القيود (Exemption)**: توجيه المستخدم لتعطيل "تحسين البطارية" (Battery Optimization) عبر `BackgroundReliabilityHelper`.
3. **تزامن البيانات**: إطلاق `updateNowForced` بعد كل عملية خلفية لتحديث الـ Widgets فوراً.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - فحص تعريفات المكونات في `AndroidManifest.xml`.
  - مراجعة كود العمال (Workers) والخدمات (Services).
* **Files Used / الملفات المستخدمة**:
  - [AndroidManifest.xml](app/src/main/AndroidManifest.xml)
  - [HabitBackgroundService.kt](app/src/main/java/com/example/core/infrastructure/service/HabitBackgroundService.kt)
  - [DailyRolloverWorker.kt](app/src/main/java/com/example/core/infrastructure/worker/DailyRolloverWorker.kt)
  - [HabitReminderWorker.kt](app/src/main/java/com/example/core/infrastructure/worker/HabitReminderWorker.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
