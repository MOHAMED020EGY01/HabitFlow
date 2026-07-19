# 14_LIBRARIES — مكتبات واعتماديات النظام / Dependency Catalog & Audit

## قائمة مكتبات الطرف الثالث / Third-Party Dependencies Inventory

يتم تعريف كافة الاعتماديات وإصداراتها في ملف الفهرس الموحد `gradle/libs.versions.toml` لضمان اتساق البناء الفني:

All dependencies are defined in the version catalog `libs.versions.toml` and imported inside `app/build.gradle.kts`. Here is the complete audit of third-party libraries:

| اسم المكتبة / Library Name | الإصدار / Version | نطاق الاستخدام / Scope & Purpose | تقييم الموثوقية والمخاطر / Risk & Alternatives |
| :--- | :---: | :--- | :--- |
| **Jetpack Compose BOM** | 2024.09.00 | محرك وبنية الرسوم والواجهات الموحدة للتطبيق.<br>Compose UI layout framework. | **مهمة جداً / Essential**. لا تملك مخاطر حالية. الترقية ممكنة مع مراعاة إصدار Kotlin. |
| **Room Persistence** | 2.7.0 | محرك تخزين البيانات وإدارة الجداول المحلية.<br>SQLite abstraction ORM. | **مهمة جداً / Essential**. توفر استعلامات آمنة وقت التصريف. البديل: SQLDelight. |
| **Preferences DataStore** | 1.1.7 | حفظ تفضيلات المستخدم البسيطة كقيم ومفاتيح.<br>Asynchronous preferences database. | **مهمة جداً / Essential**. بديل آمن وسلس لـ SharedPreferences. لا يوجد مخاطر. |
| **WorkManager KTX** | 2.9.0 | جدولة المهام الخلفية الدورية والموثوقة.<br>Background task execution agent. | **مهمة جداً / Essential**. تضمن تشغيل التفاف السجلات اليومي في منتصف الليل. |
| **Glance Material 3** | 1.1.0 | بناء قطع الشاشة التفاعلية الخارجية للهاتف.<br>Home AppWidget UI engine. | **مهمة جداً / Essential**. تستخدم الرسوم المحلية للتغلب على قيود التحديث. |
| **Coil Compose** | 2.7.0 | معالجة تحميل الصور بشكل غير متزامن.<br>Asynchronous image downloader. | **مستخدمة محلياً / Used for Avatar**. خفيفة للغاية. البديل: Glide or Landscapist. |
| **Gson** | 2.10.1 | معالجة وتحويل الكائنات لسلاسل نصية JSON.<br>JSON parsing engine. | **تتضمن ديون فنية / Tech Debt**. مستخدمة حصراً داخل `HabitDetailViewModel` لمسار أرشفة الدورات، مما يخالف أرشفة `HabitStatusManager` التي تعتمد على تقسيم النصوص البسيط. البديل: Kotlinx Serialization. |
| **LeakCanary** | 2.14 | كشف تسربات الذاكرة في بيئة التطوير.<br>Memory leak detector (debug builds). | **أداة فحص / Audit tool**. لا يتم تضمينها في نسخ الإنتاج (debugImplementation). |
| **MockK** | 1.13.12 | تزويد اختبارات الوحدة بكائنات وهمية.<br>Mocking framework for unit tests. | **مكتبة اختبار / Test dependency**. آمنة وشائعة الاستخدام. |
| **Turbine** | 1.1.0 | تسهيل assertions لتدفقات Flow المتتالية.<br>Coroutines Flow testing helper. | **مكتبة اختبار / Test dependency**. آمنة وتمنع مشاكل الانتظار اللانهائي. |
| **Roborazzi** | 1.59.0 | التقاط لقطات شاشات الاختبار لمطابقة الرسوم.<br>Screenshot testing framework. | **مكتبة اختبار / Test dependency**. تضمن سلامة الرسم التفاعلي ومظهره. |

---

## مكتبات معلنة وغير مستخدمة (تحتاج حذف) / Unused Declared Dependencies

تم الكشف عن وجود عدد من المكاتب المصرّح بها في ملف البناء ولكن لا يوجد لها أي استدعاء أو استخدام فعلي في الكود، مما يزيد من حجم ملف التطبيق APK دون فائدة:

Several dependencies are defined in `build.gradle.kts` but have **no active usage** or imports in the source code:

* **`accompanist.permissions`**: مخصصة لإدارة صلاحيات كومبوز. (غير مستخدمة، يتم طلب الصلاحيات بالطريقة القياسية لأندرويد).
* **`retrofit` & `okhttp` & `logging-interceptor`**: مخصصة للاتصال بالشبكات السحابية. (التطبيق محلي بالكامل Offline).
* **`moshi` & `converter.moshi`**: مخصصة لتحليل النصوص JSON. (يستخدم الكود Gson بدلاً منها).
* **`firebase.bom` & `firebase.ai`**: ملحقات الذكاء الاصطناعي لـ Gemini. (لا تملك أي توظيف فعلي في الميزات الحالية).

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم مطابقة المكاتب المذكورة مع قائمة الاعتماديات في `libs.versions.toml` وملفات البناء وإجراء عمليات مسح Imports للتحقق من الاستيراد الفعلي.
* **Files Used / الملفات المستخدمة**:
  - [libs.versions.toml](gradle/libs.versions.toml)
  - [build.gradle.kts](app/build.gradle.kts#L76-L137)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
