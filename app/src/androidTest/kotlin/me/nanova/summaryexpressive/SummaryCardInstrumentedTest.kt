package me.nanova.summaryexpressive

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.nanova.summaryexpressive.model.SummaryLength
import me.nanova.summaryexpressive.model.SummaryOutput
import me.nanova.summaryexpressive.ui.component.SummaryCard
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SummaryCardInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun summaryCard_rendersTitleAuthorTagsAndKeyPoints() {
        val summaryOutput = SummaryOutput(
            title = "Expressive Design in Android",
            author = "Jane Doe",
            overview = "A high-level view of M3 Expressive.",
            keyPoints = listOf("Tonal elevations", "Spring motion physics"),
            tags = listOf("Android", "Expressive"),
            summary = "Detailed summary content explaining design tokens and animations.",
            provider = "OPENAI",
            model = "gpt-4o",
            length = SummaryLength.MEDIUM,
            isYoutubeLink = false,
            isBiliBiliLink = false,
            errorReason = "Should not appear"
        )

        composeTestRule.setContent {
            SummaryExpressiveTheme {
                SummaryCard(
                    summary = summaryOutput,
                    isExpandedByDefault = true,
                    onShowSnackbar = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Expressive Design in Android").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jane Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("#Android").assertIsDisplayed()
        composeTestRule.onNodeWithText("#Expressive").assertIsDisplayed()
        composeTestRule.onNodeWithText("A high-level view of M3 Expressive.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tonal elevations").assertIsDisplayed()
        composeTestRule.onNodeWithText("Spring motion physics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Detailed summary content explaining design tokens and animations.").assertIsDisplayed()
        // errorReason must not be displayed on success
        composeTestRule.onNodeWithText("Should not appear").assertDoesNotExist()
    }

    @Test
    fun summaryCard_deduplicatesOverview_whenIdenticalOrPrefix() {
        val summaryText = "Jetpack Compose empowers developers to build native UIs faster."
        val summaryOutput = SummaryOutput(
            title = "Compose Article",
            author = "Google Android",
            overview = summaryText,
            summary = summaryText,
            keyPoints = listOf("Declarative syntax"),
            length = SummaryLength.MEDIUM,
            isYoutubeLink = false,
            isBiliBiliLink = false
        )

        composeTestRule.setContent {
            SummaryExpressiveTheme {
                SummaryCard(
                    summary = summaryOutput,
                    isExpandedByDefault = true,
                    onShowSnackbar = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Compose Article").assertIsDisplayed()
        composeTestRule.onNodeWithText("Declarative syntax").assertIsDisplayed()
        // Since overview is identical to summary, it should only appear once (in the main summary body)
        // and NOT in an "Overview" titled section
        composeTestRule.onNodeWithText("Overview").assertDoesNotExist()
    }

    @Test
    fun summaryCard_doesNotDisplayInvalidAuthor() {
        val summaryOutput = SummaryOutput(
            title = "Article Without Author",
            author = "unknown",
            summary = "Summary content goes here.",
            length = SummaryLength.MEDIUM,
            isYoutubeLink = false,
            isBiliBiliLink = false
        )

        composeTestRule.setContent {
            SummaryExpressiveTheme {
                SummaryCard(
                    summary = summaryOutput,
                    isExpandedByDefault = true,
                    onShowSnackbar = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Article Without Author").assertIsDisplayed()
        composeTestRule.onNodeWithText("unknown").assertDoesNotExist()
    }

    @Test
    fun summaryCard_doesNotDisplayLongSentenceAuthor() {
        val longAuthor = "This is a paragraph that was erroneously placed in the author field by an extraction tool."
        val summaryOutput = SummaryOutput(
            title = "Article With Paragraph Author",
            author = longAuthor,
            summary = "Real summary text.",
            length = SummaryLength.MEDIUM,
            isYoutubeLink = false,
            isBiliBiliLink = false
        )

        composeTestRule.setContent {
            SummaryExpressiveTheme {
                SummaryCard(
                    summary = summaryOutput,
                    isExpandedByDefault = true,
                    onShowSnackbar = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Article With Paragraph Author").assertIsDisplayed()
        composeTestRule.onNodeWithText(longAuthor).assertDoesNotExist()
    }

    @Test
    fun summaryCard_doesNotDisplayUrlAsAuthor() {
        val urlAuthor = "https://www.theguardian.com/profile/justinmccurry"
        val summaryOutput = SummaryOutput(
            title = "Article With URL Author",
            author = urlAuthor,
            summary = "Real summary text.",
            length = SummaryLength.MEDIUM,
            isYoutubeLink = false,
            isBiliBiliLink = false
        )

        composeTestRule.setContent {
            SummaryExpressiveTheme {
                SummaryCard(
                    summary = summaryOutput,
                    isExpandedByDefault = true,
                    onShowSnackbar = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Article With URL Author").assertIsDisplayed()
        composeTestRule.onNodeWithText(urlAuthor).assertDoesNotExist()
    }

    @Test
    fun summaryCard_rendersLlmIndicatorPill() {
        val summaryOutput = SummaryOutput(
            title = "Article With Model Pill",
            author = "Jane Doe",
            summary = "Summary text.",
            provider = "OPEN_ROUTER",
            model = "anthropic/claude-3-haiku",
            length = SummaryLength.MEDIUM,
            isYoutubeLink = false,
            isBiliBiliLink = false
        )

        composeTestRule.setContent {
            SummaryExpressiveTheme {
                SummaryCard(
                    summary = summaryOutput,
                    isExpandedByDefault = true,
                    onShowSnackbar = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Article With Model Pill").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jane Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("claude-3-haiku").assertIsDisplayed()
    }
}
