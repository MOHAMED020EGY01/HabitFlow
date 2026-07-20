# 14_LIBRARIES — مكتبات واعتماديات النظام / Dependency Catalog & Audit

## قائمة مكتبات الطرف الثالث / Third-Party Dependencies Inventory

All dependencies are defined in the version catalog `libs.versions.toml` and imported inside `app/build.gradle.kts`.

| اسم المكتبة / Library Name | الإصدار / Version | نطاق الاستخدام / Scope & Purpose |
| :--- | :---: | :--- |
| **Jetpack Compose BOM** | 2024.09.00 | محرك وبنية الرسوم والواجهات الموحدة للتطبيق. |
| **Room Persistence** | 2.7.0 | محرك تخزين البيانات وإدارة الجداول المحلية. |
| **Preferences DataStore** | 1.1.7 | حفظ تفضيلات المستخدم البسيطة كقيم ومفاتيح. |
| **WorkManager KTX** | 2.9.0 | جدولة المهام الخلفية الدورية والموثوقة. |
| **Glance Material 3** | 1.1.0 | بناء قطع الشاشة التفاعلية الخارجية للهاتف. |
| **Coil Compose** | 2.7.0 | معالجة تحميل الصور بشكل غير متزامن. |
| **Gson** | 2.10.1 | معالجة وتحويل الكائنات لسلاسل نصية JSON. |
| **LeakCanary** | 2.14 | كشف تسربات الذاكرة في بيئة التطوير. |
| **MockK** | 1.13.12 | تزويد اختبارات الوحدة بكائنات وهمية. |
| **Turbine** | 1.1.0 | تسهيل assertions لتدفقات Flow المتتالية. |
| **Roborazzi** | 1.59.0 | التقاط لقطات شاشات الاختبار لمطابقة الرسوم. |

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**: مطابقة المكاتب المذكورة مع `libs.versions.toml` وملفات البناء.
* **Files Used / الملفات المستخدمة**:
  - [libs.versions.toml](gradle/libs.versions.toml)
  - [build.gradle.kts](app/build.gradle.kts)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
