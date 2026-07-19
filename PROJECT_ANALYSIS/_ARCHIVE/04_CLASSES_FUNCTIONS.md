# 04_CLASSES_FUNCTIONS

## جرد الفئات والمسؤوليات / Class Inventory & Responsibilities

### المكونات والخدمات البرمجية الرئيسية
فيما يلي جرد للفئات الكبرى التي تشكل عصب تطبيق HabitFlow ووظيفتها في سطر واحد:
- **`HabitApplication`**: تهيئة التطبيق الأساسية ومستودعات البيانات بشكل غير متزامن وإدارة سياق اللغة وحقن الاعتماديات يدوياً.
- **`MainActivity`**: نقطة الدخول الرسومية، وإدارة شريط التنقل الشفاف ونظام حواف الشاشة والتحريكات الافتراضية.
- **`HabitRepositoryImpl`**: المستودع المركزي لتنسيق نقل البيانات وتحويل النماذج بين قاعدة البيانات Room ونماذج النطاق المستقلة.
- **`HabitDatabase`**: تعريف فئة قاعدة البيانات المحلية Room وتحديد نسخها ومجموعة جداولها مع برامج هجرة البيانات (Migrations).
- **`HabitDao`**: واجهة الاستعلام عن جداول العادات وسجلات الإنجاز وصياغة استعلامات التصفية وسجل الدورات التاريخية.
- **`NotificationDao`**: الاستعلام عن جدول الإشعارات وتخزين سجل التنبيهات المرسلة للمستخدم وحذفها.
- **`UserPreferencesManager`**: إدارة تفضيلات المستخدم البسيطة المخزنة في DataStore مثل مظهر الشاشة واللغة وحالة الخدمة.
- **`HabitBackgroundService`**: خدمة أمامية مستمرة تتحقق من التذكيرات الفائتة فور فتح قفل الشاشة لتعزيز الموثوقية.
- **`HabitOverlayService`**: خدمة أمامية مؤقتة مسؤولة عن عرض واجهة التذكير العائمة فوق التطبيقات الأخرى وتشغيل نغمة المنبه.
- **`AllHabitsWidget`**: فئة قطعة الشاشة التفاعلية (Glance Widget) لعرض العادات النشطة ومستويات الإنجاز وتوفير زر الإكمال السريع.
- **`InactiveHabitsWidget`**: ويدجت الشاشة الرئيسية لعرض آخر العادات المتوقفة وتواريخ ومدة توقفها.
- **`StreakCalculator`**: أداة حساب الأيام المتتالية المنجزة مع مراعاة أيام الجدولة الفعلية للعادة لتجنب انقطاع السلسلة.
- **`NextReminderCalculator`**: حساب الموعد التالي بدقة لإطلاق إنذار التنبيه استناداً لمواقيت التنبيه وأيام عمل العادة.
- **`HabitStatusManager`**: وحدة معالجة حالات العادات اليومية، الالتفاف اليومي عند منتصف الليل، الإيقاف التلقائي بعد 3 غيابات، وإكمال الدورات.

### Core Classes & Core Duties
Here is an inventory of key architectural classes and their core responsibilities:
- **`HabitApplication`**: Entry application class establishing manual DI container, asynchronously initializing database/repositories, and maintaining global configuration.
- **`MainActivity`**: Host entry activity managing immersive status bars, system padding config, and Compose root navigation.
- **`HabitRepositoryImpl`**: Concrete database coordinator mapping database entity objects to clean domain models.
- **`HabitDatabase`**: Declares Room Database tables, version schema increments (version 10), and handles migration pathways.
- **`HabitDao`**: Manages SQL query execution for habits, daily logs, widget info, and historical cycles.
- **`NotificationDao`**: Manages database queries for history logs of pushed reminders.
- **`UserPreferencesManager`**: Manages key-value system preferences stored in DataStore (themes, languages, animations, and banner states).
- **`HabitBackgroundService`**: Persistent foreground service executing unlock-triggered catch-up checks to make alarms highly reliable.
- **`HabitOverlayService`**: Temporarily active foreground service injecting the Compose-based alert overlay view onto WindowManager and looping alarm audio.
- **`AllHabitsWidget`**: Glance widget rendering top active habits, completion progress circles, and supporting direct tap check-ins.
- **`InactiveHabitsWidget`**: Glance widget rendering recently paused habits alongside historical stop dates and pause durations.
- **`StreakCalculator`**: Pure logic class evaluating consecutive habit completions without letting non-scheduled days break the streak.
- **`NextReminderCalculator`**: Utility determining next exact reminder alarm times given scheduled times and active days.
- **`HabitStatusManager`**: Orchestrates midnight checks to fill missing logs, enforce 3-day-miss auto-pauses, and archive completed habit cycles.

