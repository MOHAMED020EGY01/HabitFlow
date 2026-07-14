# 02_LIBRARIES

## إصدارات المنشئ والأهداف / Sdk & Build Versions

### إعدادات الإصدارات الحالية للتطبيق
- **compileSdk**: 36
- **minSdk**: 24
- **targetSdk**: 36
- **اصدار لغة كوتلن (Kotlin Version)**: 2.2.10
- **مترجم كومبوز (Compose Compiler)**: مدمج مع كوتلن عبر المكون الإضافي (`libs.plugins.kotlin.compose` - الإصدار 2.2.10)
- **أداة بناء أندرويد (AGP Version)**: 9.1.1
- **أداة معالجة الرموز (KSP)**: 2.3.5

### Configured SDK & Build Toolchain
- **compileSdk**: 36
- **minSdk**: 24
- **targetSdk**: 36
- **Kotlin Version**: 2.2.10
- **Compose Compiler**: Enabled natively via Kotlin Compose Compiler Gradle plugin (version 2.2.10)
- **Android Gradle Plugin (AGP)**: 9.1.1
- **KSP Version**: 2.3.5

---

## جرد المكاتب والاعتماديات / Library Inventory

### المكاتب المستخدمة في واجهة المستخدم والعرض (UI & Compose)
1. **Jetpack Compose BOM (2024.09.00)**
   - *الوصف*: يدير ويضمن توافق إصدارات مكتبات Jetpack Compose المختلفة.
   - *أماكن الاستخدام*: جميع واجهات الشاشات والمكونات المخصصة تحت المجلد `presentation/`.
2. **Activity Compose (1.10.1)**
   - *الوصف*: يوفر تكامل واجهات Compose الرسومية مع دورة حياة الأنشطة (Activity).
   - *أماكن الاستخدام*: فئة `MainActivity.kt` لربط شاشة البداية بمحرك أندرويد.
3. **Compose Material 3**
   - *الوصف*: يوفر عناصر تصميم Material 3 المتطورة للواجهات.
   - *أماكن الاستخدام*: جميع شاشات العرض والبطاقات ذات التأثير الزجاجي (Glassmorphism).
4. **Coil Compose (2.7.0)**
   - *الوصف*: مكتبة غير متزامنة لتحميل وعرض الصور بكفاءة.
   - *أماكن الاستخدام*: شاشة العرض الرئيسية `HomeScreen.kt` وإعدادات الملف الشخصي `SettingsScreen.kt` لعرض صورة المستخدم الشخصية.
5. **Splash Screen API (1.0.1)**
   - *الوصف*: لإدارة وإبقاء شاشة البداية الترحيبية ظاهرة حتى تهيئة التطبيق.
   - *أماكن الاستخدام*: فئة `MainActivity.kt` لربط الإقلاع مع انتهاء تهيئة البيانات غير المتزامنة.
6. **AppCompat (1.7.0)**
   - *الوصف*: يوفر التوافقية مع الإصدارات القديمة من أندرويد لـ Activities والسمات.
   - *أماكن الاستخدام*: فئة `MainActivity.kt` ترث من `AppCompatActivity`.

### UI & Compose Library Stack
1. **Jetpack Compose BOM (2024.09.00)**
   - *Description*: Manages and aligns dependencies for Compose UI libraries.
   - *Usage*: Applied across all custom screens and UI layout elements under `presentation/`.
2. **Activity Compose (1.10.1)**
   - *Description*: Bridges the gap between traditional Android Activity lifecycles and Compose contents.
   - *Usage*: Declared in `MainActivity.kt` within `setContent`.
3. **Compose Material 3**
   - *Description*: Offers Material Design 3 style guidelines and widgets.
   - *Usage*: Deployed in layout systems, input text fields, and custom buttons.
4. **Coil Compose (2.7.0)**
   - *Description*: Non-blocking asynchronous image loader for Jetpack Compose.
   - *Usage*: Fetches and shows the user profile image in `HomeScreen.kt` and `SettingsScreen.kt`.
5. **Splash Screen API (1.0.1)**
   - *Description*: Facilitates standardized splash screens on Android.
   - *Usage*: Configured inside `MainActivity.kt` to defer launch screen dismiss until background initialization finishes.
6. **AppCompat (1.7.0)**
   - *Description*: Legacy support library allowing backwards-compatible execution of activities and themes.
   - *Usage*: Root inheritance class for `MainActivity`.

---

### المكاتب الخاصة بقواعد البيانات وحفظ الحالة (Persistence & DB)
1. **Room Persistence Runtime & KTX (2.7.0)**
   - *الوصف*: طبقة تجريد فوق قواعد بيانات SQLite لحفظ البيانات محلياً وبشكل متزامن عبر تدفقات التدفق (Flows).
   - *أماكن الاستخدام*: معرفة في `data/local/database/HabitDatabase.kt` وواجهات الاستعلام `HabitDao` و `NotificationDao`.
2. **DataStore Preferences (1.1.7)**
   - *الوصف*: بديل آمن وسلس لـ SharedPreferences لحفظ البيانات البسيطة كقيم ومفاتيح بشكل غير متزامن.
   - *أماكن الاستخدام*: ملف `UserPreferencesManager.kt` لحفظ إعدادات اللغة والمظهر وتفعيل الخدمة الخلفية، وملف `PendingOverlayStore.kt` لتخزين الإشعارات المؤجلة.

### Database & Persistence Stack
1. **Room Runtime & KTX (2.7.0)**
   - *Description*: Provides local SQLite object mapping and asynchronous Kotlin coroutines database transactions.
   - *Usage*: Configured in `data/local/database/HabitDatabase.kt` and used by DAO interfaces `HabitDao` and `NotificationDao`.
