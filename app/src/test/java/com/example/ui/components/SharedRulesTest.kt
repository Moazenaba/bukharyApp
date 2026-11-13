package com.example.ui.components

import com.example.data.model.StagesMap
import com.example.ui.screens.export.ReportSheets
import com.example.ui.theme.StageColorMap
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rules the app must share with the website, read from rules.json (a copy of the web's
 * tests/contract/rules.json). If this fails, a rule changed in one app only: change both apps,
 * then copy rules.json to both repositories.
 */
class SharedRulesTest {
    @Suppress("UNCHECKED_CAST")
    private val rules: Map<String, Any?> by lazy {
        val text = javaClass.classLoader!!.getResourceAsStream("rules.json")!!.bufferedReader(Charsets.UTF_8).readText()
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        Moshi.Builder().build().adapter<Map<String, Any?>>(type).fromJson(text)!!
    }

    @Suppress("UNCHECKED_CAST")
    private fun strings(key: String) = rules[key] as List<String>

    @Test
    fun stageOrder() {
        assertEquals(strings("stageOrder"), OrderedStageKeys.map { it.first })
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun stateAndLabelAfterEachStage() {
        val states = rules["stageStates"] as List<Map<String, Any?>>
        assert(states.isNotEmpty())
        for (s in states) {
            val done = s["completed"] as List<String>
            var stages = StagesMap()
            for (k in done) stages = stages.updateStage(k, true)
            val key = currentStageKey(stages)
            assertEquals("state after ${done.joinToString("+")}", s["key"], key)
            assertEquals("label after ${done.joinToString("+")}", s["label"], StageColorMap[key]?.activeLabel)
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun ceremonyStatusReportColumns() {
        val cols = rules["ceremonyStatusColumns"] as Map<String, List<String>>
        assertEquals(cols["not_held"], ReportSheets.NOT_HELD_HEADER)
        assertEquals(cols["held"], ReportSheets.HELD_HEADER)
    }
}
