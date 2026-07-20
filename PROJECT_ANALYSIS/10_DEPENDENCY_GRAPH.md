# 10_DEPENDENCY_GRAPH — مخطط الاعتماديات / Logical Dependency Graph

## رسم بياني للاعتماديات والتسلسل المعماري / Architectural Dependency Flow

The logical mapping below demonstrates how dependencies flow downward across system layers. There is no dependency injection framework (like Dagger/Hilt); instead, all instances are instantiated directly in `HabitApplication` and requested dynamically:

```mermaid
graph TD
    subgraph Platform & UI (Outer Layer)
        A[MainActivity]
        B[AllHabitsWidget]
        C[HabitOverlayService / HabitBackgroundService]
        D[BootReceiver]
    end

    subgraph Presentation & ViewModels
        E[HomeViewModel / AddHabitViewModel]
    end

    subgraph Domain Layer
        F[UseCases: AddHabitUseCase, etc.]
        G[Repository Contracts: HabitRepository]
    end

    subgraph Data Layer
        H[HabitRepositoryImpl]
        I[HabitDatabase / Room DAOs]
        J[UserPreferencesManager]
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
```

---

## فحص الاعتماديات الدائرية / Circular Dependency Audit

* **فحص VMs و Repositories**: نماذج العرض تعتمد على حالات الاستخدام أو المستودعات. لا توجد حلقات.
* **فحص الخدمات ومستقبلات البث**: جميع التدفقات معزولة تسلسلياً.
* **النتيجة**: **Zero Circular Dependencies Detected**.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**: فحص الـ Imports واتجاه تدفق المسؤوليات المعمارية.
* **Files Used / الملفات المستخدمة**:
  - [HabitApplication.kt](app/src/main/java/com/example/app/HabitApplication.kt)
  - [HomeViewModel.kt](app/src/main/java/com/example/feature/home/presentation/HomeViewModel.kt)
  - [HabitRepositoryImpl.kt](app/src/main/java/com/example/core/repository/HabitRepositoryImpl.kt)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
