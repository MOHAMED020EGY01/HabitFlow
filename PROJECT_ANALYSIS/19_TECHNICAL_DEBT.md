# 19_TECHNICAL_DEBT — الديون الفنية وهندسة التحديث / Technical Debt Audit

يوضح هذا المستند حصر الديون الفنية والانحرافات التنسيقية المكتشفة في كود تطبيق **HabitFlow**:

This document aggregates identified technical debt, design discrepancies, and code optimization tasks:

---

## 1. انحراف التصميم المعماري (Architectural Layer Bypass)
* **الموقع**: `HomeViewModel.kt`, `AllHabitsViewModel.kt`, `HabitDetailViewModel.kt`.
* **المشكلة**: تتصل نماذج العرض هذه بشكل مباشر مع مستودع البيانات `app.repository` لقراءة وتأكيد إكمال العادات وتخطي دور حالات الاستخدام (UseCases).
* **الأثر**: تفتت منطق العمل وتبعثر كود التحقق بعيداً عن حزمة `domain/usecase`.
* **التوصية**: بناء حالات استخدام معيارية مثل `GetActiveHabitsUseCase` و `ToggleLogCheckInUseCase` وتوجيه طلبات نماذج العرض من خلالها.

---

## 2. تعارض وتناقض تمثيل البيانات المكتوبة (Inconsistent Logs Snapshot Format)
* **الموقع**: `HabitStatusManager.kt` و `HabitDetailViewModel.kt`.
* **المشكلة**: يتم كتابة وحفظ أرشيف دورات العادات السابقة `logsSnapshot` في عمود نصي واحد بقاعدة البيانات بصيغتين مختلفتين تماماً:
  1. تقوم فئة [HabitStatusManager.kt](app/src/main/java/com/example/domain/usecase/HabitStatusManager.kt#L181) بحفظ البيانات كقائمة نصوص مقسمة بفواصل منقوطة: `date:state;date:state...`.
  2. بينما تقوم فئة [HabitDetailViewModel.kt](app/src/main/java/com/example/presentation/screens/detail/HabitDetailViewModel.kt#L165-L174) بتحويل السجل وحفظه بصيغة سلسلة نصوص JSON مجمعة باستخدام مكتبة Gson.
* **الأثر**: على الرغم من أن الشاشات الحالية لا تقوم بقراءة أو عرض هذا الحقل النصي في الواجهات بل تكتفي بنسب الإنجاز العامة، إلا أن هذا التناقض يشكل ديناً فنياً خطيراً يمنع المطور من محاولة إعادة بناء شاشة لعرض السجل التاريخي للدورات السابقة بشكل موحد.
* **التوصية**: توحيد صيغة الحفظ والاستعانة بـ `kotlinx.serialization` لتوحيد تنسيق التخزين النصي.

---

## 3. تجاهل وحظر رغبات مظاهر الشاشة (Hardcoded Dark Theme Override)
* **الموقع**: فئة [MainActivity.kt](app/src/main/java/com/example/MainActivity.kt#L126).
* **المشكلة**: يمتلك التطبيق إعدادات تيح للمستخدم تغيير مظهر الشاشة (فاتح / داكن / مع النظام) وحفظ التفضيل بـ DataStore. ولكن داخل دالة onCreate للنشاط الرئيسي يتم حظر التفضيل وتطبيق مظهر داكن إجباري: `darkTheme = true`.
* **الأثر**: يتم إهمال وتجاهل تفضيل المستخدم الفعلي، مما يجعل خيارات المظهر في شاشة الإعدادات غير فعالة ومضللة.
* **التوصية**: ربط متغير `darkTheme` بتيار البيانات المستورد من `preferencesManager.appThemeFlow` ودعم تبديل المظهر الحركي.

---

## 4. حجم ملفات الواجهات البرمجية الضخم (Monolithic Screen Files)
* **الموقع**: شاشة تفاصيل العادة [HabitDetailScreen.kt](app/src/main/java/com/example/presentation/screens/detail/HabitDetailScreen.kt) وشاشة منشئ العادات [AddHabitScreen.kt](app/src/main/java/com/example/presentation/screens/add/AddHabitScreen.kt).
* **المشكلة**: يحتوي ملف `HabitDetailScreen.kt` على ما يزيد عن **1115 سطر برمجي** تضم شاشات التقويم الدائري والرسوم البيانية وعناصر التحكم والتحديث.
* **الأثر**: صعوبة القراءة وتحديث الأكواد وصعوبة صيانة الملف البرمجي وزيادة فترات دمج الفروع في مشاريع العمل المشترك.
* **التوصية**: فصل العناصر الرسومية المساعدة (مثل `PreviousCyclesCard` و `DayStatusItem`) وتوزيعها في ملفات مستقلة تحت حزمة المكونات المشتركة.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم مطابقة أسطر الأرشفة المختلفة وصيغها البرمجية وحظر السمة الداكنة المكتوبة باليد داخل النشاط الرئيسي بالدليل والسطر.
* **Files Used / الملفات المستخدمة**:
  - [HabitStatusManager.kt](app/src/main/java/com/example/domain/usecase/HabitStatusManager.kt#L180-L189)
  - [HabitDetailViewModel.kt](app/src/main/java/com/example/presentation/screens/detail/HabitDetailViewModel.kt#L160-L176)
  - [MainActivity.kt](app/src/main/java/com/example/MainActivity.kt#L120-L130)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
