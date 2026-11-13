package com.example.ui.components

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Short messages from screens that have no view-model of their own (a missing WhatsApp app, a PDF
 * that could not be made). MainActivity shows them in the app's snackbar, the same place as every
 * other message, instead of a system toast.
 */
object UiMessages {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun show(message: String) {
        _messages.tryEmit(message)
    }
}
