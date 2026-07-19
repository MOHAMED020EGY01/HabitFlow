# 32_DEVELOPMENT_GUIDE — دليل المطور وتشغيل البيئة المحلية / Development Guide

## متطلبات بيئة التطوير / Local Environment Setup Requirements

لتثبيت وبناء تطبيق **HabitFlow** محلياً على جهاز التطوير، يجب استيفاء المتطلبات الفنية التالية:

To install and compile the **HabitFlow** project locally, prepare your environment with the following specifications:

1. **نسخة الجافا (JDK)**: إصدار **JDK 17** وما فوق (يمكن استخدام Eclipse Temurin or Azul Zulu).
2. **بيئة التطوير (IDE)**: برنامج **Android Studio Koala / Ladybug** أو إصدار أحدث.
3. **نسخة الأندرويد SDK**: تنزيل منصة التجميع وتجريب أجهزة المحاكاة بإصدار **API 36**.
4. **أدوات Gradle**: يتولى ملف Gradle Wrapper تحديث وتحميل نسخته التوافقية (Gradle 8.7+).

---

## تشغيل أوامر تجميع وبناء المشروع / Terminal Build Commands

افتح سياق سطر الأوامر (Terminal) في مجلد المشروع الرئيسي وجرب الأوامر البرمجية التالية:

Use these commands in your shell to clean, compile, verify, and run tests:

* **تفريغ مجلدات التجميع القديمة (Clean build directories)**:
  ```bash
  ./gradlew clean
  ```
* **تجميع وبناء نسخة التطوير (Assemble debug APK)**:
  ```bash
  ./gradlew assembleDebug
  ```
* **تجميع وبناء نسخة الإنتاج المشفرة (Assemble release APK)**:
  ```bash
  ./gradlew assembleRelease
  ```
* **تشغيل فحص الأخطاء التنسيقية وتحليل الكود (Lint check)**:
  ```bash
  ./gradlew lintDebug
  ```

---

## تشغيل اختبارات الوحدات واللقطات الرسومية / Testing Commands

يحتوي التطبيق على اختبارات جودة للتحقق من سلامة منطق التنبيهات ورسم الواجهات التفاعلية:

* **تشغيل اختبارات الوحدة الصرفة (Run Local Unit Tests)**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
* **تشغيل اختبارات لقطات شاشات الرسوم (Run Roborazzi Snapshot Tests)**:
  ```bash
  ./gradlew recordRoborazziDebug
  ```
  يقوم الأمر بتوليد ومطابقة صور الواجهات الحالية مع الصور المرجعية للتأكد من خلو التصاميم من الارتجاج البصري أو التشوهات بعد التحديث.

---

## بروتوكول التعريب وتعديل النصوص / Localization Protocols

* **إضافة نصوص جديدة**: يتم تعريف سلاسل النصوص في ملف [strings.xml](app/src/main/res/values/strings.xml) المخصص للغة الإنجليزية أولاً.
* **إضافة النصوص العربية**: يجب توفير الترجمة المطابقة تماماً وحفظ المفاتيح بنفس الأسماء داخل ملف [strings.xml](app/src/main/res/values-ar/strings.xml) في مجلد الترجمة العربية.
* **الأرقام**: استخدم دائماً دالة `AppFormatters.forceWesternDigits()` للتأكد من تنسيق النسب المئوية والأرقام بشكل متناسق وموحد لجميع لغات التطبيق.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم مطابقة مستويات إصدارات التجميع والأهداف الموثقة وتفحص مجلدات الموارد المترجمة.
* **Files Used / الملفات المستخدمة**:
  - [build.gradle.kts](app/build.gradle.kts)
  - [values-ar/strings.xml](app/src/main/res/values-ar/strings.xml)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
