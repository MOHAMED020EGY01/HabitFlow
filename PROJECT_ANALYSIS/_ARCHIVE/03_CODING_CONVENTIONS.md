# 03_CODING_CONVENTIONS

## التسمية وتنظيم الكود / Naming & Code Organization

### قواعد التسمية المتبعة فعلياً
يتبع المشروع قواعد محددة لتسمية الملفات والطبقات لضمان سهولة التعرف على مهامها:
- **نماذج العرض (ViewModels)**: تنتهي الكلمة دائماً بـ `ViewModel` (مثال: `HomeViewModel`, `AddHabitViewModel`).
- **حالات الاستخدام (UseCases)**: تنتهي الكلمة بـ `UseCase` (مثال: `AddHabitUseCase`, `DeleteHabitUseCase`).
- **قواعد البيانات وداعمي الاستعلام (Databases & DAOs)**: تنتهي قواعد البيانات بـ `Database` (مثال: `HabitDatabase`) والواجهات بـ `Dao` (مثال: `HabitDao`).
- **العناصر الرسومية (Composables)**: تبدأ بحرف كبير وتكون بصيغة أسماء تعبر عن الواجهة (مثال: `HomeScreen`, `GlassCard`).
- **تنظيم الملفات**: يتم تنظيم الشاشات في مجلدات فرعية مستقلة تحتوي على الشاشة ونموذج العرض الخاص بها معاً (مثل: `presentation/screens/home/` يضم `HomeScreen.kt` و `HomeViewModel.kt`).

### Coding Naming & Organization
The codebase adheres to clean naming conventions making responsibilities clear:
- **ViewModels**: Saffixed with `ViewModel` (e.g., `HomeViewModel`, `AddHabitViewModel`).
- **Use Cases**: Suffixed with `UseCase` (e.g., `AddHabitUseCase`, `DeleteHabitUseCase`).
- **Database Components**: Databases end in `Database` (e.g., `HabitDatabase`), and access objects end in `Dao` (e.g., `HabitDao`).
- **Composables**: Declared using PascalCase representing view structures (e.g., `HomeScreen`, `GlassCard`).
- **File Hierarchy**: Screens are grouped by feature, placing the Screen UI composable and its respective ViewModel in the same subpackage (e.g., `presentation/screens/home/`).

---

## إدارة الحالة في كومبوز / State Management in Compose

### نمط تدفق البيانات أحادي الاتجاه (UDF)
يتم التحكم في حالة الشاشات وعرضها عبر الأنماط التالية:
1. **تدفق الحالة الثابتة (StateFlow)**: تعرّف الشاشات حالة متكاملة عبر كائن بيانات غير قابل للتعديل (مثل `AllHabitsUiState`) يتم بثه كـ `StateFlow` وتجميعه في الواجهة باستخدام `collectAsState()`.
2. **شرائح الحالة المستقلة (SnapshotStateList/SnapshotStateMap)**: لتفادي إعادة رسم القوائم بأكملها عند تحديث عنصر واحد، يستخدم التطبيق (مثل `HomeViewModel`) مجموعات تفاعلية ذكية مثل `mutableStateListOf` و `mutableStateMapOf`. يضمن هذا تحديث السطر المتأثر فقط في القائمة.
3. **أحداث الواجهة الفردية (One-off Events)**: مثل إظهار رسائل التنبيه (Snackbar) أو التوجيه لشاشة أخرى؛ حيث تُبث كتدفق مشترك مؤقت `MutableSharedFlow<UiEvent>` ويتم استقبالها لمرة واحدة داخل كتلة `LaunchedEffect`.

### UI State Architecture & Reactivity
Data flows in a Unidirectional Data Flow (UDF) model:
1. **StateFlow UI States**: Screens aggregate UI variables into immutable data classes (e.g., `AllHabitsUiState`) exposed as a `StateFlow` and observed in composables via `collectAsState`.
2. **Granular State Lists**: To optimize LazyColumn rendering, ViewModels like `HomeViewModel` leverage Compose runtime utility collections `mutableStateListOf` and `mutableStateMapOf`. Item-level changes trigger updates only in the affected row, bypassing full-list recompositions.
3. **UI Event Streams**: Instant events (like showing toast notices or triggers) are modeled as event classes emitted via `MutableSharedFlow` and handled sequentially in Compose side-effects (`LaunchedEffect`).

---

## معالجة الأخطاء / Error Handling

