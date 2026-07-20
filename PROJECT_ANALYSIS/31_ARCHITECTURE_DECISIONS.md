# 31_ARCHITECTURE_DECISIONS — سجل القرارات الهندسية للمشروع / Architectural Decisions (ADR)

This section documents the Architectural Decision Records (ADRs) detailing the key technical choices made for **HabitFlow**:

---

## ADR 1: العمل دون اتصال كمنهج أساسي (Offline-First local storage)
* **Decision**: استخدام قاعدة بيانات Room Persistence (SQLite) لحفظ كافة بيانات المستخدم محلياً.

---

## ADR 2: واجهة المظهر الزجاجي بـ Jetpack Compose
* **Decision**: بناء الواجهات والتحريكات بالكامل باستخدام Jetpack Compose.

---

## ADR 3: حقن الاعتماديات اليدوي (Manual Dependency Injection)
* **Decision**: تجنب استخدام Dagger/Hilt وتأسيس حاوية حقن يدوية داخل `HabitApplication`.

---

## ADR 4: التحديث الفوري لقطع الشاشة (Glance Widget Direct Update)
* **Decision**: تجاوز نظام التحديث الدوري لـ Glance والاستعانة بـ `WidgetDirectUpdater`.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**: مطابقة مبررات التقنيات المحددة مع الهيكل العام المكتوب للمشروع.
* **Files Used / الملفات المستخدمة**:
  - [HabitDatabase.kt](app/src/main/java/com/example/core/database/HabitDatabase.kt)
  - [HabitApplication.kt](app/src/main/java/com/example/app/HabitApplication.kt)
  - [WidgetDirectUpdater.kt](app/src/main/java/com/example/core/infrastructure/widget/WidgetDirectUpdater.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
