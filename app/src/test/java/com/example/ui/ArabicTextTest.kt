package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * A blind find-and-replace of «ده» → «هذا» once broke words that end in «ده» («ستجده» became «ستجهذا»,
 * «بموعده» became «بموعهذا»). This keeps any such glued word out of the screens.
 */
class ArabicTextTest {
    private val allowed = setOf("بهذا", "لهذا", "فهذا", "كهذا", "وهذا", "أهذا")

    @Test fun `no word has «هذا» glued inside it`() {
        val root = File("src/main/java").takeIf { it.exists() } ?: File("app/src/main/java")
        val bad = root.walkTopDown().filter { it.extension == "kt" }.flatMap { f ->
            Regex("[ء-ي]*هذا[ء-ي]*").findAll(f.readText()).map { it.value }
                .filter { it != "هذا" && it !in allowed }.map { "${f.name}: $it" }
        }.toList()
        assertEquals(emptyList<String>(), bad)
    }
}