### ثقافة معالجة الاستثناءات في المشروع
تعتمد السياسة المتبعة في المشروع على احتواء الأخطاء محلياً وتجنب انهيار التطبيق:
- **تطويق الكتل البرمجية**: يكثر استخدام النموذج المبسط `try { ... } catch (e: Exception) { e.printStackTrace() }` في المهام الخلفية، عمال المزامنة، ومحاولات تحديث وتهيئة اللغات والواجهات العائمة.
- **مسجل الأخطاء الشامل**: تم تكوين مسجل افتراضي في `HabitApplication` لالتقاط الانهيارات الكارثية غير المتوقعة وكتابتها في السجلات (`Log.e("HabitTrackerCrash")`) قبل خروج المستخدم للتأكد من سهولة تصحيح الأعطال في بيئة التطوير.

### Exception Management Style
The error handling style focuses on safety and preventing crashes over complex error state propagation:
- **Try-Catch Suppression**: Safe calls wrapped in `try { ... } catch (e: Exception) { e.printStackTrace() }` are widely used in database operations, worker triggers, language swaps, and sound controllers.
- **Global Uncaught Crash Logger**: Registered inside `HabitApplication.onCreate` to intercept uncaught errors on any thread, logging the stacktrace with a dedicated tag (`HabitTrackerCrash`) to simplify debugging before delegating to the OS launcher.

---

## التزامن وكوروتينات كوتلن / Threading & Coroutines

### توزيع المهام وإدارة المسارات
توزع المهام على خيوط المعالجة بناءً على نوعها:
- **نطاق التطبيق الموحد (`applicationScope`)**: روتين عالمي معرّف في التطبيق يمتد بعمر عملية التطبيق لتنفيذ المهام غير المرتبطة بواجهة مستخدم معينة (مثل جدولة التنبيهات ومزامنة القطع التفاعلية).
- **الخيوط الخلفية (`Dispatchers.IO`)**: تُجبر عمليات القراءة والكتابة والفرز الثقيل على خيوط المعالجة المدعومة بـ `IO` لتفادي تجمد الواجهة الرسومية (مثل `flowOn(Dispatchers.IO)` أو `withContext(Dispatchers.IO)`).
- **الكتل المحظورة (`runBlocking`)**: تُستدعى بشكل استثنائي أثناء بدء التشغيل في `HabitApplication` لضمان قراءة لغة التطبيق وتطبيق التخطيط المناسب (أو التعريب) قبل رسم الشاشة الأولى لمنع الارتجاج المرئي للواجهة.

