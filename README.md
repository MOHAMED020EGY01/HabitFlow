# HabitFlow

## نبذة عن المشروع / Overview

### ما هو تطبيق HabitFlow؟
تطبيق **HabitFlow** هو تطبيق أندرويد متكامل لتتبع العادات وبنائها محلياً (Offline-First)، تم بناؤه باستخدام لغة Kotlin وواجهات Jetpack Compose التفاعلية. يركز التطبيق على تزويد المستخدمين بتجربة تفاعلية وراقية تعتمد على التصاميم الزجاجية (Glassmorphism) والمؤثرات البصرية الديناميكية، لمساعدتهم في تتبع إنجازاتهم اليومية، وجدولة التذكيرات الصوتية العائمة، ومزامنة قطع الواجهة التفاعلية (Widgets) على الشاشة الرئيسية، مع دعم كامل للغتين العربية والإنجليزية واتجاهات النصوص التلقائية (RTL).

### What is HabitFlow?
**HabitFlow** is a premium, local-first Android habit-tracking application built with Kotlin and Jetpack Compose. Designed with rich glassmorphism aesthetics and smooth animations, it empowers users to build and maintain habits. The application features customizable daily schedules, reliable floating overlay audio alarms, dynamic home screen Glance widgets, and robust support for both Arabic and English locales (with automatic RTL mirroring).

---

## الميزات الرئيسية / Key Features

### ميزات التطبيق الفعلية
- **لوحة التحكم الرئيسية (Home Dashboard)**: تعرض بطاقات زجاجية تفاعلية للعادات النشطة المجدولة اليوم مع حلقات التقدم الدائرية ونسب الإنجاز ونظام الإكمال السريع بنقرة واحدة.
- **إضافة وتعديل العادات (Add/Edit Habits)**: يدعم جدولة مخصصة للأيام النشطة في الأسبوع، اختيار الألوان السداسية، وتعيين مواعيد تنبيه متعددة بفاصل زمني آمن لمنع تداخل التنبيهات.
- **النافذة العائمة الذكية (Interactive Overlay Alarms)**: نافذة منبثقة تفاعلية تظهر فوق التطبيقات الأخرى عند موعد التذكير مع منبه صوتي متكرر، وتدعم التخصيص والجر الحر على الشاشة.
- **أدوات الشاشة الرئيسية (Home Widgets)**: قطعتان تفاعليتان مبنيتان بـ Glance: قطعة لعرض العادات النشطة ومستوى الإنجاز بنقرة إكمال سريعة، وقطعة أخرى تعرض العادات المتوقفة وتواريخ ومدة التوقف.
- **تقويم الإنجاز (Calendar Grid)**: شاشة تقويم شهري تفاعلية تعرض حالة الالتزام اليومي بنقاط ملونة تدل على الإنجاز أو الغياب.
- **سجل التاريخ والإيقاف المؤقت (Cycles & Auto-Pause)**: آلية تقوم بإيقاف العادات تلقائياً عند غياب 3 أيام متتالية، مع أرشفة تاريخ الدورات المكتملة وحساب نسب النجاح.
- **موثوقية الخلفية (Background Reliability)**: خدمة أمامية Keepalive تعيد تشغيل التذكيرات وتطلق التنبيهات الفائتة فور فتح قفل الجهاز.

### Features Found in the Codebase
- **Home Dashboard**: Modern workspace displaying active habits scheduled for today, featuring custom progress rings, streaks, and one-tap log checking.
- **Custom Habit Editor**: Allows scheduling specific days of the week, picking hex colors, and registering multiple reminder times with a safe 10-minute gap validator.
- **Floating Overlays (Overlay Alarms)**: Implements interactive popup dialogs drawn directly onto WindowManager. Includes looping alert audio, drag-to-reposition physics, and instant check-ins.
- **Interactive Glance Widgets**: Two widgets: One for active habits (with instant remote view updates) and another presenting paused habits alongside stopped dates and duration logs.
- **Calendar History**: Monthly grid layout visualizer indicating daily completion rates using custom color-coded indicators.
- **Cycle Management**: Automatically pauses habits after 3 consecutive missed days, logs completed durations as historical cycle items, and tracks completion rates.
- **Exemptions & Reliability**: Foreground services and workers ensuring alarms fire reliably, with unlock broadcast receivers processing catch-ups.

---

## التقنيات المستخدمة / Tech Stack

### جدول المواصفات التقنية
| التقنية / Tech | الإصدار / Version | الاستخدام / Purpose |
| :--- | :--- | :--- |
| **Android compileSdk** | 36 | استهداف أحدث واجهات برمجية لنظام أندرويد / Target OS SDK |
| **Android minSdk** | 24 | الحد الأدنى لتشغيل التطبيق (Nougat+) / Minimum compatible SDK |
| **Kotlin** | 2.2.10 | لغة البرمجة الأساسية / Primary Programming Language |
| **Jetpack Compose** | BOM 2024.09.00 | محرك بناء الواجهات الرسومية التفاعلية / Declarative UI Engine |
| **Room Database** | 2.7.0 | قاعدة البيانات المحلية وإدارة الجداول / SQLite ORM Layer |
| **Preferences DataStore** | 1.1.7 | حفظ التفضيلات وخيارات المظهر واللغات / Settings Storage |
| **WorkManager** | 2.9.0 | جدولة العمال والمهام الخلفية / Background Scheduler |
| **Glance Material 3** | 1.1.0 | بناء قطع الشاشة الرئيسية التفاعلية / App Widgets Framework |
| **Coil** | 2.7.0 | تحميل الصور ومعالجة الرموز / Asynchronous Image Loader |
| **LeakCanary** | 2.14 | كشف تسربات الذاكرة في بيئة التطوير / Debug Memory Audit |

