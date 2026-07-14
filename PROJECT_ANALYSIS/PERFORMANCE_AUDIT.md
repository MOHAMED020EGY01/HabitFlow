# PERFORMANCE_AUDIT

## تدقيق الأداء / Performance Audit

### ملخص الحالة العامة للأداء
يُظهر التطبيق بشكل عام مراعاة واعية للأداء في تصميمه الأساسي، مع وجود بعض النقاط التي تتطلب مراقبة دورية. فيما يلي تحليل تفصيلي للمخاوف الحقيقية الموثقة من الكود المصدري.

### Performance Profile Summary
The application demonstrates generally performance-conscious design patterns, with several areas requiring ongoing monitoring. This document covers real code-evidenced findings.

---

## نقاط القوة الأدائية / Performance Strengths

### ما يعمل جيداً حالياً
1. **تهيئة قاعدة البيانات اللامتزامنة**: تتم داخل `applicationScope` لمنع تجميد شاشة البداية.
2. **مجموعات الحالة التفاعلية الجزئية**: استخدام `mutableStateListOf` و`mutableStateMapOf` في `HomeViewModel` يحد إعادة الرسم على العنصر المُعدَّل فقط بدلاً من إعادة رسم القائمة بأكملها.
3. **نافذة المعالجة المحدودة**: يقتصر الالتفاف اليومي (`DailyRolloverWorker`) على آخر 30 يوماً كحد أقصى لتجنب تحميل المعالج زائداً.
4. **التحديث المهادن للقطع (Debounce)**: `HabitWidgetSyncUpdater` يطبق مهلة 3 ثوانٍ لتجميع التحديثات المتسارعة في عملية رسم واحدة.
5. **التحديث المباشر للقطع (Direct Update)**: `WidgetDirectUpdater.pushDirectUpdate` يتجاوز آلية Glance البطيئة للتحديث ويدفع التغييرات مباشرة لـ `AppWidgetManager`.
6. **نمط WAL لقاعدة البيانات**: يُقلل فترات قفل الجداول ويُحسن التزامن في قراءة البيانات وكتابتها.

### Verified Strengths
1. **Async DB Init**: Initialization is wrapped in `applicationScope.launch` keeping the main thread free during startup.
2. **Granular Compose State**: `mutableStateListOf`/`mutableStateMapOf` enables row-level recomposition in lists.
3. **30-Day Processing Window**: Daily rollover capped at 30 lookback days, preventing unbounded CPU work.
4. **Widget Sync Debouncing**: 3-second debounce merges rapid user interactions into a single widget refresh.
5. **Direct Widget Push**: Bypasses Glance's 45-second session lock for immediate UI feedback.
6. **WAL Database Mode**: Reduces table lock contention during simultaneous reads and writes.

---

## مشكلات الأداء الموثقة / Documented Performance Issues

### 🔴 خطورة مرتفعة — كتابة متكررة لقاعدة البيانات داخل حلقة تكرار
- **الملف**: [HabitStatusManager.kt](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/domain/usecase/HabitStatusManager.kt)
- **السطور**: L82–87
- **التفاصيل**: يتم استدعاء `repository.updateHabit()` داخل جملة `while` في دالة معالجة الغيابات. عادة متوقفة لـ 30 يوماً ستُنتج 30 عملية كتابة متتالية محظورة في SQLite، مما قد يُعطل خيط IO ويُبطئ النظام.
- **الأثر**: تدهور ملحوظ في الأداء عند وجود عادات متوقفة لفترات طويلة.
- **التوصية**: تجميع التعديلات محلياً ثم إصدار استدعاء كتابة موحد واحد خارج حلقة التكرار.

### 🟡 خطورة متوسطة — كوروتينات `GlobalScope` في مستقبل البث
- **الملف**: [PendingOverlayReceiver.kt](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/overlay/PendingOverlayReceiver.kt)
- **السطور**: L19–21
- **التفاصيل**: استخدام `GlobalScope.launch` يعني أن الكوروتينات غير مرتبطة بدورة حياة أي مكون، مما يُصعب إلغاءها في حال حدوث خطأ أو بطء في قاعدة البيانات.
- **الأثر**: خطر تسرب ذاكرة محدود لكن حقيقي.
- **التوصية**: الانتقال إلى `app.applicationScope.launch`.

