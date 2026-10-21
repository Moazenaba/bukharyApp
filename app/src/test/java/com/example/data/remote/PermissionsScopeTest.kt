package com.example.data.remote

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PermissionsScopeTest {
    @Before
    fun setUp() = Permissions.clear()

    @After
    fun tearDown() = Permissions.clear()

    @Test
    fun oneActionDoesNotUnlockAnotherOne() {
        Permissions.actions = mapOf("exams.write" to true, "committees.delete" to false)
        assertTrue(Permissions.can("exams.write"))
        assertFalse(Permissions.can("committees.delete"))
        assertFalse(Permissions.can("unknown.action"))
    }

    @Test
    fun missingExamScopeGrantsNoExam() {
        Permissions.actions = mapOf("exams.file" to true)
        Permissions.applyServerScope(emptyList<String>(), null)
        assertFalse(Permissions.canOpenExam("exam-1"))
    }

    @Test
    fun listedExamScopeOpensOnlyThoseExams() {
        Permissions.actions = mapOf("exams.file" to true)
        Permissions.applyServerScope(emptyList<String>(), listOf("exam-1"))
        assertTrue(Permissions.canOpenExam("exam-1"))
        assertFalse(Permissions.canOpenExam("exam-2"))
    }

    @Test
    fun allExamsAndAllCommitteesOnlyWhenTheServerSaysSo() {
        Permissions.applyServerScope("all", "all")
        assertNull(Permissions.committeeScope)
        assertNull(Permissions.examScope)
        assertTrue(Permissions.inScope("any-committee"))
    }

    @Test
    fun missingCommitteeScopeGrantsNoCommittee() {
        Permissions.applyServerScope(null, null)
        assertFalse(Permissions.inScope("c1"))
    }
}
