# 31_ARCHITECTURE_DECISIONS — سجل القرارات الهندسية للمشروع / Architectural Decisions (ADR)

يوضح هذا السجل (ADRs) مبررات القرارات التقنية والهندسية التي تم اتخاذها أثناء تخطيط وتطوير تطبيق **HabitFlow**:

This section documents the Architectural Decision Records (ADRs) detailing the key technical choices made for **HabitFlow**:

---

## ADR 1: العمل دون اتصال كمنهج أساسي (Offline-First local storage)

* **القرار / Decision**: استخدام قاعدة بيانات Room Persistence (SQLite) لحفظ كافة بيانات المستخدم وعاداته محلياً بنسبة 100% دون خوادم سحابية.
* **السبب والسياق / Rationale**:
  * حماية خصوصية بيانات عادات المستخدم بشكل كامل.
  * تأمين تشغيل فوري وسريع للتطبيق دون انتظار استجابة الشبكات أو القلق من انقطاع الإنترنت.
  * إيقاف متطلبات حظر الحسابات أو كلمات المرور مما يحسن سرعة Onboarding.
* **الأثر / Consequences**:
  * لا توجد خيارات مزامنة تلقائية للبيانات في حال ضياع أو تبديل الهاتف (إلا عبر تفعيل نسخ أندرويد الاحتياطي القياسي Google Backup).
  * خلو الكود من مكتبات OkHttp أو Retrofit.

---

## ADR 2: واجهة المظهر الزجاجي التفاعلي بالكامل بـ Jetpack Compose

* **القرار / Decision**: بناء الواجهات والتحريكات البرمجية بالكامل باستخدام Jetpack Compose والابتعاد كلياً عن ملفات XML layouts القديمة.
* **السبب والسياق / Rationale**:
  * توفير واجهات مستخدم معاصرة وفخمة تعتمد على الشفافية والتأثيرات الزجاجية (Glassmorphism) وعناصر التمويه (Blur) ومجالات الإضاءة المنزلقة بسهولة حركية.
  * تسريع عملية التطوير باستخدام محركات الفلاتر والأشكال المعيارية (Canvas drawing) التي تتيح بناء حلقات تقدم مخصصة ونظام مرشحات تفاعلي.
* **الأثر / Consequences**:
  * الحاجة لمحاذاة دقيقة لإصدارات لغة Kotlin وإصدارات مكتبة Compose Compiler في Gradle.

---

## ADR 3: حقن الاعتماديات اليدوي (Manual Dependency Injection)

* **القرار / Decision**: تجنب استخدام مكتبات حقن الاعتماديات الكبيرة (مثل Dagger/Hilt) وتأسيس حاوية حقن وتوليد يدوية داخل فئة `HabitApplication`.
* **السبب والسياق / Rationale**:
  * تقليل حجم ملف التطبيق APK وحذف الأكواد المولدة وقت التصنيف (Codegen overhead).
  * تسهيل وتسريع زمن البناء الأولي للمشروع (Compilation times).
  * تسهيل تصحيح أخطاء التهيئة للكائنات؛ لكون تسلسل التوليد مكتوباً بوضوح ويمكن تتبعه بالسطر.
* **الأثر / Consequences**:
  * التزام المطور بالوصول اليدوي للكائنات عبر تمرير سياق التطبيق `(application as HabitApplication)`.
  * تسبب في انحراف جزئي بطريقة الوصول للمستودع وتخطي بعض حالات الاستخدام.

---

## ADR 4: التحديث الفوري المباشر لقطع الشاشة (Glance Widget Direct Update)

* **القرار / Decision**: تجاوز نظام التحديث الدوري المعياري لـ Glance والاستعانة بـ `WidgetDirectUpdater` للرسم البرمجي المباشر.
* **السبب والسياق / Rationale**:
  * يفرض نظام أندرويد قيوداً زمنية حادة على عمليات التحديث التلقائي للويدجت (قد تصل لـ 45 ثانية).
  * يطلب التطبيق استجابة فورية عند قيام المستخدم بنقر زر الإكمال من الشاشة الرئيسية، ليشعر أن التطبيق متفاعل ولحظي.
* **الأثر / Consequences**:
  * استدعاء عمليات التوليد الفوري لـ Compose views على خيط العرض الرئيسي يدوياً ثم دفع الرسوم إلى `AppWidgetManager`. تم موازنته بتفعيل التحديث المهادن Debounce لحماية بطارية الهاتف.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم مطابقة مبررات التقنيات المحددة مع الهيكل العام المكتوب للمشروع وخلو ملفات الاستيراد من Hilt والشبكات وإعدادات Glance.
* **Files Used / الملفات المستخدمة**:
  - [HabitDatabase.kt](app/src/main/java/com/example/data/local/database/HabitDatabase.kt)
  - [HabitApplication.kt](app/src/main/java/com/example/HabitApplication.kt)
  - [WidgetDirectUpdater.kt](app/src/main/java/com/example/widget/WidgetDirectUpdater.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
