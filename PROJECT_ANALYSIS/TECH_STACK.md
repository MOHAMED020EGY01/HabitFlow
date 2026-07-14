# TECH_STACK

## التقنيات المستخدمة في HabitFlow / HabitFlow Technology Stack

### جدول التقنيات الكامل / Full Tech Stack Table

| الفئة / Category | التقنية / Technology | الإصدار / Version | الغرض / Purpose |
| :--- | :--- | :---: | :--- |
| **المنصة / Platform** | Android | API 24–36 | هدف التشغيل / Target runtime |
| **اللغة / Language** | Kotlin | 2.2.10 | لغة البرمجة الأساسية / Primary language |
| **الواجهة / UI** | Jetpack Compose | BOM 2024.09.00 | محرك الواجهات التفاعلية / Declarative UI |
| **الواجهة / UI** | Material 3 | (عبر BOM) | نظام التصميم / Design system |
| **الواجهة / UI** | AppCompat | 1.7.0 | توافق الإصدارات القديمة / Backwards compat |
| **قاعدة البيانات / DB** | Room | 2.7.0 | ORM فوق SQLite / Local ORM |
| **المفضلات / Prefs** | DataStore Preferences | 1.1.7 | تخزين الإعدادات / Settings storage |
| **الخلفية / Background** | WorkManager KTX | 2.9.0 | المهام الدورية / Scheduled tasks |
| **التزامن / Concurrency** | Kotlinx Coroutines | 1.10.2 | البرمجة غير المتزامنة / Async programming |
| **الويدجت / Widgets** | Glance Material 3 | 1.1.0 | قطع الشاشة الرئيسية / AppWidgets |
| **الصور / Images** | Coil Compose | 2.7.0 | تحميل الصور / Async image loading |
| **الشاشة الترحيبية** | Splash Screen API | 1.0.1 | شاشة البداية / Launch screen |
| **الإنشاء / Build** | Android Gradle Plugin | 9.1.1 | أداة البناء / Build tool |
| **المعالج / Annotation** | KSP | 2.3.5 | معالجة تعليقات Room / Symbol processor |
| **الأسرار / Secrets** | Secrets Gradle Plugin | - | إدارة مفاتيح البناء / Build key management |
| **الاختبار / Test** | JUnit | 4.13.2 | اختبار الوحدات / Unit testing |
| **الاختبار / Test** | MockK | 1.13.12 | كائنات وهمية / Mocking |
| **الاختبار / Test** | Turbine | 1.1.0 | اختبار التدفقات / Flow testing |
| **الاختبار / Test** | Roborazzi | 1.59.0 | لقطات الشاشة / Screenshot testing |
| **الاختبار / Test** | Robolectric | 4.12.1 | اختبار أندرويد محلياً / JVM test env |
| **التصحيح / Debug** | LeakCanary | 2.14 | كشف تسريبات الذاكرة / Memory leak detection |

---

## اعتماديات معلنة وغير مستخدمة / Declared But Unused Dependencies

هذه المكتبات موجودة في `app/build.gradle.kts` لكن لا يوجد أي استخدام فعلي لها في كود المشروع:

| المكتبة / Library | الغرض المُقصود / Intended Purpose | السبب (غير مستخدمة) / Reason Unused |
| :--- | :--- | :--- |
| `accompanist.permissions` | إدارة الصلاحيات في Compose | الصلاحيات تُطلب بالطريقة التقليدية |
| `retrofit` + `okhttp` | شبكة HTTP / API calls | التطبيق محلي بالكامل (Offline) |
| `moshi` + `converter.moshi` | تحليل JSON | يُستخدم Gson بدلاً منه |
| `firebase.bom` + `firebase.ai` | خدمات Google Cloud / Gemini AI | لا يوجد استدعاء AI أو خلفية سحابية |

---

## الأدوات والمكونات الإضافية / Build Plugins & Tools

| المكون / Plugin | الغرض / Purpose |
| :--- | :--- |
| `kotlin.android` | تفعيل دعم كوتلن لأندرويد |
| `kotlin.compose` | تفعيل مترجم كومبوز المدمج |
| `com.android.application` | تحديد الوحدة كتطبيق أندرويد |
| `com.google.devtools.ksp` | معالجة تعليقات Room وتوليد الكود |
| `com.google.gms.google-services` | (محجوز لـ Firebase عند تفعيله) |
| `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` | تمرير المفاتيح لـ BuildConfig بأمان |
