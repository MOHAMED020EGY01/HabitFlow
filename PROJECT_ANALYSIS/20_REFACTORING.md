# 20_REFACTORING — خطة إعادة الهيكلة والتطوير المقترحة / Refactoring Roadmap

يقدم هذا الدليل خطة هندسية منظمة ومجزأة للتخلص من العيوب البرمجية والديون الفنية المتراكمة في كود تطبيق **HabitFlow**:

This guide outlines a step-by-step engineering roadmap to resolve structural code smells and technical debt:

---

## المرحلة الأولى: تجميع وإعادة ضبط المعمارية / Architecture Alignment

### 1. تأسيس حالات استخدام مستقلة (Introduce Missing Use Cases)
* **المشكلة**: التفاف الـ ViewModels للاتصال المباشر بالمستودع `app.repository`.
* **الحل**:
  * إنشاء فئة `ToggleHabitCheckInUseCase` و `GetActiveHabitsUseCase` و `GetCycleHistoryUseCase`.
  * تحويل الاستدعاءات داخل نماذج العرض لتوجيهها حصراً عبر كتل الاستعمال النظيفة.

### 2. توحيد صيغة حفظ الأرشيف (Synchronize Cycle Snapshot Schema)
* **المشكلة**: تخزين سجل الإنجاز للدورات السابقة بصيغتين متنافيتين (Gson JSON vs Semicolon String).
* **الحل**:
  * اختيار تنسيق موحد. يُنصح باستخدام صيغة JSON لمرونتها ودعم حقول إضافية مستقبلاً.
  * كتابة فئة هجرة توافقية لتعديل السجلات القديمة المخزنة بنمط النص المنقوط، أو إتاحة محلل ذكي داخل الكود يتعرف تلقائياً على نوع السلسلة النصية ويقوم بتحليلها بالشكل المناسب.

---

## المرحلة الثانية: تجزئة ملفات الواجهة المونوليثية / Monolithic Screens Decoupling

تحسين شاشة تفاصيل العادة [HabitDetailScreen.kt](app/src/main/java/com/example/presentation/screens/detail/HabitDetailScreen.kt) المكونة من 1115 سطر:

* **الخطوة 1**: اقتطاع واجهة `PreviousCyclesCard` ونقلها إلى حزمة المكونات الفرعية: `presentation/components/PreviousCyclesCard.kt`.
* **الخطوة 2**: استخراج أداة رصد الإحصائيات ورسوم التقدم الدائرية الزجاجية وتثبيتها في ملف مستقل.
* **الخطوة 3**: تنظيف ملف الشاشة الرئيسي بحيث يحتفظ فقط بالـ Scaffold والـ NavHost وإدارة حالات العرض المحددة.

---

## المرحلة الثالثة: إصلاح منطق التفضيلات والمظاهر / Preferences & Theme Fixes

* **الخطوة 1**: تعديل دالة التهيئة في `MainActivity.kt`.
* **الخطوة 2**: قراءة تيار السمة الحالية المحددة من قبل المستخدم كـ State:
  ```kotlin
  val currentTheme by preferencesManager.appThemeFlow.collectAsState(initial = "system")
  val isDarkTheme = when(currentTheme) {
      "dark" -> true
      "light" -> false
      else -> isSystemInDarkTheme()
  }
  ```
* **الخطوة 3**: تمرير `isDarkTheme` بدلاً من القيمة الثابتة الصلبة `darkTheme = true` لتفعيل إمكانية تبديل المظاهر.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم صياغة الحلول وهياكل التعديل بناءً على نقاط الضعف التي تم التحقق من أسطرها في الملفات `HabitDetailScreen.kt` و `MainActivity.kt` و `HabitStatusManager.kt`.
* **Files Used / الملفات المستخدمة**:
  - [HabitDetailScreen.kt](app/src/main/java/com/example/presentation/screens/detail/HabitDetailScreen.kt)
  - [MainActivity.kt](app/src/main/java/com/example/MainActivity.kt)
  - [HabitStatusManager.kt](app/src/main/java/com/example/domain/usecase/HabitStatusManager.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
