package me.nanova.summaryexpressive.ui.page

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import me.nanova.summaryexpressive.R
import me.nanova.summaryexpressive.ui.Nav
import me.nanova.summaryexpressive.ui.component.LogoIcon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    onDoneAndNavigate: (destinationRoute: Nav) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(ImageDecoderDecoder.Factory())
        }
        .build()

    Surface {
        Box(
            modifier = modifier.fillMaxSize(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingStepPage(
                page = page,
                imageLoader = imageLoader,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val isLast = pagerState.currentPage == pagerState.pageCount - 1
            val isFirst = pagerState.currentPage == 0

            TextButton(onClick = {
                if (isFirst) onDone()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            }) {
                Text(text = if (isFirst) stringResource(id = R.string.skip) else "Previous")
            }

            if (isLast) {
                Button(onClick = {
                    onDoneAndNavigate(Nav.Settings(highlight = "ai"))
                }) {
                    Text("Setup AI")
                }
            }

            FilledTonalButton(
                onClick = {
                    if (isLast) onDone()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            ) {
                Text(
                    text = if (isLast) stringResource(id = R.string.finishButton)
                    else "Next"
                )
            }
        }
    }
    }
}

const val CDN =
    "https://cdn.jsdelivr.net/gh/kid1412621/SummaryExpressive@refs/heads/main/.github/screenshots"

@Composable
private fun OnboardingStepPage(
    page: Int,
    imageLoader: ImageLoader,
) {

    when (page) {
        0 -> OnboardingStepContent(
            titleRes = R.string.welcome,
            descriptionRes = R.string.welcomeDescription,
            image = {
                LogoIcon(
                    size = 220.dp,
                    isRotating = true,
                    modifier = Modifier.padding(bottom = 50.dp)
                )
            },
            verticalArrangement = Arrangement.Center
        )

        1 -> OnboardingStepContent(
            descriptionRes = R.string.instruction,
            image = { OnboardingImage("$CDN/screen1.webp", imageLoader) }
        )

        2 -> OnboardingStepContent(
            descriptionRes = R.string.instructionsShare,
            image = { OnboardingImage("$CDN/screen2.webp", imageLoader) }
        )

        3 -> OnboardingStepContent(
            descriptionRes = R.string.instructionsHistory,
            image = { OnboardingImage("$CDN/screen3.webp", imageLoader) },
        )
    }
}

@Composable
private fun OnboardingImage(imageRes: String, imageLoader: ImageLoader) {
    val context = LocalContext.current
    val placeholder = ColorPainter(Color.LightGray)
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val maxHeight = with(density) { (containerSize.height * 0.8f).toDp() }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageRes)
            .crossfade(true)
            .build(),
        imageLoader = imageLoader,
        placeholder = placeholder,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .clip(shape = RoundedCornerShape(20.dp))
            .heightIn(max = maxHeight)
    )
}

@Composable
private fun OnboardingStepContent(
    image: @Composable (() -> Unit)? = null,
    @StringRes titleRes: Int? = null,
    @StringRes descriptionRes: Int? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 15.dp, vertical = 40.dp),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        image?.invoke()

        titleRes?.let {
            Text(
                text = stringResource(id = it),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        descriptionRes?.let {
            Text(
                text = stringResource(id = it),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun OnboardingStepContentPreview() {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(ImageDecoderDecoder.Factory())
        }
        .build()

    OnboardingStepContent(
        descriptionRes = R.string.instruction,
        image = { OnboardingImage("$CDN/screen1.webp", imageLoader) }
    )
}


@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen(onDone = {}, onDoneAndNavigate = {})
}
