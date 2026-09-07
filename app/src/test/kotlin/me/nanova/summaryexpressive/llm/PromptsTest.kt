package me.nanova.summaryexpressive.llm

import me.nanova.summaryexpressive.model.SummaryLength
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PromptsTest {

    @Test
    fun `test default prompt replaces placeholders for short length`() {
        val prompt = generateFinalPromptString(
            length = SummaryLength.SHORT,
            showLength = true,
            useContentLanguage = true,
            appLanguage = "English",
            isAppendMode = true,
            customBasePrompt = "",
            additionalSystemPrompt = ""
        )

        assertFalse(prompt.contains("[Length instructions]"))
        assertFalse(prompt.contains("[Language instructions]"))
        assertTrue(prompt.contains("The summary should be about a few sentences(better within 100 words) long"))
        assertTrue(prompt.contains("Identify Content Language"))
    }

    @Test
    fun `test default prompt replaces placeholders for long length`() {
        val prompt = generateFinalPromptString(
            length = SummaryLength.LONG,
            showLength = true,
            useContentLanguage = false,
            appLanguage = "English",
            isAppendMode = true,
            customBasePrompt = "",
            additionalSystemPrompt = ""
        )

        assertFalse(prompt.contains("[Length instructions]"))
        assertFalse(prompt.contains("[Language instructions]"))
        assertTrue(prompt.contains("The summary should be about a detailed, multi-paragraph summary long"))
        assertTrue(prompt.contains("The summary should be written in English."))
    }

    @Test
    fun `test default prompt hides length instruction when showLength is false`() {
        val prompt = generateFinalPromptString(
            length = SummaryLength.MEDIUM,
            showLength = false,
            useContentLanguage = true,
            appLanguage = "English",
            isAppendMode = true,
            customBasePrompt = "",
            additionalSystemPrompt = ""
        )

        assertFalse(prompt.contains("[Length instructions]"))
        assertFalse(prompt.contains("The summary should be about"))
    }

    @Test
    fun `test custom prompt without placeholders appends length and language instructions`() {
        val customPrompt = "Custom instructions for summarizing."
        val prompt = generateFinalPromptString(
            length = SummaryLength.SHORT,
            showLength = true,
            useContentLanguage = false,
            appLanguage = "French",
            isAppendMode = false,
            customBasePrompt = customPrompt,
            additionalSystemPrompt = ""
        )

        assertTrue(prompt.startsWith("Custom instructions for summarizing."))
        assertTrue(prompt.contains("The summary should be written in French."))
        assertTrue(prompt.contains("The summary should be about a few sentences(better within 100 words) long"))
    }

    @Test
    fun `test append mode adds additional instructions`() {
        val prompt = generateFinalPromptString(
            length = SummaryLength.MEDIUM,
            showLength = true,
            useContentLanguage = true,
            appLanguage = "English",
            isAppendMode = true,
            customBasePrompt = "",
            additionalSystemPrompt = "Focus on financial statistics."
        )

        assertTrue(prompt.contains("Additional Instructions:"))
        assertTrue(prompt.contains("Focus on financial statistics."))
    }
}
