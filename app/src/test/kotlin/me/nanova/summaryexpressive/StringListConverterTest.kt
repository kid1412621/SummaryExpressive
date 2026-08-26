package me.nanova.summaryexpressive

import me.nanova.summaryexpressive.data.converters.StringListConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class StringListConverterTest {

    private val converter = StringListConverter()

    @Test
    fun `test serialization and deserialization of list`() {
        val list = listOf("gpt-4o", "gpt-4o-mini", "custom-model")
        val json = converter.fromStringList(list)
        val result = converter.toStringList(json)
        assertEquals(list, result)
    }

    @Test
    fun `test null handling`() {
        assertNull(converter.fromStringList(null))
        assertNull(converter.toStringList(null))
        assertNull(converter.toStringList(""))
        assertNull(converter.toStringList("   "))
    }

    @Test
    fun `test fallback to comma separated string`() {
        val commaString = "gpt-4o, gpt-4o-mini, custom-model"
        val expected = listOf("gpt-4o", "gpt-4o-mini", "custom-model")
        val result = converter.toStringList(commaString)
        assertEquals(expected, result)
    }
}