---

## نماذج العرض وحالات الاستخدام / ViewModels & UseCases

### نماذج العرض (ViewModels)
- **`HomeViewModel`**: إدارة العادات النشطة اليومية ومعدلات الإنجاز وعرض الاقتباسات المترجمة وتصفية البيانات بحسب تاريخ اليوم.
- **`AllHabitsViewModel`**: فرز وترتيب العادات بحسب التقدم وتاريخ البدء وتصفيتها (نشطة/متوقفة/مكتملة) والبحث النصي عنها.
- **`HabitDetailViewModel`**: عرض إحصائيات عادة محددة (السجل، التكرار، الدورات السابقة)، مع منطق الإيقاف والاستئناف وإعادة البدء والحذف.
- **`AddHabitViewModel`**: إدارة مدخلات نموذج إضافة أو تعديل عادة والتحقق من صحة البيانات وعدم وجود تعارض بين مواعيد التنبيهات.
- **`CalendarViewModel`**: إدارة وتجهيز نقاط التقويم اليومية المكتملة لعرضها في شاشة التقويم الشهري الموحد.
- **`NotificationsViewModel`**: جلب وتجهيز سجل التنبيهات وحذف إشعارات محددة أو تفريغ السجل بالكامل.
- **`SettingsViewModel`**: إدارة الملف الشخصي (الاسم والصورة) وتعديل خيارات الواجهة والمظهر وتطهير كافة البيانات المخزنة.
- **`SplashViewModel`**: التحقق من انتهاء تهيئة التطبيق وتحديد الشاشة التالية (التعليمات التوجيهية أو الواجهة الرئيسية).
- **`SummaryViewModel`**: تجميع بيانات العادات الإحصائية وحساب نسب النجاح وتجهيز الرسوم البيانية لتوزيع العادات والألوان.

### حالات الاستخدام (UseCases)
- **`AddHabitUseCase`**: التحقق من صلاحية إضافة العادة وعدم تخطي السقف الأعلى المسموح به (6 عادات نشطة).
- **`DeleteHabitUseCase`**: حذف العادة كلياً وإلغاء جميع جدولة مهام التنبيه والعمال الخلفيين المرتبطين بها.
- **`GetActiveHabitsCountUseCase`**: تجميع وتدفق عدد العادات النشطة حالياً في قاعدة البيانات لمتابعة حد الاستهلاك.
- **`GetAllHabitsUseCase`**: استرجاع قائمة بجميع العادات الموثقة في قاعدة البيانات بشكل تفاعلي.
- **`GetHabitDetailsUseCase`**: دمج تفاصيل العادة وسجل إنجازاتها لحساب نسب النجاح الحالية ومجموع الغيابات الحقيقية والانتظام.
- **`GetHabitsSummaryUseCase`**: قراءة إجمالي العادات النشطة ومعدلات النجاح لتسهيل عرض ملخص لوحة التحكم.
- **`ToggleHabitActiveUseCase`**: إيقاف عادة نشطة أو إعادة تنشيط عادة متوقفة بعد التحقق من سقف العادات المسموح بها.
- **`UpdateHabitUseCase`**: إجراء تعديلات على الاسم، الوصف، الألوان، الأيام النشطة، أو مواعيد التنبيه لعادة مخزنة سابقاً.
- **`ValidateReminderTimeUseCase`**: التأكد من وجود فاصل زمني كافٍ (لا يقل عن 10 دقائق) بين التذكيرات لنفس العادة أو بين العادات المشتركة في نفس الأيام النشطة.