2. **DataStore Preferences (1.1.7)**
   - *Description*: Modern local key-value storage solution replacing old SharedPreferences.
   - *Usage*: Located in `UserPreferencesManager.kt` (for flags like language, theme, and animations) and `PendingOverlayStore.kt` (for deferring floating overlays).

---

### مكاتب الخلفية وجدولة المهام (Background & Coroutines)
1. **WorkManager Runtime KTX (2.9.0)**
   - *الوصف*: لجدولة وإطلاق المهام الخلفية الموثوقة والتي تضمن التنفيذ حتى لو تم إغلاق التطبيق.
   - *أماكن الاستخدام*: إدارة وجدولة المهام الأربع: `DailyRolloverWorker` و `DbVacuumWorker` و `HabitReminderWorker` و `HabitOverlayWorker`.
2. **Kotlinx Coroutines Core & Android (1.10.2)**
   - *الوصف*: لدعم الخيوط المتعددة والبرمجة غير المتزامنة عبر خيوط IO وخيوط العرض الرئيسية.
   - *أماكن الاستخدام*: منتشر في جميع نوافذ العرض (ViewModels) والعمال والمستودعات لإجراء العمليات غير المحظورة.

### Background Work & Threading Stack
1. **WorkManager (2.9.0)**
   - *Description*: Android utility for scheduling persistent, constraint-based background processing.
   - *Usage*: Manages schedules for `DailyRolloverWorker`, `DbVacuumWorker`, `HabitReminderWorker`, and `HabitOverlayWorker`.
2. **Kotlinx Coroutines (1.10.2)**
   - *Description*: Facilitates non-blocking concurrent programming on Dispatchers (IO, Default, Main).
   - *Usage*: Heavily present in all repositories, background workers, and view models.

---

### المكاتب الخاصة بالاختبار واستكشاف الأخطاء (Testing & Debugging)
1. **Roborazzi (1.59.0)**
   - *الوصف*: مكتبة لتسجيل لقطات شاشات الاختبار (Screenshot Testing) للتحقق من سلامة التصميم التفاعلي.
   - *أماكن الاستخدام*: اختبارات واجهة المستخدم في مجلد الاختبارات للتأكد من اتساق الشاشات مع التغييرات.
2. **LeakCanary (2.14)**
   - *الوصف*: تكتشف تلقائياً تسريبات الذاكرة العشوائية (RAM) أثناء تشغيل نسخ التطوير.
   - *أماكن الاستخدام*: يتم تنشيطها فقط في نسخ التطوير (debug) بتهيئة داخل `setupLeakCanaryConfig()` في `HabitApplication.kt`.
3. **MockK (1.13.12)**
   - *الوصف*: لإنشاء كائنات وهمية لخدمة اختبارات الوحدة.
4. **Turbine (1.1.0)**
   - *الوصف*: مكتبة متخصصة لاختبار تدفق البيانات (Flows) والتحقق من قيم البث المتتالية.

### Testing & Debugging Stack
1. **Roborazzi (1.59.0)**
   - *Description*: Snapshot screen testing tool to check UI integrity during automated tests.
   - *Usage*: Integrated in `test/` package for Compose layout rendering snapshots.
2. **LeakCanary (2.14)**
   - *Description*: Automatic memory leak detection utility for debug profiles.
   - *Usage*: Initialized via reflection inside `HabitApplication.kt` (for debug builds only).
3. **MockK (1.13.12)**
   - *Description*: Mocking library tailored specifically for Kotlin environments.
4. **Turbine (1.1.0)**
   - *Description*: Utility making Flow and StateFlow assertions streamlined.

---

## المكاتب المعرّفة وغير المستخدمة / Unused Declared Dependencies

### قائمة الاعتماديات المهجورة
تم فحص الكود البرمجي بالكامل للبحث عن استدعاءات للمكتبات أدناه، وتبين أنه **لم يتم استخدامها في أي ملف برميجي** على الرغم من التصريح بها في ملف البناء:
- **`libs.accompanist.permissions`**: مخصصة لإدارة الصلاحيات في كومبوز. (غير مستخدمة، الصلاحيات تُطلب يدوياً أو يتم تجاوزها).
- **`libs.retrofit` / `libs.okhttp` / `libs.logging.interceptor`**: مخصصة للمزامنة والاتصال بالخوادم البعيدة. (التطبيق محلي بالكامل).
- **`libs.converter.moshi` / `libs.moshi.kotlin` / `libs.moshi.kotlin.codegen`**: مخصصة للتحويل بين JSON وكائنات كوتلن. (يتم استخدام مكتبة `Gson` بدلاً منها في `HabitDetailViewModel`).
- **`libs.firebase.bom` / `libs.firebase.ai` / `libs.firebase.appcheck.recaptcha`**: ملحقات خدمات جوجل والذكاء الاصطناعي. (لا يوجد أي استخدام للذكاء الاصطناعي أو المزامنة البعيدة حالياً).

### List of Declared but Unused Libraries
A global search of imports and logic confirmed that these libraries are declared in `app/build.gradle.kts` but have **zero active usage** in the codebase:
- **`libs.accompanist.permissions`**: Designed for Compose-side permission handling. Permissions are requested natively.
- **`libs.retrofit` / `libs.okhttp` / `libs.logging.interceptor`**: Intended for network operations. App runs entirely offline/local-first.
- **`libs.converter.moshi` / `libs.moshi.kotlin` / `libs.moshi.kotlin.codegen`**: Deployed for JSON parsing. Codebase uses `Gson` inside `HabitDetailViewModel` instead.
- **`libs.firebase.bom` / `libs.firebase.ai` / `libs.firebase.appcheck.recaptcha`**: Firebase APIs and Gemini integrations. No active features depend on remote cloud endpoints.