### 🟡 خطورة متوسطة — استدعاء `runBlocking` في بدء التشغيل
- **الملف**: [HabitApplication.kt](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/HabitApplication.kt)
- **التفاصيل**: يتم تنفيذ `runBlocking` لقراءة اللغة المختارة من DataStore وتطبيق التعريب قبل رسم الواجهة. هذا ضروري لتجنب ارتجاج اللغة (Flash of Wrong Language) لكن يحظر الخيط الرئيسي مؤقتاً.
- **التوصية**: يُبقى كما هو مع مراقبة وقت الإقلاع في نسخ الإصدار، وتحسينه عبر `SplashScreen.setKeepOnScreenCondition` إذا احتاج للتمديد.

### 🟢 خطورة منخفضة — ويدجت Glance ذات إعادة رسم ثقيلة
- **الملف**: [AllHabitsWidget.kt](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/widget/AllHabitsWidget.kt)
- **التفاصيل**: تحتوي القطعة على رسم عشوائي مخصص للحلقات والخلفيات الزجاجية. على الأجهزة المنخفضة المواصفات قد تُعاني من تأخر في الرسم الأول.
- **التوصية**: اختبار الأداء على أجهزة بذاكرة RAM منخفضة (2GB) وإضافة حالة تحميل مؤقتة إذا تجاوز التأخر 300ms.

---

### Documented Performance Issues

#### 🔴 High — Repeated DB Writes in Loop
- **File**: [HabitStatusManager.kt](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/domain/usecase/HabitStatusManager.kt)
- **Lines**: L82–87
- **Details**: `repository.updateHabit()` called inside a `while` loop when processing inactive habits. A 30-day paused habit generates 30 blocking SQLite write calls.
- **Impact**: Measurable IO thread congestion for long-paused habits.
- **Fix**: Consolidate modifications in memory; issue a single write after the loop.

#### 🟡 Medium — GlobalScope Coroutines in Broadcast Receiver
- **File**: [PendingOverlayReceiver.kt](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/overlay/PendingOverlayReceiver.kt)
- **Details**: Unscoped `GlobalScope.launch` creates un-cancellable coroutines.
- **Fix**: Use `app.applicationScope.launch` instead.

#### 🟡 Medium — `runBlocking` on Main Thread at Startup
- **File**: [HabitApplication.kt](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/HabitApplication.kt)
- **Details**: `runBlocking` reads language preferences before UI draws. Necessary for RTL correctness but briefly blocks main thread.
- **Fix**: Retain but monitor startup duration. Use `SplashScreen.setKeepOnScreenCondition` if startup needs more time.

#### 🟢 Low — Heavy Glance Widget Rendering
- **File**: [AllHabitsWidget.kt](file:///c:/Users/mohamed/Desktop/TESt/habitflow%20%2819%29/app/src/main/java/com/example/widget/AllHabitsWidget.kt)
- **Details**: Custom canvas-drawn progress rings and glass effects may slow initial renders on low-RAM devices.
- **Fix**: Test on 2GB RAM devices and add a lightweight placeholder state.

---

## توصيات الأداء المستقبلية / Future Performance Recommendations

| الأولوية / Priority | التوصية / Recommendation |
| :---: | :--- |
| 🔴 فوري / Immediate | إصلاح كتابة DB داخل الحلقة في HabitStatusManager |
| 🟡 قريب / Soon | استبدال GlobalScope بـ applicationScope |
| 🟡 قريب / Soon | إضافة قياسات Benchmark Micro لوقت الإقلاع |
| 🟢 متوسط / Medium | اختبار الويدجت على أجهزة المواصفات المنخفضة |
| 🟢 متوسط / Medium | مراجعة التنبيهات المتزامنة لتجنب تكرار العروض |
