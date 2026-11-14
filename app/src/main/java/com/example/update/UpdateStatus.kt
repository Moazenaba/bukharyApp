package com.example.update

/** Why the app is, or is not, going to update: every outcome of an update check, in words the person can act on. */
enum class UpdateStatus {
    UP_TO_DATE,
    /** A newer, verified APK is downloaded and can be installed. */
    READY,
    /** A newer APK is downloaded but the app may not install apps yet. */
    NEEDS_PERMISSION,
    /** The published APK is signed with another key: Android refuses it as an update, so it has to be installed by hand once. */
    WRONG_SIGNATURE,
    WRONG_APP,
    NOT_NEWER,
    UNREACHABLE,
    DOWNLOAD_FAILED,
    CHECKSUM_MISMATCH,
    NOT_CONFIGURED
}

data class UpdateReport(
    val status: UpdateStatus,
    /** The installed version name. */
    val installed: String,
    /** The published version name, when there is one. */
    val latest: String = "",
    /** Where to download the published APK (for the by-hand install). */
    val apkUrl: String = "",
    /** The technical reason, for the support line. */
    val detail: String = ""
)

object UpdateText {
    fun title(r: UpdateReport): String = when (r.status) {
        UpdateStatus.UP_TO_DATE -> "التطبيق على آخر نسخة (${r.installed})."
        UpdateStatus.READY -> "في تحديث جديد ${r.latest} جاهز للتثبيت."
        UpdateStatus.NEEDS_PERMISSION -> "في تحديث ${r.latest}، لكن لازم تسمح للتطبيق بتثبيت التحديثات (مرة واحدة)."
        UpdateStatus.WRONG_SIGNATURE ->
            "التحديث ${r.latest} موقّع بمفتاح مختلف عن نسختك، فأندرويد مش هيقبله كتحديث. نزّل النسخة الجديدة من الموقع وثبّتها فوق القديمة مرة واحدة."
        UpdateStatus.WRONG_APP -> "الملف المنشور مش تطبيق المسابقة. بلّغ المسؤول."
        UpdateStatus.NOT_NEWER -> "الملف المنشور مش أحدث من نسختك (${r.installed})."
        UpdateStatus.UNREACHABLE -> "ما قدرتش أوصل لسيرفر التحديثات. اتأكد من الإنترنت وجرّب تاني."
        UpdateStatus.DOWNLOAD_FAILED -> "فشل تحميل التحديث ${r.latest}. جرّب تاني."
        UpdateStatus.CHECKSUM_MISMATCH -> "ملف التحديث وصل تالف. جرّب تاني."
        UpdateStatus.NOT_CONFIGURED -> "التطبيق ده مش مضبوط على سيرفر تحديثات."
    }

    /** True when the person can do something about it right now with a button. */
    fun actionable(s: UpdateStatus): Boolean = s == UpdateStatus.READY || s == UpdateStatus.NEEDS_PERMISSION || s == UpdateStatus.WRONG_SIGNATURE
}

object UpdateDecision {
    /** The published version is a real upgrade: a higher code and an APK to get. */
    fun isNewer(installedCode: Int, publishedCode: Int, apk: String): Boolean = publishedCode > installedCode && apk.isNotBlank()

    /** The permission dialog is asked again once a day (not only once forever) while an update waits. */
    fun shouldAskPermission(lastAskedAt: Long, now: Long, dayMs: Long = 24L * 60 * 60 * 1000): Boolean = lastAskedAt <= 0L || now - lastAskedAt >= dayMs
}