### Asynchronous Execution Rules
The codebase divides concurrent threads into logical areas:
- **`applicationScope`**: A lifecycle-persistent CoroutineScope utilizing `SupervisorJob() + Dispatchers.Default` for operations that must survive screen destructions (e.g., widget synchronization, database rollover processing).
- **`Dispatchers.IO`**: Explicitly set for all SQLite interactions and heavy-duty operations via `withContext(Dispatchers.IO)` or `flowOn(Dispatchers.IO)`.
- **`runBlocking`**: Utilized sparingly during initial startup (like fetching language configurations in `HabitApplication`'s onCreate) to block the UI thread until preferences are read, ensuring text direction is correctly configured prior to view drawing.

---

## حقن الاعتماديات اليدوي / Manual Dependency Injection

### آلية تمرير الكائنات والخدمات
بعد الاستغناء عن مكتبة Hilt، يتبع التطبيق طريقة حقن يدوية مركزية:
1. يتم الإعلان عن كائن قاعدة البيانات والمستودع وحالات الاستخدام كمتغيرات تهيئة مؤجلة `lateinit var` في `HabitApplication`.
2. يتم بناؤها دفعة واحدة وبشكل آمن داخل كتل برمجية غير متزامنة لتجنب التجميد الإقلاعي.
3. تحصل النماذج (ViewModels) على الاعتماديات عبر قراءة كائن التطبيق مباشرة:
   ```kotlin
   val app = application as HabitApplication
   val repository = app.repository
   ```

### Manual DI Architecture
The manual dependency injection architecture is structured as follows:
1. Core services (database, repositories, and use cases) are declared as globally accessible `lateinit var` properties on the `HabitApplication` companion instance.
2. The initial build is packaged inside a single `CoroutineScope.async` call on startup to avoid blocking the main thread.
3. ViewModels retrieve reference instances by casting context to the custom application wrapper:
   ```kotlin
   val app = application as HabitApplication
   val repository = app.repository
   ```

---

## التعريب والاتجاهات / Localization & Layout Direction

### دعم العربية والكتابة من اليمين لليسار (RTL)
يتم معالجة التعريب بشكل متكامل على مستوى النظام والكومبوز:
- **تحويل سياق التطبيق (Context wrapping)**: تستخدم فئة `LocaleDirectionHelper` لإنتاج سياق محلي مُهيأ للغة المطلوبة (`Context.createConfigurationContext`) لضمان قراءة قيم النصوص الصحيحة من مجلدات الموارد المترجمة (`values-ar`).
- **توجيه التخطيط**: يتم تمرير اتجاه تخطيط الشاشة (`LayoutDirection.Rtl` أو `LayoutDirection.Ltr`) وتغليف واجهة التطبيق بأكملها به لضمان دوران الأزرار والقوائم والبطاقات تلقائياً.
- **تنسيق الأرقام**: يحتوي الكود المساعد `AppFormatters.forceWesternDigits` على منطق لضمان اتساق الأرقام وتنسيق التواريخ والنسب المئوية بما يتناسب مع رغبة المستخدم.

### Dynamic Localization & Text Direction
RTL and translation systems are enforced locally:
- **Context Customization**: The `LocaleDirectionHelper` creates configured contexts using `context.createConfigurationContext(config)` containing the user's selected locale (e.g., Arabic).
- **CompositionLocal Layouts**: The UI tree is wrapped inside a CompositionLocalProvider serving the dynamically resolved `LocalLayoutDirection`. Layouts mirror automatically when language switches.
- **Digit Filtering**: Custom formatters like `AppFormatters.forceWesternDigits` ensure digits (e.g. percentages or dates) render consistently depending on UI requirements.

---

## تصميم وتحديث القطع التفاعلية / Glance Widget Conventions

### النمط البرمجي المعتمد للقطع الخارجية
تتبع ويدجت الشاشة الرئيسية (Glance AppWidgets) نمط بناء ثلاثي الطبقات:
1. **تأثير الغطاء الزجاجي (Glassmorphism 3-Layer Pattern)**:
   - *الطبقة الأولى*: حاوية خارجية تحدد الحدود الجانبية وشكل الزوايا المنحنية (`cornerRadius`).
   - *الطبقة الثانية*: خلفية داكنة خفيفة توفر حماية لسهولة قراءة النصوص.
   - *الطبقة الثالثة*: غشاء زجاجي ملون وشبه شفاف (`glassTint`) يحتوي على المحتوى التفاعلي للقطعة.
2. **التحديث الفوري المباشر (Direct Update Bypass)**:
   يتجنب التطبيق آلية التحديث القياسية البطيئة لـ Glance (التي قد تستغرق 45 ثانية بسبب قفل الجلسات)؛ حيث يقوم باستدعاء مباشر لمعالج الرسوم `WidgetDirectUpdater.pushDirectUpdate` لتوليد الرسوم محلياً ودفعها مباشرة لـ `AppWidgetManager`.
3. **التحديث المهادن الذكي (Debounced Sync)**:
   عند قيام المستخدم بعمليات نقر متعددة سريعة، يقوم `HabitWidgetSyncUpdater.updateNow` بتطبيق مهلة تأخير (3 ثوانٍ)؛ لتجميع كافة التحديثات في عملية رسم واحدة لحماية بطارية الهاتف من الاستنزاف.

### Glance Widget Implementation Conventions
AppWidgets on the home screen strictly employ optimized Glance rendering patterns:
- **Glassmorphism 3-Layer Canvas**:
  - *Layer 1 (Outer border)*: Defines outline boundaries and corner radii.
  - *Layer 2 (Inner fill)*: Implements a dark background card protecting text readability.
  - *Layer 3 (Glass layer)*: A semitransparent accent-tinted box (`glassTint` with ~15% alpha) holding the interactive components.
- **Immediate Direct Updates**:
  Instead of utilizing Glance's standard asynchronous update cycle (which suffers from a built-in session lock duration of up to 45s), widgets bypass this using `WidgetDirectUpdater.pushDirectUpdate`. It composing views on the Main thread and immediately pushes the RemoteViews to `AppWidgetManager`.
- **Sync Debounce (3-second threshold)**:
  Rapid toggles are intercepted by `HabitWidgetSyncUpdater.updateNow`, which queues and debounces requests for 3 seconds. Multiple successive user clicks trigger only a single database/view refresh, minimizing system wakeups.
