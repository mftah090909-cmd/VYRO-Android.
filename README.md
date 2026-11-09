# VYRO Android

تطبيق VYRO اجتماعي عربي مبني بـ Kotlin وJetpack Compose وMaterial 3، ومربوط فعليًا بمشروع Supabase.

## التشغيل

1. افتح مجلد `VYRO-Android` في Android Studio حديث يدعم AGP 8.7 وKotlin 2.0.
2. شغّل `./gradlew assembleDebug` بعد إضافة Gradle Wrapper من Android Studio، أو شغّل `gradle assembleDebug` إذا كان Gradle مثبتًا.
3. ثبّت `app/build/outputs/apk/debug/app-debug.apk` على جهاز Android 6 أو أحدث.

## ما تم تنفيذه عبر Supabase

- إنشاء حساب وتسجيل الدخول عبر Supabase Auth.
- حفظ جلسة الوصول محليًا وإمكانية تسجيل الخروج.
- تحميل المنشورات من PostgreSQL وإنشاؤها للمستخدم الحالي.
- الإعجاب وإلغاء الإعجاب وإضافة التعليقات عبر REST API.
- ملف شخصي أولي وتنقل عربي بخمس تبويبات.
- مخطط Supabase في `supabase/migrations/001_initial_schema.sql` يتضمن RLS.
- GitHub Actions باستخدام Java 17 واسم artifact هو `VYRO-Android-APK`.

## غير منفذ بعد

رفع الصور والفيديو، الملف الشخصي الكامل، FCM، الرسائل الفورية عبر Realtime، معالجة الفيديو، والرومات الصوتية الحية تحتاج طبقات إضافية ومزودًا حقيقيًا للصوت مثل LiveKit أو Agora. لا تستخدم الواجهة وحدها كبديل عن الخدمة الحقيقية.

## إعداد Supabase المقترح

نفّذ ملف SQL في Supabase SQL Editor، ثم فعّل Email Provider من Authentication > Providers. أثناء الاختبار يمكنك تعطيل Confirm email أو إبقاءه مفعّلًا للإطلاق الحقيقي. مفتاح Publishable/anon مخصص للتطبيق ويمكن استخدامه مع RLS؛ لا تضع `service_role` أو كلمة مرور قاعدة البيانات داخل Android.

## ملاحظة مهمة

الاتصال الحالي بالمصادقة والمنشورات والإعجابات والتعليقات حقيقي، بينما الميزات المذكورة في قسم «غير منفذ بعد» تحتاج تطويرًا إضافيًا واختبارًا منفصلًا.