---

## الفئات والوظائف الضخمة (الخارجة عن المألوف) / Line-Count Outliers

### الفئات المرشحة للتجزئة نظراً لحجمها الكبير
لوحظ وجود عدد من الملفات التي تجاوز حجمها مئات الأسطر البرمجية، وتعتبر فئات مرشحة للتفكيك إلى مكونات أصغر في المستقبل:
1. **`HabitDetailScreen.kt` (1115 سطراً)**:
   - *السبب*: يحتوي على الكثير من عناصر الرسم البياني المخصصة، نوافذ الحذف والتوجيه الرسومي، تفاصيل السجل الأسبوعي وجداول الدورات التاريخية معاً في نفس الملف.
   - *التوصية*: فصل مكونات الرسوم البيانية للنسب المئوية وعرض الدورات السابقة في ملفات مستقلة في حزمة المكونات المشتركة.
2. **`SettingsScreen.kt` (723 سطراً)**:
   - *السبب*: يجمع خيارات متعددة لإدارة الملف الشخصي، الألوان الزجاجية، تفعيل الخدمة الخلفية، واختيار اللغات.
   - *التوصية*: استخلاص كل قسم كعنصر برمجي منفصل (مثال: `ProfileSection`, `ThemeSection`).
3. **`AddHabitScreen.kt` (598 سطراً)**:
   - *السبب*: يعالج اختيار الألوان والوقت والأيام وجدول التنبيهات في واجهة واحدة.
4. **`HomeScreen.kt` (571 سطراً)**:
   - *السبب*: يحتوي على لوحة تحكم مخصصة لعرض التقدم والإحصائيات السريعة والبطاقات ذات التأثير الزجاجي التفاعلي.

### Line-Count Outliers & Refactoring Candidates
These files represent significant line-count outliers and are recommended targets for design decomposition:
1. **`HabitDetailScreen.kt` (1115 lines)**:
   - *Why*: Packs custom canvas charts, delete confirmation dialogs, calendar grids, cycle history lists, and complex states into a single file.
   - *Recommendation*: Extract stats charts and cycle history list items into modular composable files.
2. **`SettingsScreen.kt` (723 lines)**:
   - *Why*: Manages profile editors, language config dropdowns, background keepalive toggles, and data wipe dialogues in one container.
   - *Recommendation*: Move sub-sections (e.g., Profile setting panel) into separate layout files.
3. **`AddHabitScreen.kt` (598 lines)**:
   - *Why*: Handles day selectors, multiple-time wheel pickers, custom color grids, and validation error layouts.
4. **`HomeScreen.kt` (571 lines)**:
   - *Why*: Renders a dashboard layout, progress rings, custom glass cards, and motivational quote displays.

---

## الأكواد الميتة (غير المستخدمة) / Dead Code & Unreferenced Files

### الأكواد البرمجية التي لا يقرأها أي كائن آخر
تم إجراء عملية بحث نصي واسعة النطاق للتأكد من سلامة جميع الاستدعاءات، وتحديد الكود المهمل التالي:
1. **`HalfCircleProgressRenderer.kt` (ملف كامل / Whole file)**:
   - *التشخيص*: فئة الخدمة `HalfCircleProgressRenderer` معرّفة داخل المجلد `widget/util/` بالكامل، ولكن لا توجد أي إشارة أو استيراد لها في أي مكان آخر بالتطبيق. تستخدم القطع حالياً فئة `CircularProgressRingRenderer` حصراً للرسم.
   - *القرار*: يعتبر كوداً ميتاً تماماً تجب تصفيته أو إزالته.

### Unused Logic & Dead Code
A search of imports and references identified these unreferenced code fragments:
1. **`HalfCircleProgressRenderer.kt` (Whole File)**:
   - *Details*: The renderer object is defined at `widget/util/HalfCircleProgressRenderer.kt` but is never imported or referenced. The Glance widgets draw progress circles exclusively via `CircularProgressRingRenderer`.
   - *Status*: Dead code. It can be safely deleted to reduce APK size and compiler overhead.
