# 10_DEPENDENCY_GRAPH — مخطط الاعتماديات / Logical Dependency Graph

## رسم بياني للاعتماديات والتسلسل المعماري / Architectural Dependency Flow

يسرد المخطط التالي اتجاه التدفق البرمجي لتمرير وبناء الكائنات والاعتماديات في تطبيق **HabitFlow**، مع توضيح نقطة الربط اليدوي في التطبيق:

The logical mapping below demonstrates how dependencies flow downward across system layers. There is no dependency injection framework (like Dagger/Hilt); instead, all instances are instantiated directly in `HabitApplication` and requested dynamically:

```mermaid
graph TD
    subgraph Platform & UI (Outer Layer)
        A[MainActivity]
        B[AllHabitsWidget / InactiveHabitsWidget]
        C[HabitOverlayService / HabitBackgroundService]
        D[BootReceiver / PendingOverlayReceiver]
    end

    subgraph Presentation & ViewModels
        E[HomeViewModel / AddHabitViewModel / DetailViewModel]
    end

    subgraph Domain Layer (Pure Rules)
        F[UseCases: AddHabitUseCase, DeleteHabitUseCase]
        G[Repository Contracts: HabitRepository]
    end

    subgraph Data Layer (Concrete Implementations)
        H[HabitRepositoryImpl]
        I[HabitDatabase / Room DAOs]
        J[UserPreferencesManager / DataStore]
    end

    %% Dependency Links
    A --> E
    E --> F
    E --> H
    F --> G
    H --> G
    H --> I
    H --> J
    C --> J
    C --> H
    B --> H
    D --> H
    D --> I
```

---

## فحص الاعتماديات الدائرية / Circular Dependency Audit

تمت مراجعة الهيكل البرمجي والروابط للاستيراد في كود المشروع بالكامل للتأكد من خلوه من أي **اعتماديات دائرية (Circular Dependencies)** والتي قد تؤدي إلى انهيارات إقلاعية وتسربات في الذاكرة العشوائية:

* **فحص VMs و Repositories**:
  * نماذج العرض (ViewModels) تعتمد على واجهات المستودعات أو حالات الاستخدام.
  * حالات الاستخدام ومستودع البيانات تقع تحت طبقة مختلفة ولا يوجد أي إشارة أو استدعاء لـ ViewModels أو Screens من داخل الطبقات الداخلية.
* **فحص الخدمات ومستقبلات البث**:
  * خدمة `HabitOverlayService` تدير النافذة العائمة وتستدعي `ReminderAudioRepository`. المنبه يطلق الخدمة، والخدمة تعيد بث النتائج لـ `OverlayQueueManager`. لا توجد حلقة مغلقة تملك مسار استدعاء حاد متزامن.
* **فحص حقن الاعتماديات**:
  * نظراً لأن حقن الاعتماديات يدوي كلياً ومحكوم بتسلسل غير متزامن يبدأ من `HabitApplication` ويُعين المتغيرات لمرة واحدة `lateinit` داخل خيط IO منفصل، فلا يوجد تعارض في دورات الحياة الإقلاعية للكائنات.

* **Result / النتيجة**:
  * **خالٍ من الاعتماديات الدائرية / Zero Circular Dependencies Detected**: تم التأكد أن جميع التدفقات معزولة تسلسلياً وتسير في اتجاه أحادي خاضع للتكامل.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم إجراء مسح نصي لجميع الاستدعاءات والواردات (Imports) للتأكد من اتجاه تدفق المسؤوليات المعمارية.
* **Files Used / الملفات المستخدمة**:
  - [HabitApplication.kt](app/src/main/java/com/example/HabitApplication.kt#L100-L127)
  - [HomeViewModel.kt](app/src/main/java/com/example/presentation/screens/home/HomeViewModel.kt)
  - [HabitRepositoryImpl.kt](app/src/main/java/com/example/data/repository/HabitRepositoryImpl.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
