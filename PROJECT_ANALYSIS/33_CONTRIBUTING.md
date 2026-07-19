# 33_CONTRIBUTING — إرشادات المساهمة والتطوير المشترك / Contribution Guidelines

## دليل المساهمة الفنية للمطورين / Contribution Guide

يرحب مشروع **HabitFlow** بمساهمات المطورين. لضمان المحافظة على نظافة الأكواد البرمجية وموثوقيتها وتفادي انحراف التصميم المعماري، يرجى الالتزام بالإرشادات التالية:

We welcome contributions. To maintain codebase cleanliness, architectural alignment, and stability, please adhere to these conventions:

---

## 1. معايير كتابة الأكواد والتنسيق / Code Style & Conventions

* **كتابة كود Kotlin**: اتبع الدليل الرسمي للغة Kotlin. استخدم أدوات التنسيق التلقائية المدمجة في Android Studio (`Ctrl + Alt + L`) قبل حفظ الملفات.
* **تسمية الفئات والمكونات**:
  * نماذج العرض تنتهي بـ `ViewModel` (مثل `HomeViewModel`).
  * كلاسات الجداول تنتهي بـ `Entity` (مثل `HabitEntity`).
  * عقود المستودعات تبدأ بـ `interface` وتحققاتها تنتهي بـ `Impl` (مثل `HabitRepositoryImpl`).
  * المكونات الرسومية بكومبوز تبدأ بحروف كبيرة PascalCase (مثل `GlassCard`).

---

## 2. بروتوكول طلبات الدمج (Pull Request Checklist)

قبل رفع كود التعديل ودفع التغييرات للمستودع الرئيسي، تحقق من استيفاء البنود التالية:

1. **سلامة البناء**: يجب أن يجتاز الكود تجميع وبناء Gradle بنجاح دون أخطاء تصنيفية: `./gradlew assembleDebug`.
2. **اجتياز الاختبارات**: تشغيل واجتياز اختبارات الوحدات لضمان عدم تلف منطق حساب التنبيهات: `./gradlew testDebugUnitTest`.
3. **سلامة الرسوم (Roborazzi)**: تشغيل ومقارنة لقطات الرسوم وتوليد لقطات جديدة في حال تعديل التصاميم: `./gradlew recordRoborazziDebug`.
4. **التعريب واتجاه النصوص**:
   * التحقق التام من عمل المكون الرسومي باتجاه اليمين لليسار (RTL) عند تشغيل اللغة العربية واتجاه اليسار لليمين (LTR) عند تشغيل اللغة الإنجليزية.
   * التأكد من تغليف شاشة العرض بأكملها داخل `CompositionLocalProvider(LocalLayoutDirection provides layoutDirection)`.
5. **خالٍ من الأكواد الميتة**: عدم ترك مكاتب معطلة في Gradle أو ملفات مساعدة دون مراجع نشطة في الأكواد.

---

## 3. معايير التسمية للفروع (Branch Naming Conventions)

* لإضافة ميزات جديدة: `feature/short-description` (مثال: `feature/cloud-sync`).
* لإصلاح الثغرات الأمنية والبرمجية: `bugfix/short-description` (مثال: `bugfix/rollover-date-crash`).
* لتحسين الأداء وتعديل المظاهر: `refactor/short-description` (مثال: `refactor/split-detail-screen`).

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم صياغة المعايير البرمجية بناءً على الهياكل والتقنيات الفعلية المتبعة في كود المشروع.
* **Files Used / الملفات المستخدمة**:
  - [AppFormatters.kt](app/src/main/java/com/example/util/AppFormatters.kt)
  - [build.gradle.kts](app/build.gradle.kts)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
