# 23_UNUSED_RESOURCES — الموارد المهملة والملفات الزائدة / Unused Resources Report

## الموارد غير المستوردة والمهملة / Unused Assets & Resources

تم فحص مجلد الموارد [res/](app/src/main/res) لمطابقته مع الأكواد، وتبين وجود الملفات التالية التي لا يتم استيرادها أو استعراضها في واجهات التطبيق الرسومية:

We scanned the app resources folder and checked for code usage. A large unreferenced image file was found in the drawables directory:

### 1. صورة `habit_flow_nav_mockup_1783011143917.jpg`
* **الموقع**: حزمة [drawable/](app/src/main/res/drawable) بحجم **368.5 كيلوبايت**.
* **التفاصيل**: صورة سكرين شوت أو كروكي تصميم الواجهات تم وضعه مؤقتاً في مجلد الموارد.
* **السبب**: لا يملك أي استدعاء في كلاسات المظهر أو المعرفات الرسومية للتطبيق.
* **التوصية**: الحذف الفوري لتقليل حجم ملف الـ APK بأكثر من 300 كيلوبايت دفعة واحدة.

### 2. صورة شعار العادة `habit_icon_logo.jpg`
* **الموقع**: حزمة [drawable/](app/src/main/res/drawable) بحجم **15.6 كيلوبايت**.
* **التفاصيل**: صورة ثابتة لشعار قديم للتطبيق.
* **السبب**: يتم بناء شعار التطبيق الفعلي وأيقونة المنبه عبر محول المتجهات `ic_habit_notification.xml` و `ic_launcher_foreground.xml` التفاعليين.
* **التوصية**: الحذف لتنظيف مجلد الموارد.

---

## قسم التحقق والأدلة / Verification & Evidence

* **Confidence Score / نسبة الثقة**: 100%
* **Evidence / الأدلة**:
  - تم فحص مجلد الصور وإجراء بحث شامل برقم الملف `habit_flow_nav_mockup_1783011143917` والتحقق من عدم وجود أي مخرجات له في الكود المصدري.
* **Files Used / الملفات المستخدمة**:
  - [drawable/](app/src/main/res/drawable)
* **Verification Status / حالة التحقق**: VERIFIED / مؤكد
