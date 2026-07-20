# 19_TECHNICAL_DEBT — الديون الفنية وهندسة التحديث / Technical Debt Audit

يوضح هذا المستند حصر الديون الفنية والانحرافات التنسيقية المكتشفة في كود تطبيق **HabitFlow**:

This document aggregates identified technical debt, design discrepancies, and code optimization tasks:

---

## 1. انحراف التصميم المعماري (Architectural Layer Bypass)
* **الموقع**: `HomeViewModel.kt`, `AllHabitsViewModel.kt`, `HabitDetailViewModel.kt`.
* **المشكلة**: تتصل نماذج العرض هذه بشكل مباشر مع مستودع البيانات `app.repository` لقراءة وتأكيد إكمال العادات وتخطي دور حالات الاستخدام (UseCases).
* **الأثر**: تفتت منطق العمل وتبعثر كود التحقق بعيداً عن حزمة `domain/usecase`.
* **التوصية**: بناء حالات استخدام معيارية مثل `GetActiveHabitsUseCase` و `ToggleLogCheckInUseCase`.

---

## 2. تعارض وتناقض تمثيل البيانات المكتوبة (Inconsistent Logs Snapshot Format)
* **الموقع**: `HabitStatusManager.kt` و `HabitDetailViewModel.kt`.
* **المشكلة**: يتم كتابة وحفظ أرشيف دورات العادات السابقة `logsSnapshot` في عمود نصي واحد بقاعدة البيانات بصيغتين مختلفتين تماماً:
  1. تقوم فئة `HabitStatusManager.kt` بحفظ البيانات كقائمة نصوص مقسمة بفواصل منقوطة: `date:state;date:state...`.
  2. بينما تقوم فئة `HabitDetailViewModel.kt` بحفظه بصيغة JSON نصي باستخدام مكتبة Gson.
* **الأثر**: يمنع هذا التناقض المطور من محاولة إعادة بناء شاشة لعرض السجل التاريخي للدورات السابقة بشكل موحد.

---

## 3. تجاهل وحظر رغبات مظاهر الشاشة (Hardcoded Dark Theme Override)
* **الموقع**: فئة `MainActivity.kt`.
* **المشكلة**: يمتلك التطبيق إعدادات تتيح للمستخدم تغيير مظهر الشاشة ولكن يتم حظر التفضيل وتطبيق مظهر داكن إجباري: `darkTheme = true`.
* **الأثر**: يتم إهمال وتجاهل تفضيل المستخدم الفعلي.

---

## 4. حجم ملفات الواجهات البرمجية الضخم (Monolithic Screen Files)
* **الموقع**: شاشة تفاصيل العادة `HabitDetailScreen.kt`.
* **المشكلة**: يحتوي ملف `HabitDetailScreen.kt` على ما يزيد عن **1115 سطر برمجي** تضم شاشات التقويم الدائري والرسوم البيانية وعناصر التحكم.
* **الأثر**: صعوبة القراءة وتحديث الأكواد وصعوبة صيانة الملف البرمجي.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم مطابقة أسطر الأرشفة المختلفة وصيغها البرمجية وحظر السمة الداكنة المكتوبة باليد داخل النشاط الرئيسي بالدليل والسطر.
* **Files Used / الملفات المستخدمة**:
  - [HabitStatusManager.kt](app/src/main/java/com/example/core/domain/usecase/HabitStatusManager.kt)
  - [HabitDetailViewModel.kt](app/src/main/java/com/example/feature/habit/presentation/HabitDetailViewModel.kt)
  - [MainActivity.kt](app/src/main/java/com/example/app/MainActivity.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
