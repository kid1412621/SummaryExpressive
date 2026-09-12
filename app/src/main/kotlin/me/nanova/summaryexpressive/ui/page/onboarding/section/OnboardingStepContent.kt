package me.nanova.summaryexpressive.ui.page.onboarding.section

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.ui.component.LogoIcon
import me.nanova.summaryexpressive.ui.theme.SummaryExpressiveTheme

const val CDN =
    "https://cdn.jsdelivr.net/gh/kid1412621/SummaryExpressive@refs/heads/main/.github/screenshots"

const val ONBOARDING_PAGE_COUNT = 4

@Composable
fun OnboardingStepPage(
    page: Int,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
) {
    when (page) {
        0 -> OnboardingStepContent(
            modifier = modifier,
            titleRes = R.string.welcome,
            descriptionRes = R.string.welcomeDescription,
            image = {
                LogoIcon(
                    size = 200.dp,
                    isRotating = true,
                )
            },
            verticalArrangement = Arrangement.Center
        )

        1 -> OnboardingStepContent(
            modifier = modifier,
            descriptionRes = R.string.instruction,
            image = { OnboardingImage("$CDN/screen1.webp", imageLoader) },
            verticalArrangement = Arrangement.Center
        )

        2 -> OnboardingStepContent(
            modifier = modifier,
            descriptionRes = R.string.instructionsShare,
            image = { OnboardingImage("$CDN/screen2.webp", imageLoader) },
            verticalArrangement = Arrangement.Center
        )

        3 -> OnboardingStepContent(
            modifier = modifier,
            descriptionRes = R.string.instructionsHistory,
            image = { OnboardingImage("$CDN/screen3.webp", imageLoader) },
            verticalArrangement = Arrangement.Center
        )
    }
}

@Composable
fun OnboardingImage(
    imageRes: String,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHighest

    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
            .fillMaxHeight()
            .aspectRatio(640f / 1422f)
            .padding(vertical = 8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageRes)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            placeholder = ColorPainter(placeholderColor),
            error = ColorPainter(placeholderColor),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraLarge)
        )
    }
}

@Composable
fun OnboardingStepContent(
    modifier: Modifier = Modifier,
    image: @Composable (() -> Unit)? = null,
    @StringRes titleRes: Int? = null,
    @StringRes descriptionRes: Int? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (image != null) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                image()
            }
        }

        titleRes?.let {
            Text(
                text = stringResource(id = it),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        descriptionRes?.let {
            Text(
                text = stringResource(id = it),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingStepContentPreview() {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(ImageDecoderDecoder.Factory())
            }
            .build()
    }

    SummaryExpressiveTheme {
        OnboardingStepContent(
            descriptionRes = R.string.instruction,
            image = { OnboardingImage("$CDN/screen1.webp", imageLoader) }
        )
    }
}