---

## البنية البرمجية والطبقات / Architecture & Module Structure

### نظرة سريعة على بنية المشروع
يتبع المشروع نمط **Clean Architecture** و **MVVM** لتقسيم الكود إلى طبقات معزولة:
- **`presentation/`**: واجهات الكومبوز (Screens/Components) والتحريكات ونماذج العرض (ViewModels) التي تراقب وتبث الحالة للواجهات.
- **`domain/`**: الكود النقي والتجاري المستقل الذي يعرّف كائنات العادات (`Habit`) وواجهات المستودعات وحالات الاستخدام مثل الفرز والتحقق.
- **`data/`**: تنظيم قواعد بيانات Room وجداول الكيانات والعمال الخلفيين وإعدادات DataStore.

> [!NOTE]
> لمزيد من التفاصيل حول اتجاه البنية وانحرافات الطبقات الفعلية، يرجى مراجعة [دليل الهيكل والمجلدات](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/01_STRUCTURE.md).

### Directory & Architecture Overview
The application structure is split into logical modules keeping frameworks decoupled:
- **Presentation**: Jetpack Compose screen layouts, premium modular UI components, and ViewModels.
- **Domain**: Pure Kotlin logic. Holds models like `Habit` and use cases managing business validation.
- **Data**: Houses database schemas, DAO transaction operations, preferences repositories, and background workers.

> [!NOTE]
> For a deep dive into layers and direct database call pathways, check out [01_STRUCTURE.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/01_STRUCTURE.md).

---

## متطلبات التشغيل والتهيئة / Prerequisites & Setup

### متطلبات البدء بالتطوير
1. **برنامج التطوير**: أندرويد ستوديو (Android Studio - الإصدار الأخير مفضل).
2. **بيئة الجافا**: إصدار JDK 11 أو أعلى.
3. **الجهاز**: محاكي أندرويد أو هاتف حقيقي بنظام Android 7.0 (API 24) أو أعلى.

### خطوات بناء المشروع تشغيلياً
1. قم بفتح مجلد المشروع الرئيسي داخل Android Studio.
2. انتظر اكتمال فحص ومزامنة ملفات Gradle وتحميل الاعتماديات.
3. قم بإنشاء ملف `.env` في جذر المشروع يحتوي على مفاتيح التطبيق وسرية التوقيع (يمكنك الاستعانة بـ `.env.example`).
4. قم بربط جهاز الاختبار واضغط على زر التشغيل `Run app` للبناء والتثبيت.

### Building the Project
1. Open the project root folder in **Android Studio**.
2. Allow Gradle sync to run and download all version catalog dependencies.
3. Create a local `.env` configuration file in the project root if signing keys or build config variables are required.
4. Set up an emulator or physical device running API 24+, and press the Run button.

---

## ملاحظات المطورين والتطوير / Development Notes

### دليل المساهمة وفهم الكود
للمطورين الجدد الذين يعملون على هذا المشروع، تم توثيق تفاصيل الهيكل والمكتبات واتجاهات التطبيق بالتفصيل في مجلد التحليل لتجنب تعارض الكود:
- **المكتبات والإصدارات**: راجع [02_LIBRARIES.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/02_LIBRARIES.md) للتعرف على استخدامات ومواقع كل مكتبة.
- **القواعد البرمجية (Conventions)**: اطلع على [03_CODING_CONVENTIONS.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/03_CODING_CONVENTIONS.md) لفهم كود إدارة الحالة، التعريب، وصياغة الويدجت.
- **الفئات والأكواد الميتة**: لمعرفة الفئات الكبيرة والأكواد المرشحة للحذف، راجع [04_CLASSES_FUNCTIONS.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/04_CLASSES_FUNCTIONS.md).
- **المخاطر والعيوب الحالية**: قائمة بالثغرات البرمجية والانهيارات المحتملة متوفرة في [05_RISKS.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/05_RISKS.md).

### Contributors Resource Map
Before making any layout modifications or logic changes, please read our structural analysis guides under `PROJECT_ANALYSIS/`:
- **Libraries & Scope**: See [02_LIBRARIES.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/02_LIBRARIES.md) for libraries uses and target packages.
- **Styling & State Rules**: Check [03_CODING_CONVENTIONS.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/03_CODING_CONVENTIONS.md) for Compose states, RTL logic, and widget direct updates.
- **Refactoring & Lines Index**: Visit [04_CLASSES_FUNCTIONS.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/04_CLASSES_FUNCTIONS.md) to locate dead logic and line outliers.
- **Known Code Vulnerabilities**: Read [05_RISKS.md](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20(19)/PROJECT_ANALYSIS/05_RISKS.md) to inspect uninitialized property races and auto-pause bugs.
