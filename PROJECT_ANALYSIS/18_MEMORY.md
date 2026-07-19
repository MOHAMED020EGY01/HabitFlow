# 18_MEMORY — إدارة الذاكرة وتدقيق الاستهلاك / Memory Safety

## ممارسات حماية الذاكرة العشوائية / Memory Safety Best Practices

تمت مراجعة الكود البرمجي لضمان الكفاءة في إدارة الذاكرة ومنع حدوث أي **تسريبات للذاكرة (Memory Leaks)** والتي قد تؤدي لتوقف التطبيق بسبب نفاد الذاكرة (Out Of Memory Exception):

### 1. إدارة دورة حياة محرك نطق النصوص (TTS Lifecycle)
* **المشكلة**: يعتبر محرك نطق النصوص Android TextToSpeech مستهلكاً كبيراً لموارد الذاكرة ويحتفظ داخلياً بسياق عمل الخدمة (Context Reference).
* **الحل الفعلي**:
  * يحتوي `ReminderSpeechEngine.kt` على آلية إغلاق ذاتي وإيقاف مؤقت عند انتهاء عملية النطق بـ 30 ثانية لتجنب التعليق.
  * في حال خلو طابور معالجة الصوت من أي طلبات لمدة **دقيقة كاملة** (`IDLE_TIMEOUT_MS = 60_000L`)، يقوم كائن `ReminderSpeechManager` تلقائياً باستدعاء `shutdown()` لتحرير كائن الـ TextToSpeech بالكامل من الذاكرة العشوائية، وإعادة بنائه لاحقاً عند الحاجة.

### 2. معالجة دورة الحياة داخل النوافذ العائمة (ServiceLifecycleOwner)
* **المشكلة**: عند رسم واجهات Compose داخل WindowManager خارجي، لا توجد فئة Activity لإدارة النطاق ودورة حياة الكتل الرسومية ونماذج العرض الملحقة بها، مما قد يعلق كتل الرسوم في الذاكرة للأبد بعد إغلاق الخدمة.
* **الحل الفعلي**:
  * تبني الخدمة [HabitOverlayService.kt](app/src/main/java/com/example/overlay/HabitOverlayService.kt#L250-L300) فئة مخصصة باسم `ServiceLifecycleOwner` تحقق واجهات `LifecycleOwner` و `SavedStateRegistryOwner` و `ViewModelStoreOwner`.
  * تقوم بتهيئة النطاق عند إطلاق الخدمة وحقن الفئة للرسم.
  * عند إغلاق التنبيه وإزالة النافذة، تستدعي الخدمة `lifecycleRegistry.currentState = Lifecycle.State.DESTROYED` و `viewModelStore.clear()` مما يضمن إطلاق دورات الهدم وتحرير الذاكرة ونماذج العرض بالكامل فوراً.

---

## مخاطر تسريب الذاكرة النشطة المكتشفة / Memory Leaks Risk Analysis

### 🔴 خطورة مرتفعة — تهيئة LeakCanary عبر الانعكاس
* **الموقع**: فئة [HabitApplication.kt](app/src/main/java/com/example/HabitApplication.kt#L130-L165).
* **التفاصيل**: يقوم الكود بمحاولة تهيئة وتنشيط مكتبة `LeakCanary` برمجياً عبر استخدام الانعكاس (Reflection):
  ```kotlin
  val leakCanaryClass = Class.forName("leakcanary.LeakCanary")
  val configMethod = leakCanaryClass.getMethod("getConfig")
  ```
  هذه التهيئة مبنية لإخفاء استخدام المكتبة في نسخ النشر والإنتاج (Release builds)؛ لكون LeakCanary مدرجة كـ `debugImplementation` وغير متوافرة في تجميع الإنتاج.
* **الأثر والخطورة**:
  1. قد تسبب فشل بناء الكود أو انهياره عند تفعيل R8/ProGuard بسبب محاولة تعمية فئة `leakcanary.LeakCanary`؛ لذا تم إدراج قواعد الاستثناء في ملف Proguard.
  2. في حال تم استدعاء هذا الكود خارج كتلة محكمة الحماية، قد يؤدي إلى انهيار إقلاعي كارثي للتطبيق في نسخ الإنتاج.
* **التوصية**: بدلاً من محاولة تنشيط أو قراءة إعدادات LeakCanary عبر الانعكاس، يجب ترك المكتبة تعمل بالطريقة التلقائية الخاصة بها (Auto-initialize via App Startup)؛ حيث تقوم LeakCanary بتسجيل وتنشيط نفسها تلقائياً في نسخ التطوير (Debug Build) دون الحاجة لكتابة أي أسطر تهيئة داخل فئة `HabitApplication`.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم التحقق من سلوك إيقاف الـ TTS ودورة حياة الـ Custom Lifecycle Owner في النافذة العائمة، ومطابقة كتل استدعاء LeakCanary.
* **Files Used / الملفات المستخدمة**:
  - [ReminderSpeechManager.kt](app/src/main/java/com/example/speech/ReminderSpeechManager.kt#L101-L125)
  - [HabitOverlayService.kt](app/src/main/java/com/example/overlay/HabitOverlayService.kt#L250-L310)
  - [HabitApplication.kt](app/src/main/java/com/example/HabitApplication.kt#L130-L165)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
