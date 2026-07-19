# 05_RISKS

## المعالجة الثقيلة وحظر خيط المعالجة / Heavy Processing & Thread Blocking

### عمليات قاعدة البيانات المتكررة داخل التكرار
- **الملف والسطر (File & Line)**: [HabitStatusManager.kt:L82-87](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/domain/usecase/HabitStatusManager.kt#L82-L87)
- **الخطورة (Severity)**: متوسطة إلى مرتفعة (Medium/High)
- **المخاطرة (Risk)**: داخل جملة التكرار `while (!tempDate.isAfter(yesterday))`، يتم استدعاء عملية تحديث العادة في قاعدة البيانات `repository.updateHabit(currentHabit)` بشكل متكرر في كل دورة تكرار عند معالجة العادات غير النشطة لتحديث تواريخ انتهاء الدورة. إذا كانت العادة متوقفة لـ 15 أو 30 يوماً متتالياً، سينتج عن ذلك 15 إلى 30 عملية كتابة متتالية ومحجوبة في قاعدة بيانات SQLite المحلية، مما يعوق الأداء وقد يتسبب في قفل قاعدة البيانات بشكل مؤقت.
- **الحل المقترح (Suggested Fix)**: نقل استدعاء `repository.updateHabit(currentHabit)` إلى خارج كتلة تكرار `while`؛ بحيث يتم معالجة وتحديث كائن العادة بالكامل محلياً في الذاكرة العشوائية أولاً، ومن ثم حفظ النتيجة النهائية مرة واحدة في قاعدة البيانات بعد انتهاء الدورة البرمجية.

### Database Updates Inside Loop
- **File & Line**: [HabitStatusManager.kt:L82-87](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/domain/usecase/HabitStatusManager.kt#L82-L87)
- **Severity**: Medium/High
- **Risk**: Inside the `while` loop computing missing inactive days, `repository.updateHabit(currentHabit)` is invoked during every iteration to append days to the cycle end date. If a habit has been inactive for 30 consecutive days, this causes 30 sequential blocking SQLite database writes, choking I/O operations.
- **Suggested Fix**: Move the `updateHabit` write command out of the while loop block. Process all state modifications in memory on the habit object copy, and issue only a single consolidated database update call after the loop completes.

---

## مراجع غير مهيأة وانهيارات وقت التشغيل / Uninitialized References & Runtime Crashes

### مخاطر الوصول للمستودع قبل البناء غير المتزامن
- **الملف والسطر (File & Line)**: [DailyRolloverWorker.kt:L28-32](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/data/worker/DailyRolloverWorker.kt#L28-L32) و [HabitReminderWorker.kt:L108-110](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/data/worker/HabitReminderWorker.kt#L108-L110)
- **الخطورة (Severity)**: مرتفعة (High)
- **المخاطرة (Risk)**: تقوم فئة العمال `DailyRolloverWorker` و `HabitReminderWorker` بالوصول المباشر إلى خاصية `app.repository` للتحقق من العادات وتسجيل الإشعارات. ونظراً لأن تهيئة المستودع وقاعدة البيانات تتم بشكل غير متزامن داخل `HabitApplication.onCreate()` على خيط معالجة خلفي، فإنه في حال انطلاق العامل الخلفي بواسطة نظام التشغيل لحظة إقلاع التطبيق (أو كجزء من عملية إحياء للخدمات)، قد يتم محاولة القراءة قبل اكتمال التهيئة، مما يسبب استثناء وصول لمتغير غير مهيأ `UninitializedPropertyAccessException` وانهيار عملية الخلفية.
- **الحل المقترح (Suggested Fix)**: استدعاء دالة الانتظار المعرفة مسبقاً `app.ensureInitialized()` قبل أي محاولة استخدام لخصائص المستودع أو الاستخدامات داخل العمال الخلفيين للتأكد من جاهزية الاعتماديات قبل الاستدعاء.

### Race Conditions on Startup Background Workers
- **File & Line**: [DailyRolloverWorker.kt:L28-32](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/data/worker/DailyRolloverWorker.kt#L28-L32) and [HabitReminderWorker.kt:L108-110](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/data/worker/HabitReminderWorker.kt#L108-L110)
- **Severity**: High
- **Risk**: These workers access `app.repository` directly. Because repository configuration happens asynchronously inside `HabitApplication` in a background thread, if a worker is woken up by Android OS immediately on boot or after application termination, the dependency lookup will fail with an `UninitializedPropertyAccessException` crash.
- **Suggested Fix**: Prepend all worker database/repository calls with `app.ensureInitialized()`. This blocks or suspends the worker coroutine until dependencies are successfully populated.

---

## تسريبات الذاكرة وإدارة النطاقات / Memory Leaks & Scoping Issues

### تشغيل روتينات في النطاق العام غير الخاضع للرقابة
- **الملف والسطر (File & Line)**: [PendingOverlayReceiver.kt:L19-21](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/overlay/PendingOverlayReceiver.kt#L19-L21)
- **الخطورة (Severity)**: متوسطة (Medium)
- **المخاطرة (Risk)**: يستخدم الكود كتلة `@OptIn(DelicateCoroutinesApi::class) GlobalScope.launch` لإجراء تحديث على قاعدة البيانات وإطلاق خدمة التذكير العائم فور فك قفل الجهاز. النطاق `GlobalScope` لا يرتبط بدورة حياة المكون ولا يمكن إلغاؤه، مما يعني أنه في حال حدوث بطء في الوصول لقاعدة البيانات أو استجابة المعالجة، فقد تظل هذه الروتينات معلقة وتسبب تسرباً للذاكرة العشوائية لعدم ارتباطها ببيئة عمل محكومة.
- **الحل المقترح (Suggested Fix)**: استبدال `GlobalScope` بالنطاق العالمي المعرّف للتطبيق والمحمي بـ SupervisorJob: `app.applicationScope.launch` لضمان إدارة الروتينات المتزامنة بشكل سليم أو استخدام معالج البث المخصص `goAsync()` المتوفر لمستقبل البث.

### GlobalScope Usage in Broadcast Receivers
- **File & Line**: [PendingOverlayReceiver.kt:L19-21](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/overlay/PendingOverlayReceiver.kt#L19-L21)
- **Severity**: Medium
- **Risk**: Employs `@OptIn(DelicateCoroutinesApi::class) GlobalScope.launch` to read preferences and launch services. Coroutines fired in `GlobalScope` escape traditional cancellation mechanisms, potentially causing memory leaks or orphan processes if database reads freeze or take longer than the receiver's execution window.
- **Suggested Fix**: Transition from `GlobalScope` to `app.applicationScope` (the supervisor-controlled scope built in `HabitApplication.kt`), ensuring operations run safely and cleanly.

---

## جودة وموثوقية منطق العمل / Code Quality & Logic Flaws

### قصور الإيقاف التلقائي للعادات المخصصة
- **الملف والسطر (File & Line)**: [HabitStatusManager.kt:L114-116](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/domain/usecase/HabitStatusManager.kt#L114-L116)
- **الخطورة (Severity)**: منخفضة إلى متوسطة (Low/Medium)
- **المخاطرة (Risk)**: يقوم منطق التوقيف التلقائي للعادات `checkAndAutoPause` بالتحقق من الأيام الثلاثة التقويمية الفائتة للعادة (`today.minusDays(i)`). ومع ذلك، في العادات التي لا تعمل كل يوم (مثال: عادة تجري يومي الإثنين والجمعة فقط)، لا يتم تسجيل سجل غياب `"MISS"` في الأيام غير المجدولة (مثل الثلاثاء والأربعاء). وبالتالي، لن تجد الدالة سجلات غياب متتالية لثلاثة أيام تقويمية متتالية أبداً، مما يجعل ميزة الإيقاف التلقائي معطلة تماماً لهذه العادات.
- **الحل المقترح (Suggested Fix)**: تعديل عملية التحقق البرمجية لتبحث في آخر 3 أيام مجدولة ونشطة فعلياً للعادة، بدلاً من الأيام التقويمية الثلاثة المتتالية في نظام التقويم العام.

### Broken Auto-Pause Logic on Scheduled Habits
- **File & Line**: [HabitStatusManager.kt:L114-116](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/domain/usecase/HabitStatusManager.kt#L114-L116)
- **Severity**: Low/Medium
- **Risk**: The auto-pause feature pauses habits that accumulate 3 consecutive miss logs using calendar days lookbacks (`today.minusDays(1/2/3)`). For habits running on specialized schedules (e.g. only Mondays), non-active days have no logs generated. Hence, `lastThreeMiss` will always fail to detect 3 consecutive misses, disabling the auto-pause function for all non-daily habits.
- **Suggested Fix**: Adjust the lookback algorithm to scan the last 3 *active* scheduled days for that habit rather than arbitrary consecutive calendar dates.
