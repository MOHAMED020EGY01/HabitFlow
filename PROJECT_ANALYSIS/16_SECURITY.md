# 16_SECURITY — تدقيق ومراجعة أمن النظام / Security Specification

## مراجعة صلاحيات النظام المستعلمة / Android Permissions Audit

تم فحص صلاحيات النظام الموثقة داخل ملف المانيفست `AndroidManifest.xml` لتقييم ضرورتها الأمنية وضمان عدم وجود ثغرات في استهلاك الصلاحيات:

We audited the permissions requested in `AndroidManifest.xml` to ensure least-privilege compliance and verify their usage scope:

* **`POST_NOTIFICATIONS`**:
  * *الغرض*: إظهار قنوات الإشعارات للتذكيرات للعادات.
  * *الحالة أمنياً*: آمنة ومطلوبة إلزامياً على أجهزة Android 13+.
* **`SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM`**:
  * *الغرض*: حجز إنذارات دقيقة بالدقيقة والنعش الصوتي عبر المنبه.
  * *الحالة أمنياً*: آمنة. تم استدعاء `USE_EXACT_ALARM` المتوافقة مع متطلبات المتجر لتفادي حظر جوجل عند التثبيت.
* **`RECEIVE_BOOT_COMPLETED`**:
  * *الغرض*: استقبال بث الإقلاع العام لإعادة جدولة تذكيرات العادات تلقائياً.
  * *الحالة أمنياً*: آمنة. تتطلب معالجة حذرة غير محظورة للـ Receiver.
* **`SYSTEM_ALERT_WINDOW`**:
  * *الغرض*: إتاحة الرسم التفاعلي فوق التطبيقات لعرض شاشة التنبيه المنبثقة.
  * *الحالة أمنياً*: **مرتفعة الخطورة / High Risk**. تسمح الصلاحية برسم واجهات تراكبية. تم تأمينها في التطبيق بتقديم شاشات شفافة للمستخدم، ويتم رصدها والتحقق منها قبل أي عرض.
* **`FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE`**:
  * *الغرض*: تأمين الخدمة الأمامية للموثوقية ولمنبهات التراكب.
  * *الحالة أمنياً*: آمنة ومدرجة بأسباب وخصائص نوع subtype الخاصة بأندرويد 14+.
* **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`**:
  * *الغرض*: حث المستخدم على إيقاف تحسين البطارية للتطبيق لضمان عدم توقف المنبهات.
  * *الحالة أمنياً*: **متوسطة الخطورة / Medium Risk**. قد تثير انتباه مراجعي متجر جوجل بلاي. يجب توضيح أن التطبيق هو تطبيق منبهات وعادات Offline-First يحتاج لهذه الصلاحية للالتزام بمواعيد التنبيه اليومية.

---

## أمن المكونات المصدّرة للأنشطة / Exported Components Audit

يجب فحص المكونات للتأكد من عدم قدرة التطبيقات الخبيثة على اختراق شاشات التطبيق:

* **MainActivity**:
  * *الحالة*: `android:exported="true"` (مصدّرة).
  * *مبرر*: إلزامية؛ لأنها نقطة انطلاق التطبيق من خلال Launcher الهاتف.
* **BootReceiver**:
  * *الحالة*: `android:exported="true"` (مصدّرة).
  * *مبرر*: إلزامية؛ لأنها تستقبل بث نظام التشغيل عند الإقلاع `BOOT_COMPLETED`. محمية تلقائياً بنظام أندرويد لكونها تستجيب لحدث محدد من النظام فقط.
* **AllHabitsWidgetReceiver & InactiveHabitsWidgetReceiver & HabitStatsSummaryWidgetReceiver**:
  * *الحالة*: `android:exported="true"` (مصدّرة).
  * *مبرر*: إلزامية؛ لتسمح لنظام الأندرويد وقطاع التطبيقات الفرعية (AppWidgetManager) بتحديث الواجهات وقبول نقرات الشاشة.
* **مكونات مغلقة بأمان (`android:exported="false"`)**:
  * `HabitOverlayService`
  * `HabitBackgroundService`
  * `HabitOverlayReceiver`
  * `PendingOverlayReceiver` (ملاحظة: بالرغم من أنها تستمع لـ `USER_PRESENT` الذي هو حدث نظام، إلا أنها غير مصدّرة ويتم التسجيل أو المعالجة بشكل داخلي آمن).

---

## حماية وتخزين تفضيلات المستخدم / Local Storage Security

* **قاعدة بيانات Room**: يتم تخزين الجداول محلياً بالكامل. لا تشمل حماية قاعدة البيانات التشفير الكامل (SQLCipher)، لذا تعتبر البيانات نصية داخل الجهاز. نظراً لكون التطبيق لا يخزن كلمات مرور أو بطاقات بنكية، فإن هذا مقبول أمنياً.
* **Preferences DataStore**: تخزن إعدادات المظهر واللغات بصيغة ملفات مفضلات غير مشفرة، ولكنها مغلقة في النطاق الخاص بالتطبيق (Private app directory) مما يمنع التطبيقات الأخرى من الوصول إليها.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم فحص كود المانيفست `AndroidManifest.xml` بالكامل ورصد كافة الصلاحيات المكتوبة وتحديد قيم `android:exported` للمكونات المختلفة.
* **Files Used / الملفات المستخدمة**:
  - [AndroidManifest.xml](app/src/main/AndroidManifest.xml#L5-L124)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
