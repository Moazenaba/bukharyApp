package com.example.ui.simple

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.ui.components.UiMessages

/** Opens the phone's dialer with the number typed in. */
fun callPhone(context: Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        UiMessages.show("لا يوجد تطبيق اتصال على هذا الجهاز")
    }
}

/** Opens a link (WhatsApp, a sheet) in whatever app handles it. */
fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        UiMessages.show("لا يوجد تطبيق يفتح هذا الرابط")
    }
}
