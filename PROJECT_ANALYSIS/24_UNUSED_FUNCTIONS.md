# 24_UNUSED_FUNCTIONS — الدوال البرمجية المهملة وغير المستعملة / Unused Functions

## قائمة الوظائف والدوال الزائدة / Unused & Legacy Functions

تم إجراء تدقيق واستعراض لجميع الملحقات والدوال المساعدة، وخلص التقرير الفني لتعيين الدوال التالية كإرث قديم (Legacy overloads) يمكن الاستغناء عنها وتوحيد استدعاءاتها:

A code audit identified several legacy method overloads that remain in the codebase for compatibility but are bypassed by cleaner equivalents in most new features:

### 1. دوال تنسيق التوقيت القديمة في `AppFormatters`
* **الموقع**: فئة `AppFormatters.kt`.
* **الدوال**:
  * `fun formatTime(hour: Int, minute: Int, isArabic: Boolean)`
  * `fun formatTime(time: LocalTime, isArabic: Boolean)`
* **التفاصيل**: وظائف تنسيق تعتمد على المتغير الثنائي `isArabic` لتحديد لغة تنسيق الوقت.
* **السبب**: تم استبدال استخداماتها وتعميمها بنظام تمرير رمز اللغة المباشر `langCode` الذي يدعم المظاهر المتعددة.

### 2. دالة تنسيق التواريخ ذات كائن الـ Locale في `AppFormatters`
* **الموقع**: فئة `AppFormatters.kt`.
* **الدالة**:
  * `fun formatDate(date: LocalDate, pattern: String, locale: Locale)`
* **التفاصيل**: دالة تقوم بتحويل التاريخ معتمدة على تمرير كائن Locale كامل.
* **السبب**: يتم توفير السياق وتحديد اللغات تلقائياً عبر فئة `LocaleDirectionHelper`.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم فحص وتتبع الاستدعاءات للدوال المعرفة في فئة `AppFormatters` وتحديد طرق التمرير المزدوجة بالسطر والدليل.
* **Files Used / الملفات المستخدمة**:
  - [AppFormatters.kt](app/src/main/java/com/example/core/util/AppFormatters.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
