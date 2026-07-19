# 11_BACKGROUND_SYSTEM — مهام الخلفية وأنظمة المزامنة / Background Architecture

## المكونات الأساسية لأنظمة الخلفية / Core Background Components

يعتمد تطبيق **HabitFlow** على 4 مكونات رئيسية لنظام تشغيل أندرويد لضمان استقرار وجدولة المهام في الخلفية:

The application coordinates background logic using 4 core Android OS frameworks:

```mermaid
graph TD
    subgraph Background Scheduler (WorkManager)
        A[DailyRolloverWorker]
        B[DbVacuumWorker]
        C[HabitReminderWorker]
        D[HabitOverlayWorker]
    end

    subgraph OS Broadcast System
        E[BootReceiver]
        F[PendingOverlayReceiver]
        G[HabitOverlayReceiver]
    end

    subgraph Foreground Services (Keepalive)
        H[HabitBackgroundService]
        I[HabitOverlayService]
    end

    %% Interaction Paths
    E -->|Re-schedule| C
    E -->|Re-schedule| D
    F -->|Unlock trigger| I
    G -->|Alert trigger| I
    C -->|Trigger overlay| I
```

---

## 1. مجدولة الأعمال والمهام الخلفية / WorkManager Workers

يدير نظام **WorkManager** المهام الدورية التي تتطلب موثوقية عالية وتضمن التنفيذ تحت شروط معينة (مثل عدم انخفاض البطارية):

* **DailyRolloverWorker (يومي عند منتصف الليل)**:
  * *الغرض*: تشغيل التفاف السجلات اليومي، تعبئة الغيابات MISS، تفعيل التوقيف التلقائي للعادات بعد 3 أيام غياب، وأرشفة الدورات المكتملة.
  * *جدولة*: periodicWork بفاصل 24 ساعة، مع تأخير بدئي (initialDelay) محسوب ديناميكياً ليتزامن مع منتصف الليل بدقة. شروطه: `setRequiresBatteryNotLow(true)`.
* **DbVacuumWorker (أسبوعي)**:
  * *الغرض*: تحرير وتنظيف مساحات قاعدة البيانات SQLite غير المستخدمة وتحسين سرعة الأرشفة عبر تنفيذ استعلام `PRAGMA incremental_vacuum`.
  * *جدولة*: periodicWork بفاصل 7 أيام.
* **HabitReminderWorker & HabitOverlayWorker (يومي مجدول)**:
  * *الغرض*: إطلاق المنبهات والتذكيرات الصوتية وعرض النوافذ العائمة عند موعد تذكير كل عادة نشطة.
  * *جدولة*: يتم جدولتهما بشكل ديناميكي لكل عادة نشطة عند بدء تشغيل التطبيق أو حفظ التحديثات. يتم حسابهما بناءً على مواعيد التنبيهات والأيام النشطة المحددة للعادة.

---

## 2. الخدمات الخلفية النشطة / Foreground Services

يحتاج أندرويد لـ **Foreground Services** لإبقاء المهام مستمرة ومنع قفل العمليات من قبل النظام:

* **HabitBackgroundService (Reliability Service)**:
  * *الغرض*: خدمة أمامية مستمرة (`START_STICKY`) تضمن عمل منبهات العادات في الخلفية وتراقب فتح قفل الهاتف عن طريق التسجيل الديناميكي لمستقبل البث `ACTION_USER_PRESENT` للتحقق من وجود تنبيهات فائتة (Catch-up overlays) فور استيقاظ الهاتف.
  * *تصنيف الخدمة (FGS Type)*: معرفة كـ `specialUse` بقيمة subtype `habit_background_keepalive` في ملف المانيفست.
* **HabitOverlayService (Overlay Service)**:
  * *الغرض*: خدمة أمامية مؤقتة يتم استدعاؤها لحقن نافذة التذكير العائمة في `WindowManager` وتشغيل الصوت.
  * *تصنيف الخدمة (FGS Type)*: معرفة كـ `specialUse` بقيمة subtype `habit_reminder_overlay`.

---

## 3. مستقبلات البث العام / Broadcast Receivers

* **BootReceiver**: يستمع لحدث إقلاع الهاتف `android.intent.action.BOOT_COMPLETED` ليعيد جدولة أوقات التذكيرات ومهام الخلفية لجميع العادات النشطة المسجلة في قاعدة البيانات المحلية.
* **PendingOverlayReceiver**: يستمع لحدث فتح قفل الشاشة `ACTION_USER_PRESENT` لإطلاق أي منبهات عائمة تم تأجيلها وتخزينها في `PendingOverlayStore` أثناء قفل الهاتف.
* **HabitOverlayReceiver**: يستقبل المنبهات الموجهة من عمال التذكير ليقوم بدفعها إلى طابور المعالجة `OverlayQueueManager` تمهيداً لعرضها.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم التحقق من مانيفست التطبيق وملفات تعريف العمال (`CoroutineWorker`) وإعدادات الجدولة وفصل الخدمات الأمامية بقيمها المجمعة.
* **Files Used / الملفات المستخدمة**:
  - [AndroidManifest.xml](app/src/main/AndroidManifest.xml#L44-L123)
  - [DailyRolloverWorker.kt](app/src/main/java/com/example/data/worker/DailyRolloverWorker.kt#L41-L69)
  - [HabitBackgroundService.kt](app/src/main/java/com/example/service/HabitBackgroundService.kt#L42-L77)
  - [BootReceiver.kt](app/src/main/java/com/example/data/receiver/BootReceiver.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
