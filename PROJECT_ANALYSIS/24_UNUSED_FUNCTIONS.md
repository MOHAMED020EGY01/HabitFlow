# 24_UNUSED_FUNCTIONS — الدوال البرمجية المهملة وغير المستعملة / Unused Functions

## قائمة الوظائف والدوال الزائدة / Unused & Legacy Functions

تم إجراء تدقيق واستعراض لجميع الملحقات والدوال المساعدة المحددة في فئات التطبيق المساعدة، وخلص التقرير الفني لتعيين الدوال التالية كإرث قديم (Legacy overloads) يمكن الاستغناء عنها وتوحيد استدعاءاتها:

A code audit of utility and helper classes identified several deprecated or legacy method overloads that remain in the codebase for compatibility but are bypassed by cleaner, parameter-driven equivalents:

### 1. دوال تنسيق التوقيت القديمة في `AppFormatters`
* **الموقع**: فئة [AppFormatters.kt](app/src/main/java/com/example/util/AppFormatters.kt#L83-L88).
* **الدوال**:
  * `fun formatTime(hour: Int, minute: Int, isArabic: Boolean): String`
  * `fun formatTime(time: LocalTime, isArabic: Boolean): String`
* **التفاصيل**: وظائف تنسيق تعتمد على المتغير الثنائي `isArabic` لتحديد لغة تنسيق الوقت وتأكيد الـ AM/PM باللغة المقابلة.
* **السبب**: تم استبدال استخداماتها وتعميمها بنظام تمرير رمز اللغة المباشر `langCode: String? = null` الذي يدعم المظاهر المتعددة والتهيئة المعيارية بشكل أنظف وموسع.
* **التوصية**: تحويل الاستدعاءات القليلة المتبقية في التقويم الموحد وحذفها لتبسيط الفئة.

### 2. دالة تنسيق التواريخ ذات كائن الـ Locale في `AppFormatters`
* **الموقع**: فئة [AppFormatters.kt](app/src/main/java/com/example/util/AppFormatters.kt#L104).
* **الدالة**:
  * `fun formatDate(date: LocalDate, pattern: String, locale: Locale): String`
* **التفاصيل**: دالة تقوم بتحويل التاريخ معتمدة على تمرير كائن Locale كامل من جافا.
* **السبب**: يتم توفير السياق وتحديد اللغات والاتجاهات RTL تلقائياً عبر فئة `LocaleDirectionHelper` وقنوات تمرير الـ `langCode` النصية الموحدة في التطبيق.
* **التوصية**: الحذف بعد تعديل السطر المتبقي في تقويم شاشة المتابعة.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم فحص وتتبع الاستدعاءات للدوال المعرفة في فئة `AppFormatters` وتحديد طرق التمرير المزدوجة بالسطر والدليل.
* **Files Used / الملفات المستخدمة**:
  - [AppFormatters.kt](app/src/main/java/com/example/util/AppFormatters.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
