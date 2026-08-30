package com.example

import com.example.data.local.Converters
import com.example.model.Dict
import com.example.model.Subtask
import com.example.model.Terminology
import org.junit.Assert.*
import org.junit.Test

class ColdCacheUnitTest {

    @Test
    fun testSubtasksConverter() {
        val converters = Converters()
        val originalSubtasks = listOf(
            Subtask(id = "sub-1", text = "Initialize Core", done = true),
            Subtask(id = "sub-2", text = "Sync Buffer", done = false)
        )

        val json = converters.fromSubtaskList(originalSubtasks)
        assertNotNull(json)
        val deserialized = converters.toSubtaskList(json)
        assertEquals(2, deserialized.size)
        assertEquals("Initialize Core", deserialized[0].text)
        assertTrue(deserialized[0].done)
        assertEquals("Sync Buffer", deserialized[1].text)
        assertFalse(deserialized[1].done)
    }

    @Test
    fun testDictionaryTranslations() {
        val systemRam = Dict.get(Terminology.SYSTEM, "ram")
        val humanRam = Dict.get(Terminology.HUMAN, "ram")

        assertEquals("ACTIVE_RAM", systemRam)
        assertEquals("В работе", humanRam)
    }
}
