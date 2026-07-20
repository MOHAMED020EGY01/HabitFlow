# 17_PERFORMANCE — تقييم كفاءة وتدقيق الأداء / Performance Specification

## نقاط القوة الأدائية البرمجية / Verified Performance Strengths

تمت مراجعة وتحليل كود التطبيق المصدري، وتبين وجود ممارسات جيدة تدعم كفاءة الأداء:

1. **تهيئة قاعدة البيانات غير المحظورة**: تتم تهيئة قاعدة بيانات Room واستبناء UseCases بشكل غير متزامن داخل `applicationScope.launch(Dispatchers.IO)` أثناء بقاء شاشة الترحيب Splash ظاهرة، مما يضمن خلو الخيط الرئيسي (Main Thread) من عمليات الإدخال والإخراج الثقيلة عند تشغيل التطبيق.
2. **تحسين رسم القوائم في كومبوز**: يعتمد التطبيق في `HomeViewModel` على SnapshotStateList (`mutableStateListOf`) و SnapshotStateMap (`mutableStateMapOf`). يضمن هذا تحديث السطر أو الخلية التي نقر عليها المستخدم فقط دون إعادة رسم القائمة بأكملها.
3. **التحديث المهادن للقطع التفاعلية (Debounce)**: يضمن تأجيل تحديث قطع الشاشة بمهلة قدرها 3 ثوانٍ عدم استهلاك البطارية عند قيام المستخدم بنقرات سريعة متتالية.
4. **تجاوز بطء Glance**: يتم دفع التحديثات مباشرة لـ `AppWidgetManager` لتوليد الرسوم وتفادي تأخير Glance البالغ 45 ثانية.

---

## مشكلات وثغرات الأداء المكتشفة / Performance Issues & Bottlenecks

### 🔴 خطورة مرتفعة — عمليات كتابة قاعدة البيانات المتكررة داخل التكرار
* **الموقع**: دالة `performDailyRollover` في فئة [HabitStatusManager.kt](app/src/main/java/com/example/domain/usecase/HabitStatusManager.kt#L82-L87).
* **التفاصيل**: يتم استدعاء عملية التحديث `repository.updateHabit(currentHabit)` داخل كتلة حلقة التكرار `while (!tempDate.isAfter(yesterday))` التي تبحث وتكمل غيابات العادات المتوقفة مؤقتاً. إذا توقفت العادة لـ 30 يوماً متتالية، سيتم إجراء 30 عملية كتابة متتالية ومحجوبة لقاعدة بيانات SQLite على خيط المعالجة، مما يسبب بطء معالجة حاد وتجمد المعالج مؤقتاً.
* **الأثر**: تدهور شديد في الأداء وزيادة استهلاك الطاقة وقت معالجة الالتفاف الليلي للتشغيل الأول.
* **التوصية**: نقل استدعاء `updateHabit` خارج كتلة تكرار `while`؛ ليتم تعديل وتجميع حقول كائن العادة بالكامل في الذاكرة العشوائية أولاً، ومن ثم حفظ التحديث النهائي بضربة واحدة في قاعدة البيانات بعد انتهاء الدورة.

### 🟡 خطورة متوسطة — حظر الخيط الرئيسي بـ `runBlocking` وقت الإقلاع
* **الموقع**: فئة [HabitApplication.kt](app/src/main/java/com/example/HabitApplication.kt#L90-L92).
* **التفاصيل**: يتم استخدام `runBlocking` لقراءة اللغة المفضلة من تفضيلات DataStore Preferences بشكل متزامن على خيط المعالجة الرئيسي لتهيئة اتجاه الواجهات وتطبيق اللغة قبل بدء الشاشة الرسومية وتجنب الارتجاج المرئي وتأثير وميض اللغة الخاطئة (Wrong Language Flash).
* **الأثر**: حظر مؤقت للخيط الرئيسي عند فتح التطبيق.
* **التوصية**: يعتبر الحظر الحظي مقبولاً لتأمين اتساق اللغات والاتجاهات RTL، ولكن يجب مراقبة فترات الاستجابة وإبقاء SplashScreen ظاهرة باستخدام `setKeepOnScreenCondition` إذا طالت فترات القراءة.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم رصد كتل التحديث وحلقات التكرار داخل `HabitStatusManager` وتطبيق `runBlocking` وقت الإقلاع داخل `HabitApplication`.
* **Files Used / الملفات المستخدمة**:
  - [HabitStatusManager.kt](app/src/main/java/com/example/core/domain/usecase/HabitStatusManager.kt)
  - [HabitApplication.kt](app/src/main/java/com/example/app/HabitApplication.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
