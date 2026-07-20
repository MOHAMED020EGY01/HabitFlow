# 30_CHANGELOG_ANALYSIS — سجل التغييرات التاريخية للبناء / Changelog & Evolution

This document tracks the technical evolution, database additions, and feature implementations of the **HabitFlow** codebase:

---

## سجل هجرات وتحديثات قاعدة البيانات / Database Schema Changelog

| نسخة قاعدة البيانات / Version | التحديثات المضافة / Changes & Additions |
| :---: | :--- |
| **v1** | تأسيس الجداول الأولية لـ `habits` و `habit_logs`. |
| **v2** | إضافة حقول الحالة وتواريخ الدورة وجدول أرشيف الدورات السابقة. |
| **v3** | إضافة فهارس البحث وتفعيل قيود المفاتيح الخارجية. |
| **v4** | إضافة فهرس بحث مخصص لحقل العادات النشطة وتاريخ الإنشاء. |
| **v5** | تعديل صيغة تخزين أوقات التنبيهات. |
| **v6** | إضافة حقل أيام العمل الأسبوعية النشطة `activeDays`. |
| **v7** | إضافة فهرس بحث مخصص لحقل `startedAt`. |
| **v8** | تأسيس جدول الإشعارات `notifications`. |
| **v9** | إضافة حقل آخر وقت للتوقف المؤقت `inactiveSinceTimestamp`. |
| **v10** | إضافة حقل صوت المنبه المختار `reminderVoice`. |
| **v11 & v12** | تحديث وتحديث هاش الهوية. |

---

## تطور الميزات والمنصة البرمجية / Platform & Feature Evolution

* **Phase 1**: تأسيس واجهات Jetpack Compose ونمط Clean Architecture.
* **Phase 2**: إدراج `HabitOverlayService` وتوفير نطق النصوص المخصص.
* **Phase 3**: إدراج Glance Widgets وتأسيس عمال المزامنة والتحديث المباشر.
* **Phase 4**: إضافة أنواع الخدمات الأمامية المحددة `specialUse` لدعم أندرويد 14+.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**: فحص هياكل الهجرات وتحديث حقول الكائنات البرمجية.
* **Files Used / الملفات المستخدمة**:
  - [HabitDatabase.kt](app/src/main/java/com/example/core/database/HabitDatabase.kt)
  - [AndroidManifest.xml](app/src/main/AndroidManifest.xml)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
