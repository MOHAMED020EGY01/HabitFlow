# DATABASE

## قاعدة البيانات والتخزين المحلي / Database & Local Persistence

### التخزين الأساسي المستخدم
يعتمد تطبيق HabitFlow كلياً على نمط التخزين المحلي (Local-First) باستخدام تقنيتين رئيسيتين:
- **Room (فوق SQLite)**: للبيانات المنظمة مثل العادات وسجلات الإنجاز والإشعارات.
- **DataStore (مفضلات DataStore Preferences)**: لحفظ تفضيلات المستخدم البسيطة مثل اللغة والمظهر.

### Primary Storage Technologies
HabitFlow is fully local-first. No remote API or cloud database is integrated:
- **Room over SQLite**: For structured relational data like habits, daily logs, cycle histories, and notifications.
- **DataStore (Preferences)**: For lightweight, non-relational user settings like language, theme flags, and onboarding state.

---

## مخطط قاعدة البيانات / Database Schema

### الجداول الرئيسية (Room Entities)
قاعدة البيانات معرّفة في فئة `HabitDatabase` بالإصدار الحالي **10**، وتتضمن الجداول التالية:

#### 1. `habits` — جدول تعريف العادات الرئيسي
يخزن كامل وصف العادة وإعداداتها التشغيلية:
| العمود / Column | النوع / Type | الوصف / Description |
| :--- | :--- | :--- |
| `id` | String (UUID) | المعرف الفريد للعادة / Unique habit ID |
| `name` | String | اسم العادة / Habit display name |
| `description` | String | وصف العادة / Optional habit description |
| `colorHex` | String | رمز اللون السداسي / Hex color code for UI |
| `durationDays` | Int | مدة العادة بالأيام / Total planned duration in days |
| `activeDays` | String (JSON) | أيام الأسبوع النشطة (JSON list) / Active days of week |
| `reminderTimes` | String (JSON) | مواعيد التنبيه المتعددة / Reminder times as JSON list |
| `status` | String | حالة العادة (ACTIVE/INACTIVE/COMPLETED) / Lifecycle status |
| `startDate` | String | تاريخ بدء الدورة الحالية / Cycle start date |
| `endDate` | String (nullable) | تاريخ انتهاء الدورة / Cycle end date |
| `inactiveDaysCount` | Int | عدد أيام الغياب المتراكمة / Accumulated missed days |
| `inactiveTimestamp` | Long (nullable) | وقت الإيقاف الأخير / Unix timestamp of last pause |

#### 2. `habit_logs` — جدول سجلات الإنجاز اليومي
يسجل حالة العادة لكل يوم تقويمي:
| العمود / Column | النوع / Type | الوصف / Description |
| :--- | :--- | :--- |
| `id` | Long (auto) | مفتاح أساسي / Auto-generated primary key |
| `habitId` | String (FK) | ربط بجدول العادات / Foreign key to habits |
| `date` | String | تاريخ اليوم (YYYY-MM-DD) / Log date |
| `status` | String | الحالة (DONE/MISS/PENDING) / Completion status |

#### 3. `habit_cycle_history` — جدول أرشيف الدورات المكتملة
يحتفظ بسجل تاريخي لكل دورة منتهية أو متوقفة:
| العمود / Column | النوع / Type | الوصف / Description |
| :--- | :--- | :--- |
| `id` | Long (auto) | مفتاح أساسي / Auto-generated primary key |
| `habitId` | String (FK) | ربط بجدول العادات / Foreign key to habits |
| `startDate` | String | تاريخ بداية الدورة / Cycle start date |
| `endDate` | String | تاريخ نهاية الدورة / Cycle end date |
| `completionRate` | Float | نسبة الإنجاز الكلية / Overall completion rate |
| `totalDays` | Int | إجمالي أيام الدورة / Total days in cycle |
| `completedDays` | Int | عدد أيام الإنجاز / Completed days count |
| `outcome` | String | نتيجة الدورة (COMPLETED/FAILED/PAUSED) / Cycle result |

