package me.nanova.summaryexpressive.llm

import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import me.nanova.summaryexpressive.model.SummaryLength

val defaultSystemPromptPlaceholder = """
    You are an expert summarization assistant. Your task is to produce a clear, concise, and accurate summary of the provided text.
    [Language instructions]
    The summary should be about [Length instructions] long, and must not exceed the length of the original content.

    - If the text is an article, focus on the main arguments, key points, and conclusions.
    - If the text is a video transcript, focus on the key topics and speakers' points.
    - If the text is from a document, focus on the core information and purpose.
    
    Include the main point and any conclusion if relevant.
    Do not use any headings, introductions, or metacommentary.
    No markdown formatting or special characters.
    Highlight the main concepts or viewpoints with bulleted or numbered list.
    If you receive an error message as input, do not try to summarize it. Instead, repeat the error message back to the user verbatim.
""".trimIndent()


fun generateFinalPromptString(
    length: SummaryLength,
    showLength: Boolean,
    useContentLanguage: Boolean,
    appLanguage: String,
    isAppendMode: Boolean,
    customBasePrompt: String,
    additionalSystemPrompt: String,
): String {
    val lengthInstruction = when (length) {
        SummaryLength.SHORT -> "a few sentences(better within 100 words)"
        SummaryLength.MEDIUM -> "two to three paragraphs"
        SummaryLength.LONG -> "a detailed, multi-paragraph summary"
    }

    val languageInstruction = if (useContentLanguage) {
        """
        **Mandatory Procedure:**
        1.  **Identify Content Language:** First, determine the original language of the 'content' field in the user's request. This is the SOLE source for language identification. Ignore tool call details for this step.
        2.  **Use the identified language for summarization**".
        """
    } else "The summary should be written in $appLanguage."

    val baseToUse = if (!isAppendMode && customBasePrompt.isNotBlank()) {
        customBasePrompt
    } else {
        defaultSystemPromptPlaceholder
    }

    return buildString {
        append(baseToUse)

        if (isAppendMode && additionalSystemPrompt.isNotBlank()) {
            append("\n\nAdditional Instructions:\n")
            append(additionalSystemPrompt)
        }

        append("\n\n")
        append(languageInstruction)

        if (showLength) {
            append("\n")
            append("The summary should be about $lengthInstruction long, and must not exceed the length of the original content.")
        }
    }
}

fun createSummarizationPrompt(
    length: SummaryLength,
    showLength: Boolean,
    useContentLanguage: Boolean,
    appLanguage: String,
    isAppendMode: Boolean = true,
    customBasePrompt: String = "",
    additionalSystemPrompt: String = "",
): Prompt {
    val finalPrompt = generateFinalPromptString(
        length,
        showLength,
        useContentLanguage,
        appLanguage,
        isAppendMode,
        customBasePrompt,
        additionalSystemPrompt
    )

    return prompt("summarizer-prompt") {
        system(finalPrompt)
    }
}