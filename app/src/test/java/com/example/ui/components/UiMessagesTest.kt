package com.example.ui.components

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class UiMessagesTest {
    @Test
    fun `a message reaches the screen that is collecting`() = runBlocking {
        val received = async(start = CoroutineStart.UNDISPATCHED) { UiMessages.messages.first() }
        UiMessages.show("لا يوجد واتساب")
        assertEquals("لا يوجد واتساب", withTimeout(2_000) { received.await() })
    }
}
