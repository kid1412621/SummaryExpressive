package me.nanova.summaryexpressive.llm

import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import me.nanova.summaryexpressive.model.SummaryLength

val defaultSystemPromptPlaceholder = """
    You are an expert summarization assistant. Your task is to produce a clear, high-quality, and objective summary of the provided content.
    [Language instructions]
    The summary should be about [Length instructions] long, and must not exceed the length of the original content.

    Content-Specific Guidelines:
    - Articles & Web Content: Synthesize the core theme, key arguments, significant evidence or data, and primary conclusions.
    - Video Transcripts: Extract essential topics, discussions, and speaker insights. Completely ignore sponsorships, self-promotions, advertisements, channel subscribe calls-to-action, and conversational filler.
    - Documents & Reports: Highlight core findings, methodology or background, major decisions, and actionable outcomes.
    - Plain Text & Notes: Distill the primary ideas directly and cohesively.

    Style & Structure Rules:
    - Jump directly into the summary without introductory meta-phrases (e.g., avoid "This article discusses", "In this video", "The author writes").
    - Maintain an objective, neutral tone and adhere strictly to facts mentioned in the text—do not extrapolate, speculate, or introduce external knowledge.
    - Provide a well-structured narrative body, highlighting key takeaways with clear bullet points where helpful.
    - If the input text is an error message, access denial notice, or contains insufficient substance to summarize, output the issue clearly without attempting to hallucinate a summary.
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
        2.  **Use the identified language for summarization**.
        """.trimIndent()
    } else {
        "The summary should be written in $appLanguage."
    }

    var baseToUse = if (!isAppendMode && customBasePrompt.isNotBlank()) {
        customBasePrompt
    } else {
        defaultSystemPromptPlaceholder
    }

    val hasLanguagePlaceholder = baseToUse.contains("[Language instructions]")
    val hasLengthPlaceholder = baseToUse.contains("[Length instructions]")

    if (hasLanguagePlaceholder) {
        baseToUse = baseToUse.replace("[Language instructions]", languageInstruction)
    }

    if (hasLengthPlaceholder) {
        if (showLength) {
            baseToUse = baseToUse.replace("[Length instructions]", lengthInstruction)
        } else {
            baseToUse = baseToUse.lines()
                .filterNot { it.contains("[Length instructions]") }
                .joinToString("\n")
        }
    }

    return buildString {
        append(baseToUse)

        if (isAppendMode && additionalSystemPrompt.isNotBlank()) {
            append("\n\nAdditional Instructions:\n")
            append(additionalSystemPrompt)
        }

        if (!hasLanguagePlaceholder) {
            append("\n\n")
            append(languageInstruction)
        }

        if (showLength && !hasLengthPlaceholder) {
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