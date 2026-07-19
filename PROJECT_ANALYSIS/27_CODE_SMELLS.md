# 27_CODE_SMELLS — تقرير العيوب الفنية وتنسيق الأكواد / Code Smells Audit

يحتوي هذا التقرير على رصيد شامل لعيوب الكود ونقاط الضعف التنسيقية المكتشفة في كود مشروع **HabitFlow**:

This report catalogued the design smells, anti-patterns, and formatting issues present in the **HabitFlow** codebase:

---

## 1. عيوب التصميم المعماري / Architectural Smells

### 🔴 تخطي الطبقات المعمارية (Layering Violations)
* **الوصف**: تتصل كلاسات ViewModels مباشرة مع مستودع البيانات `app.repository` لتنفيذ مهام قراءة وتعديل وسجل إنجاز العادات، مما يهمش دور حالات الاستخدام (UseCases).
* **الملفات المتأثرة**: `HomeViewModel.kt`, `AllHabitsViewModel.kt`, `HabitDetailViewModel.kt`.
* **الأثر**: تفتيت منطق العمل وصعوبة كتابة اختبارات معزولة بشكل مستقل لكل طبقة.

---

## 2. عيوب مخطط وتخزين البيانات / Schema Design Smells

### 🔴 تخزين متضارب لحقل واحد (Inconsistent Column Serialization)
* **الوصف**: يتم تخزين أرشيف دورات العادات السابقة `logsSnapshot` في نفس العمود النصي بقاعدة البيانات بصيغتين غير متوافقتين تماماً:
  1. كأرقام ونصوص مقسمة بفواصل منقوطة في `HabitStatusManager.kt` (`date:state;date:state...`).
  2. كـ JSON نصي مجمع باستخدام مكتبة Gson في `HabitDetailViewModel.kt`.
* **الأثر**: يمنع هذا التضارب التطبيق من محاولة قراءة أو معالجة العمود مستقبلاً لعرض السجلات للمستخدم، ويشكل ديناً فنياً خطيراً في طبقة التخزين.

---

## 3. عيوب تكوين المظهر والتفضيلات / Configuration Smells

### 🟡 إهمال تفضيل المظهر المختار (Hardcoded Preference Override)
* **الوصف**: يتيح التطبيق للمستخدم اختيار وتعديل مظهر التطبيق (فاتح/داكن/مع النظام) وحفظه في DataStore، ولكن يتم قفل المظهر قسرياً في [MainActivity.kt](app/src/main/java/com/example/MainActivity.kt#L126) على المظهر الداكن (`darkTheme = true`).
* **الأثر**: يجعل خيار المظهر في شاشة الإعدادات غير فعال ووهمي للمستخدم.

---

## 4. عيوب أحجام الملفات البرمجية / Structural Smells

### 🟡 ملفات الواجهات المونوليثية الضخمة (Monolithic View Files)
* **الوصف**: يحتوي ملف شاشة التفاصيل [HabitDetailScreen.kt](app/src/main/java/com/example/presentation/screens/detail/HabitDetailScreen.kt) على أكثر من **1115 سطر برمجي** يضم كود الرسم والتقويم والرسوم البيانية وتنسيق التواقيت وتصدير السلاسل الزمنية.
* **الأثر**: صعوبة قراءة وصيانة وتحديث الشاشة، وتكامل فروع العمل المشتركة.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم التحقق من أسطر حفظ الأرشيف وطريقة تهيئة المظهر المحددة وقائمة استدعاءات نماذج العرض.
* **Files Used / الملفات المستخدمة**:
  - [HabitDetailScreen.kt](app/src/main/java/com/example/presentation/screens/detail/HabitDetailScreen.kt)
  - [HabitStatusManager.kt](app/src/main/java/com/example/domain/usecase/HabitStatusManager.kt#L181)
  - [HabitDetailViewModel.kt](app/src/main/java/com/example/presentation/screens/detail/HabitDetailViewModel.kt#L165-L174)
  - [MainActivity.kt](app/src/main/java/com/example/MainActivity.kt#L126)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