#### 4. `notifications` — جدول سجل الإشعارات
يحتفظ بقائمة تاريخية لتنبيهات التذكير المرسلة:
| العمود / Column | النوع / Type | الوصف / Description |
| :--- | :--- | :--- |
| `id` | Long (auto) | مفتاح أساسي / Auto-generated primary key |
| `habitId` | String | المعرف المرتبط بالإشعار / Associated habit ID |
| `habitName` | String | اسم العادة عند وقت التنبيه / Habit name at notification time |
| `timestamp` | Long | وقت إرسال الإشعار / Unix timestamp sent |
| `type` | String | نوع الإشعار (REMINDER/OVERLAY/SYSTEM) / Notification type |

---

## DataStore والمفضلات / DataStore Preferences

### البيانات المخزنة في DataStore
يدير ملف `UserPreferencesManager.kt` المفاتيح والقيم التالية:

| المفتاح / Key | النوع / Type | الوظيفة / Purpose |
| :--- | :--- | :--- |
| `user_name` | String | اسم المستخدم الشخصي / Profile display name |
| `user_photo_path` | String | مسار صورة الملف الشخصي / Profile image local path |
| `selected_language` | String | اللغة المختارة (ar/en) / Selected UI language code |
| `is_dark_theme` | Boolean | وضع الشاشة الداكنة / Dark mode toggle |
| `onboarding_done` | Boolean | اكتمال التوجيه الأولي / First-launch onboarding flag |
| `background_service_enabled` | Boolean | تفعيل خدمة الخلفية / Keepalive service toggle |
| `glass_effects_enabled` | Boolean | تفعيل التأثيرات الزجاجية / Glassmorphism effects toggle |
| `transparent_navbar` | Boolean | شريط التنقل الشفاف / Transparent navigation bar |
| `last_rollover_date` | String | آخر تاريخ تفعيل الالتفاف اليومي / Last midnight rollover marker |
| `show_inactive_banner` | Boolean | إظهار لافتة العادات المتوقفة / Show paused habits banner |

يدير ملف `PendingOverlayStore.kt` قائمة انتظار التذكيرات العائمة المؤجلة حتى فتح قفل الجهاز.

---

## الهجرات / Migration History

| رقم الهجرة / Migration | التغييرات / Changes |
| :---: | :--- |
| 1 → 2 | إنشاء الجداول الأساسية / Initial schema creation |
| 2 → 3 | إضافة فهارس للبحث السريع / Index additions for query speed |
| 3 → 4 | إنشاء جدول الإشعارات / Notification table creation |
| 4 → 5 | إضافة حقل `inactiveTimestamp` / Inactive timestamp support |
| 5 → 8 | تعديلات على تنسيق الوقت وحقول الدورات / Reminder format and cycle fields |
| 8 → 10 | إعادة هيكلة جدول سجل الدورات وتعديل مخطط بيانات الأيام / Cycle history schema refactor |

---

## الإعدادات التشغيلية / Operational Configuration

- **نمط WAL (Write-Ahead Logging)**: مُفعّل لتحسين سرعة الكتابة وتقليل قفل الجداول.
- **التنظيف التلقائي (Incremental Vacuum)**: مُفعّل عبر `DbVacuumWorker` بدورية أسبوعية.
- **نافذة المعالجة القصوى**: تقتصر معالجة التقويم التاريخي على آخر 30 يوماً لتجنب استهلاك المعالج.
- **النسخ الاحتياطي**: مُفعّل عبر Android Auto Backup مع قواعد استثناء تمنع نسخ قاعدة البيانات الحساسة.

### Operational Notes
- **WAL Mode**: Enabled for all database connections to improve concurrent read/write performance.
- **Weekly Vacuum**: `DbVacuumWorker` runs a PRAGMA incremental_vacuum every 7 days to reclaim freed page space.
- **30-Day Lookback**: Historical daily log processing caps at 30 days to prevent unbounded CPU spikes.
- **Backup Rules**: Android Auto Backup is enabled but configured with exclusion rules in `backup_rules.xml` for sensitive tables.
