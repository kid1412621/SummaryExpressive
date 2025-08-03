package me.nanova.summaryexpressive.llm

import ai.koog.prompt.dsl.Prompt

enum class SummaryLength {
    SHORT, MEDIUM, LONG
}

fun createSummarizationPrompt(length: SummaryLength): Prompt {
    val lengthInstruction = when (length) {
        SummaryLength.SHORT -> "a few sentences(better within 100 characters)"
        SummaryLength.MEDIUM -> "two to three paragraphs"
        SummaryLength.LONG -> "a detailed, multi-paragraph summary"
    }

    return Prompt.build("summarizer-prompt") {
        system(
            """
            You are an expert summarization assistant. Your task is to produce a clear, concise, and accurate summary of the provided text.
            The summary should be about $lengthInstruction long.

            - If the text is an article, focus on the main arguments, key points, and conclusions.
            - If the text is a video transcript, focus on the key topics and speakers' points.
            - If the text is from a document, focus on the core information and purpose.

            Structure the output in well-structured markdown for readability.
            If you receive an error message as input, do not try to summarize it. Instead, repeat the error message back to the user verbatim.
            """.trimIndent()
        )
    }
}