# 15_BUILD_SYSTEM — تكوين نظام بناء وجدولة المشروع / Build System & Toolchain

## تفاصيل تهيئة البناء والأهداف / Build SDK Configuration

يتم إدارة بناء تطبيق **HabitFlow** باستخدام أداة **Gradle** عبر ملفات كوتلن التفاعلية (`.gradle.kts`). إعدادات البناء الأساسية هي كالتالي:

The build specifications are managed globally using Gradle Kotlin DSL (`.gradle.kts`). The compile and compatibility variables are set as follows:

* **compileSdk**: 36 (استهداف أحدث واجهات برمجية لنظام أندرويد لضمان استخدام التحسينات الأمنية).
* **targetSdk**: 36 (ضمان توافق تشغيل كامل مع أحدث إصدارات نظام أندرويد 15).
* **minSdk**: 24 (تأمين عمل التطبيق لأي جهاز يعمل بنظام Android 7.0 Nougat وما فوق).
* **معرف التطبيق (Application ID)**: `com.aistudio.habittracker.yfnwdp`.
* **نسخة الكود (Version Code)**: 1 (رقم الإصدار الداخلي للمتجر).
* **اسم النسخة (Version Name)**: "1.0" (رقم النسخة المعروضة للمستخدم).

---

## تجميع وإتاحة الميزات الحديثة للجافا / SDK Desugaring

للسماح للمطور باستخدام وظائف وتواريخ ومكتبات جافا الحديثة (مثل `java.time.LocalDate` أو `java.time.Duration`) مع المحافظة على التوافق التام مع الأجهزة القديمة التي تعمل بـ API 24:
* تم تفعيل علم محول الميزات التوافقي: `isCoreLibraryDesugaringEnabled = true` في إعدادات تجميع خيارات التطبيق.
* تم إدراج مكتبة محول الميزات التوافقية للمكتبات الأساسية: `coreLibraryDesugaring(libs.desugar.jdk.libs)` بقيمة إصدار `2.1.4`.
* يتكامل هذا مع لغة تجميع جافا 11 للترجمة المتوافقة: `sourceCompatibility = JavaVersion.VERSION_11` و `targetCompatibility = JavaVersion.VERSION_11`.

---

## تحسين وحماية الكود وقت الإنتاج / Minification & Proguard Optimization

عند بناء التطبيق لنسخ النشر والإنتاج (`release buildType`)، يتم تفعيل القواعد التالية لتقليل حجم ملف التطبيق APK وحمايته من التفكيك العكسي:

* **تقليل حجم الصور**: `isCrunchPngs = true` لتفعيل ضغط صور PNG تلقائياً.
* **إزالة الأكواد غير المستخدمة (Minification)**: `isMinifyEnabled = true` لتفعيل معالج R8 لإلغاء المكاتب والملفات الزائدة وتعمية الأسماء.
* **تصفية الموارد (Resource Shrinking)**: `isShrinkResources = true` لحذف الصور والملفات غير المستدعية والواردة في مجلد الموارد `res/`.
* **ملف القواعد المخصصة**: `proguard-rules.pro` لحفظ سلامة فئات كائنات قاعدة البيانات Room و Glance و Gson و WorkManager التي قد تتوقف في حال تغيير أسمائها برمجياً.

---

## إدارة وتخزين أسرار البناء / Build Secrets Management

يعتمد البناء على أداة الإضافات الأمنية **Secrets Gradle Plugin** بقيمة إصدار `2.0.1`:
* تقوم الأداة بالبحث التلقائي وقراءة ملفات التفضيلات البيئية `.env` و `.env.example` لتسجيل مفاتيح التوقيع وقواعد البيانات بأمان داخل ملف البناء التلقائي `BuildConfig` دون إتاحتها علناً في مستودعات Git العامة.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم فحص كود ملف `app/build.gradle.kts` وملف الإعدادات الموحد وقواعد برو جارد ومطابقة الأرقام والإعدادات بالكامل.
* **Files Used / الملفات المستخدمة**:
  - [build.gradle.kts](app/build.gradle.kts#L9-L70)
  - [proguard-rules.pro](app/proguard-rules.pro)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
