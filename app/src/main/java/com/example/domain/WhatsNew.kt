package com.example.domain

/**
 * When the app should tell the person it has just been updated: once, the first time the new
 * version opens, and never on a fresh install or on the next opens of the same version.
 */
object WhatsNew {
    const val TITLE = "تم تحديث التطبيق"
    const val MESSAGE = "تم الحصول على آخر نسخة محدثة من التطبيق وتم تثبيته لتنعم بتجربة أكثر سلاسة ومحدثة بالكامل."

    /** A new install is told apart from an update by the phone's own record: it was installed long before it was last updated. */
    private const val UPDATE_GAP_MS = 60_000L

    /**
     * @param storedVersionCode the version that was running the last time the app opened (null: never recorded,
     *   which is the first run of this feature, or a fresh install)
     */
    fun shouldAnnounce(storedVersionCode: Int?, currentVersionCode: Int, firstInstallTime: Long, lastUpdateTime: Long): Boolean =
        if (storedVersionCode != null) currentVersionCode > storedVersionCode
        else lastUpdateTime - firstInstallTime > UPDATE_GAP_MS
}
