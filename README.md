# مسجد الإمام البخاري — تطبيق الأندرويد

تطبيق لإدارة اللجان والامتحانات والمهام. البيانات الرئيسية على السيرفر المشترك مع الويب؛ التطبيق بيحفظ نسخة محلية (Room)، والتعديلات اللي ما وصلتش للسيرفر بتتحفظ في طابور وتتبعت عند توفر الاتصال.

## التشغيل
1. افتح المشروع في Android Studio (JDK 17 أو أحدث).
2. شغّل السيرفر (`Web`) على الجهاز. على المحاكي العنوان الافتراضي للتطوير هو `http://10.0.2.2:3000/`.
3. شغّل التطبيق.

## بناء الإصدار
```bash
./gradlew assembleRelease -PAPI_BASE_URL=https://<رابط-السيرفر>/
```
الإصدار لازم يستخدم رابط `https`، وإلا البناء بيفشل. توقيع الإصدار يتحدد بمتغيرات البيئة `KEYSTORE_PATH` و`STORE_PASSWORD` و`KEY_PASSWORD`.

## نشر تحديث للأجهزة (بدون متجر)
التطبيق بيفحص السيرفر لما يتفتح، ولو فيه نسخة أحدث بينزّلها ويثبّتها لوحده (`AppUpdater`). لنشر نسخة:
1. ارفع `versionCode` و`versionName` في `app/build.gradle.kts`، واعمل merge على `main`.
2. من GitHub: **Actions > Publish Android > Run workflow**، واكتب «ما الجديد».
3. الـ workflow بيبني نسخة موقّعة ويحطها في مستودع الموقع (`public/app`)، ولما الموقع يتنشر الأجهزة تاخد التحديث.

الـ secrets المطلوبة في **Settings > Secrets and variables > Actions** (ما تتكتبش في أي مكان تاني):
- `KEYSTORE_BASE64`: ملف المفتاح `bukhari-upload.jks` محوّل لنص (على Windows في PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("bukhari-upload.jks")) | Set-Clipboard` وبعدين الصق في الـ secret؛ على Linux: `base64 -w0 bukhari-upload.jks`).
- `STORE_PASSWORD` و`KEY_PASSWORD`: كلمتا سر المفتاح.
- `API_BASE_URL`: رابط الموقع الحقيقي، يبدأ بـ `https://` وينتهي بـ `/`.
- `BUKHARY_PUSH_TOKEN`: token (Fine-grained) على مستودع الموقع `bukhary` بصلاحية `Contents: Read and write` فقط.

لازم المفتاح يكون نفس مفتاح النسخ المثبّتة، وإلا أندرويد بيرفض التحديث (والنشر بيفشل قبل ما يوصل للأجهزة).


```bash
./gradlew testDebugUnitTest
```

## ملاحظات
- الصلاحيات ونطاق اللجان (بالمعرّف) بتيجي من السيرفر (`/api/auth/me`)، والتطبيق بيعرض بس الإجراءات اللي السيرفر هيقبلها.
- ملفات PDF الامتحانات بتتنزل للمشرف العام ورؤساء اللجان، ضمن قائمة الامتحانات المسموحة لكل واحد، وبتتمسح من الجهاز عند الخروج.
- التعديلات اللي ما اتبعتتش ما بتتمسحش لما الجلسة تنتهي أو عند الخروج إلا لو المستخدم اختار ده. بتتبعت بس لما نفس
  المستخدم يدخل تاني، وكل تعديل معاه مفتاح `Idempotency-Key` فما يتنفذش مرتين.
- التعديلات اللي السيرفر رفضها (مثلاً تعارض) بتفضل ظاهرة في «حالة البيانات والمزامنة» لحد ما المستخدم يعيد إرسالها أو يحذفها.
- مراحل اللجنة والاستلام والحفل بتتبعت كإجراءات (`POST /api/committees/:id/actions`) والسيرفر بيطبق القواعد ويحط الوقت.
- اختبار العقد: `ContractPayloadTest` بيكتب الطلبات في `app/build/contract-payloads/android.json`؛ انسخه لـ
  `tests/contract/android-payloads.json` في مستودع الويب لما الطلبات تتغير.
- نسخة release للمستخدمين لازم تتوقع: `./gradlew assembleRelease -PrequireReleaseSigning=true -PAPI_BASE_URL=https://...`
  مع `KEYSTORE_PATH` و`STORE_PASSWORD` و`KEY_PASSWORD`، وارفع `versionCode` في كل إصدار.
