package com.example.ui.screens.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** The join form must refuse what the server refuses, before anything is sent. */
class JoinRequestValidationTest {
    @Test
    fun `password needs eight characters with letters and digits`() {
        assertNotNull(passwordProblem("abc123"))
        assertNotNull(passwordProblem("abcdefgh"))
        assertNotNull(passwordProblem("12345678"))
        assertNull(passwordProblem("abcd1234"))
        assertNull(passwordProblem("كلمةسر12"))
    }

    @Test
    fun `Arabic-Indic digits do not count as the digit a password needs`() {
        // The server only accepts 0-9 here, so the form must say so instead of failing later.
        assertNotNull(passwordProblem("كلمةسر١٢"))
    }

    @Test
    fun `phones may use Arabic digits, spaces and dashes`() {
        assertNull(joinRequestProblem("معاذ", "٠١٠٩٢٨٢٠٨٠٢", "", "abcd1234"))
        assertNull(joinRequestProblem("معاذ", "+20 109-282-0802", "", "abcd1234"))
        assertEquals("رقم الهاتف غير صحيح", joinRequestProblem("معاذ", "01x9", "", "abcd1234"))
    }

    @Test
    fun `email is optional but must be valid when given`() {
        assertNull(joinRequestProblem("معاذ", "0109282080", "", "abcd1234"))
        assertEquals("البريد الإلكتروني غير صحيح", joinRequestProblem("معاذ", "0109282080", "not-an-email", "abcd1234"))
    }

    @Test
    fun `required fields are checked first`() {
        assertEquals("الاسم ورقم الجوال وكلمة المرور حقول مطلوبة", joinRequestProblem("", "0109282080", "", "abcd1234"))
    }
}
