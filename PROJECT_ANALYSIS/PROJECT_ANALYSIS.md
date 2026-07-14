# PROJECT_ANALYSIS — فهرس التوثيق الشامل / Master Documentation Index

## نظرة عامة على التوثيق / Documentation Overview

هذا الملف يعمل كمرجع مركزي للوصول لكافة وثائق تحليل مشروع HabitFlow المنظمة.
This file serves as the central reference index for all organized HabitFlow project analysis documents.

---

## فهرس الوثائق / Document Index

| الوثيقة / Document | الحجم (سطراً) | المحتوى / Contents |
| :--- | :---: | :--- |
| [01_STRUCTURE.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/01_STRUCTURE.md) | 126 | هيكل المجلدات، النمط المعماري، نقاط الدخول، مخطط الاعتماديات |
| [02_LIBRARIES.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/02_LIBRARIES.md) | 146 | جرد المكتبات، الإصدارات، المستخدمة وغير المستخدمة |
| [03_CODING_CONVENTIONS.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/03_CODING_CONVENTIONS.md) | 132 | قواعد التسمية، إدارة الحالة، التعريب، الويدجت |
| [04_CLASSES_FUNCTIONS.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/04_CLASSES_FUNCTIONS.md) | ~170 | جرد الفئات، ViewModels، UseCases، الأكواد الميتة، الملفات الضخمة |
| [05_RISKS.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/05_RISKS.md) | ~100 | مخاطر الأداء، الانهيارات المحتملة، تسريبات الذاكرة، عيوب منطق العمل |
| [ARCHITECTURE.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/ARCHITECTURE.md) | ~130 | الطبقات المعمارية الأربع، مخطط تدفق البيانات، الانحراف المعماري |
| [DATABASE.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/DATABASE.md) | ~110 | مخطط الجداول، حقول DataStore، تاريخ الهجرات، الإعدادات |
| [PERFORMANCE_AUDIT.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/PERFORMANCE_AUDIT.md) | ~100 | نقاط القوة، المشكلات الموثقة بمستوى الخطورة، توصيات الأولويات |
| [TECH_STACK.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/TECH_STACK.md) | ~40 | جدول التقنيات الكامل |

---

## ملخص سريع للمشروع / Quick Project Summary

### المنتج / Product
- **النوع**: تطبيق أندرويد لتتبع العادات (Local-First / Offline).
- **المنصة**: Android API 24+ (Nougat وما فوق).
- **اللغات المدعومة**: العربية (RTL) والإنجليزية.
- **وضع التطوير**: إنتاج قيد التحسين المستمر.

### الأرقام الرئيسية / Key Numbers
| المقياس / Metric | القيمة / Value |
| :--- | :--- |
| ملفات Kotlin في قاعدة الكود | ~80+ ملف |
| إصدار قاعدة البيانات | 10 |
| الحد الأقصى للعادات النشطة | 6 عادات |
| أكبر ملف | `HabitDetailScreen.kt` (1115 سطراً) |
| عدد شاشات التطبيق | 10 شاشات |
| عدد حالات الاستخدام (UseCases) | 9 |
| عدد نماذج العرض (ViewModels) | 9 |
| عدد العمال الخلفيين (Workers) | 4 |
| عدد الخدمات الأمامية | 2 |
| عدد مستقبلات البث | 3 |
| عدد الويدجت | 2 |

---

## خارطة الكود لأهم السيناريوهات / Scenario Code Map

### إضافة عادة جديدة (Complete Flow)
```
AddHabitScreen → AddHabitViewModel → AddHabitUseCase (التحقق من الحد الأقصى)
     → ValidateReminderTimeUseCase (التحقق من التعارض)
          → HabitRepository.addHabit()
               → HabitDao.insertHabit()
                    → HabitReminderWorker (جدولة التذكير)
                         → AllHabitsWidget (تحديث الويدجت)
```

### تشغيل التذكير العائم (Overlay Reminder Flow)
```
HabitReminderWorker → HabitOverlayWorker → HabitOverlayReceiver
     → HabitOverlayService (عرض النافذة العائمة)
          → User Completes ← HabitOverlayAction broadcast
               → HabitRepository.toggleLog() → Widget Update
```

### الالتفاف الليلي (Nightly Rollover Flow)
```
DailyRolloverWorker (منتصف الليل)
     → HabitStatusManager.processAllHabits()
          → تسجيل الغيابات للأمس → التحقق من الإيقاف التلقائي (3 غيابات)
               → أرشفة الدورات المنتهية → تحديث الويدجت
```

---

## حالة التوثيق / Documentation Status

> [!NOTE]
> تم إنشاء وتحديث جميع وثائق التحليل خلال الفترة **يوليو 2026** استناداً للكود المصدري الفعلي للمشروع، وليس للافتراضات.

> [!TIP]
> عند إجراء تعديلات جوهرية في المشروع، يُوصى بتحديث الوثائق المتأثرة مباشرة لضمان دقة المرجع.

All analysis documents were generated in **July 2026** based on evidence from the actual source code, not assumptions.
When making significant changes to the project, please update the relevant analysis documents to maintain reference accuracy.
