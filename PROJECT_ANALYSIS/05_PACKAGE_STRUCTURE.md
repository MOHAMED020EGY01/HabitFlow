# 05_PACKAGE_STRUCTURE — تقسيم الحزم البرمجية / Subpackage Structure & Boundaries

## هيكل الحزم الفعلي / Actual Package Layout

تم تنظيم المشروع باتباع نمط "العمارة النظيفة المعتمدة على الميزات" (Feature-Based Clean Architecture). يتم تقسيم الكود إلى ثلاث حزم رئيسية تحت `com.example`:

The project is organized using a Feature-Based Clean Architecture pattern. The code is divided into three primary packages under `com.example`:

```text
com.example/
├── app/                 # طبقة التكامل والتشغيل / Integration & Startup Layer
│   ├── HabitApplication.kt   # حاوية DI والتهيئة / DI Container & Initialization
│   └── MainActivity.kt        # النشاط الرئيسي والتنقل / Main Activity & Navigation
│
├── core/                # النواة المشتركة / Shared Core
│   ├── audio/           # محركات الصوت و TTS / Audio Engines & TTS
│   ├── database/        # قاعدة بيانات Room والجداول / Room Database & DAOs
│   ├── datastore/       # إدارة التفضيلات / Preferences (DataStore)
│   ├── domain/          # منطق الأعمال وحالات الاستخدام المشتركة / Core Domain Logic
│   ├── infrastructure/  # الخدمات، العمال، والقطع التفاعلية / Services, Workers, Widgets
│   ├── model/           # نماذج البيانات والمحولات / Data Models & Mappers
│   ├── navigation/      # تعريفات المسارات والتحريكات / Navigation Routes & Animations
│   ├── repository/      # مستودعات البيانات / Repositories
│   ├── ui/              # المظهر والمكونات المشتركة / Theme & Shared UI
│   └── util/            # الأدوات المساعدة / Utility Classes
│
└── feature/             # ميزات التطبيق المستقلة / Independent Feature Slices
    ├── calendar/        # ميزة التقويم / Calendar Feature
    ├── habit/           # إدارة العادات / Habit Management
    ├── home/            # الشاشة الرئيسية / Home Dashboard
    ├── notifications/   # سجل الإشعارات / Notifications Log
    ├── onboarding/      # شاشات الترحيب / Onboarding Flow
    ├── settings/        # الإعدادات / Settings
    ├── splash/          # شاشة البداية / Splash Screen
    └── summary/         # ملخص الإحصائيات / Statistics Summary
```

---

## مسؤوليات الحزم والحدود البرمجية / Package Responsibilities & Boundaries

### 1. حزمة `app` (الرأس)
تعمل كغراء يربط كافة أجزاء النظام ببعضها.
* `HabitApplication` هي المسؤولة عن بناء الرسم البياني للاعتماديات (Dependency Graph) يدوياً وتوفيرها لكافة المكونات.
* `MainActivity` تستضيف `NavHost` الذي يربط الشاشات المختلفة ببعضها.

### 2. حزمة `core` (القلب)
توفر البنية التحتية والمنطق الذي تحتاجه كافة الميزات.
* **قاعدة البيانات (`database`)**: تعريف جداول العادات والسجلات.
* **البنية التحتية (`infrastructure`)**: تشمل `WorkManager` للتذكيرات و `Glance` للـ Widgets و `Foreground Services` للموثوقية.
* **المظهر (`ui`)**: تطبيق نمط "Glassmorphism" الموحد عبر المكونات المشتركة.

### 3. حزمة `feature` (الأطراف)
كل مجلد فرعي داخل `feature` يمثل وحدة وظيفية كاملة.
* تلتزم الميزة بمبدأ الاستقلالية، حيث لا تعتمد ميزة على أخرى بشكل مباشر.
* تعتمد الميزات على `core` للوصول إلى البيانات أو الأدوات المشتركة.
* تحتوي كل ميزة عادة على حزمة `presentation` للـ ViewModels والـ Composables.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - فحص شجرة الملفات الفعلي ومراجعة الـ Packages في كافة الملفات المصدرية.
* **Files Used / الملفات المستخدمة**:
  - [HabitApplication.kt](app/src/main/java/com/example/app/HabitApplication.kt)
  - [MainActivity.kt](app/src/main/java/com/example/app/MainActivity.kt)
  - `app/src/main/java/com/example/core/`
  - `app/src/main/java/com/example/feature/`
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
