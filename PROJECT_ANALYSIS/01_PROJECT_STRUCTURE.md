# 01_PROJECT_STRUCTURE — هيكل المجلدات والمشروع / Project Structure

## هيكل مجلدات المشروع / Directory Tree Map

هيكل مجلدات المشروع الفعلي المعتمد على الميزات (Feature-Based) ونمط "العمارة النظيفة":

The structural map of the project's source code following the Feature-Based Clean Architecture pattern:

```text
habitflow/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml (المانيفست المركزي / Main Manifest)
│   │   │   ├── java/com/example/
│   │   │   │   ├── app/ (نقطة التكامل / Integration Layer)
│   │   │   │   │   ├── HabitApplication.kt (حاوية DI والتهيئة / DI Container & Init)
│   │   │   │   │   └── MainActivity.kt (النشاط الرئيسي والتنقل / Root Activity & Nav)
│   │   │   │   ├── core/ (البنية التحتية المشتركة / Shared Infrastructure)
│   │   │   │   │   ├── audio/ (محركات الصوت و TTS / Audio & TTS Engines)
│   │   │   │   │   ├── database/ (إعداد Room والجداول / Room DB & DAOs)
│   │   │   │   │   ├── datastore/ (إدارة التفضيلات / Preferences DataStore)
│   │   │   │   │   ├── domain/ (منطق الأعمال المشترك / Shared Domain Logic)
│   │   │   │   │   ├── infrastructure/ (العمال والخدمات والويدجت / Workers, Services, Widgets)
│   │   │   │   │   ├── model/ (النماذج والمحولات / Models & Mappers)
│   │   │   │   │   ├── navigation/ (تعريفات المسارات / Navigation Routes)
│   │   │   │   │   ├── repository/ (مستودعات البيانات / Data Repositories)
│   │   │   │   │   ├── ui/ (مكونات الواجهة والمظهر / Shared UI & Theme)
│   │   │   │   │   └── util/ (أدوات المساعدة العامة / General Utilities)
│   │   │   │   └── feature/ (شرائح الميزات الرأسية / Vertical Feature Slices)
│   │   │   │       ├── habit/ (إدارة العادات / Habit Management)
│   │   │   │       ├── home/ (لوحة التحكم / Dashboard)
│   │   │   │       ├── summary/ (التحليلات / Analytics)
│   │   │   │       ├── calendar/ (التقويم / Calendar)
│   │   │   │       ├── notifications/ (سجل التنبيهات / Notification Log)
│   │   │   │       ├── settings/ (الإعدادات / Settings)
│   │   │   │       ├── onboarding/ (الترحيب / Onboarding)
│   │   │   │       └── splash/ (شاشة التحميل / Splash)
│   │   │   └── res/ (الموارد المترجمة والصور / Resources & Assets)
│   │   └── test/ (اختبارات الوحدات / Unit Tests)
│   ├── build.gradle.kts (إعداد بناء الوحدة / App Module Gradle)
│   └── proguard-rules.pro (قواعد الحماية والضغط / Proguard Rules)
├── gradle/
│   └── libs.versions.toml (فهرس الإصدارات / Version Catalog)
├── build.gradle.kts (بناء المشروع الرئيسي / Root Gradle)
└── settings.gradle.kts (إعدادات المشروع / Settings Gradle)
```

---

## مسؤوليات الحزم الرئيسية / Package Responsibilities

* **`com.example.app`**:
  * تنسيق تشغيل الميزات وتهيئة حاوية حقن الاعتماديات اليدوية.
  * إدارة الرسم البياني للتنقل (NavHost) في `MainActivity`.

* **`com.example.core`**:
  * توفير الأدوات والبنية التحتية التي تعتمد عليها كافة الميزات.
  * إدارة التخزين المركزي (Room, DataStore) والتواصل مع النظام (Workers, Services).
  * توفير نظام التصميم الموحد (Theme, GlassCard).

* **`com.example.feature.<name>`**:
  * كبسولة برمجية مستقلة تحتوي على واجهات العرض (Presentation) ومنطق الميزة الخاص.
  * تلتزم بعدم الاعتماد على ميزات أخرى بشكل مباشر، بل تعتمد على `core`.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - فحص شجرة المجلدات الفعلي ومحتوى الملفات البرمجية.
* **Files Used / الملفات المستخدمة**:
  - `app/src/main/java/com/example/`
  - [HabitApplication.kt](app/src/main/java/com/example/app/HabitApplication.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
