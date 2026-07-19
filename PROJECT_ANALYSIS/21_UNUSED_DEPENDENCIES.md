# 21_UNUSED_DEPENDENCIES — تقرير الاعتماديات المعطلة والزائدة / Unused Dependencies

## حصر الاعتماديات المعرّفة وغير المستوردة / Unused Declared Dependencies

تمت مراجعة وتحليل ملف تهيئة تجميع وحدة التطبيق [build.gradle.kts](app/build.gradle.kts) ومقارنته مع الواردات والملفات المرجعية لمجلد الأكواد البرمجية، وخلص الفحص الفني إلى رصد عدد من المكتبات المعرّفة التي لا تملك أي استخدام أو إشارة برمجية (Imports) في الكود:

A comparison of `build.gradle.kts` against the imports of all Kotlin source files reveals several dependencies declared in the build script that are never imported. They should be pruned:

* **`androidx.accompanist:accompanist-permissions`**:
  * *الغرض الأصلي*: تبسيط التعامل مع طلبات الصلاحيات في كومبوز.
  * *الاستخدام الفعلي*: يتم طلب الصلاحيات باستخدام واجهات أندرويد القياسية والمنصات التابعة لكومبوز natively (مثل `rememberLauncherForActivityResult`).
  * *التوصية*: الحذف.

* **`com.squareup.retrofit2:retrofit` & `com.squareup.retrofit2:converter-gson`**:
  * *الغرض الأصلي*: الاتصال بالشبكات الخارجية وجلب الاستعلامات السحابية.
  * *الاستخدام الفعلي*: لا توجد اتصالات شبكية، التطبيق يعمل كلياً دون اتصال بالإنترنت (Offline-First).
  * *التوصية*: الحذف.

* **`com.squareup.okhttp3:okhttp` & `com.squareup.okhttp3:logging-interceptor`**:
  * *الغرض الأصلي*: عميل اتصال ونظام تسجيل عمليات الشبكة.
  * *الاستخدام الفعلي*: غير مستعمل؛ لغياب الاتصالات بالإنترنت.
  * *التوصية*: الحذف.

* **`com.squareup.moshi:moshi` & `com.squareup.moshi:moshi-kotlin` & `com.squareup.retrofit2:converter-moshi`**:
  * *الغرض الأصلي*: محلل ومحول كائنات JSON.
  * *الاستخدام الفعلي*: يتم استخدام مكتبة Gson محلياً بدلاً منها داخل نماذج العرض.
  * *التوصية*: الحذف.

* **`com.google.firebase:firebase-bom` & `com.google.firebase:firebase-vertexai`**:
  * *الغرض الأصلي*: تكوين ميزات الذكاء الاصطناعي السحابية Gemini AI.
  * *الاستخدام الفعلي*: لا تملك أي فئة برمجية للتحقق أو استدعاء لخدمات الذكاء الاصطناعي أو الـ Firebase.
  * *التوصية*: الحذف.

---

## أثر بقاء هذه المكاتب / Implications of Unused Dependencies

1. **زيادة حجم ملف التطبيق (Binary Size Inflation)**: على الرغم من أن R8/ProGuard يقومان بحذف المكاتب غير المستخدمة أثناء التجميع النهائي، إلا أن إدراجها يبطئ من فترات المزامنة والبناء (Sync & Compilation times) ويزيد من حجم مجلدات الكاش المحفوظة للتطوير.
2. **الديون التحديثية (Maintenance Burden)**: تحتاج هذه المكاتب لتتبع وتحديث مستمر لإصداراتها في `libs.versions.toml` لمواكبة متطلبات الأمان والتكامل دون الحاجة الفعلية لها.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم إجراء عمليات بحث شاملة داخل حزم التطبيق عن نصوص الاستيراد مثل `retrofit2`, `okhttp3`, `accompanist.permissions`, `firebase` وتبين عدم استخدامها إطلاقاً.
* **Files Used / الملفات المستخدمة**:
  - [build.gradle.kts](app/build.gradle.kts#L76-L137)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
