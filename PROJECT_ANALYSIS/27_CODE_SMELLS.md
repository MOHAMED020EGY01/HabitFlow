# 27_CODE_SMELLS — تقرير العيوب الفنية وتنسيق الأكواد / Code Smells Audit

This report catalogued the design smells, anti-patterns, and formatting issues present in the **HabitFlow** codebase:

---

## 1. عيوب التصميم المعماري / Architectural Smells
* **تخطي الطبقات المعمارية (Layering Violations)**: تتصل كلاسات ViewModels مباشرة مع مستودع البيانات.

---

## 2. عيوب مخطط وتخزين البيانات / Schema Design Smells
* **تخزين متضارب لحقل واحد (Inconsistent Column Serialization)**: تخزين `logsSnapshot` بصيغتين مختلفتين في `HabitStatusManager.kt` و `HabitDetailViewModel.kt`.

---

## 3. عيوب تكوين المظهر والتفضيلات / Configuration Smells
* **إهمال تفضيل المظهر المختار (Hardcoded Preference Override)**: قفل المظهر قسرياً في `MainActivity.kt` على المظهر الداكن.

---

## 4. عيوب أحجام الملفات البرمجية / Structural Smells
* **ملفات الواجهات المونوليثية الضخمة (Monolithic View Files)**: يحتوي ملف `HabitDetailScreen.kt` على أكثر من 1100 سطر.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**: تم التحقق من أسطر حفظ الأرشيف وطريقة تهيئة المظهر المحددة.
* **Files Used / الملفات المستخدمة**:
  - [HabitDetailScreen.kt](app/src/main/java/com/example/feature/habit/presentation/HabitDetailScreen.kt)
  - [HabitStatusManager.kt](app/src/main/java/com/example/core/domain/usecase/HabitStatusManager.kt)
  - [MainActivity.kt](app/src/main/java/com/example/app/MainActivity.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
