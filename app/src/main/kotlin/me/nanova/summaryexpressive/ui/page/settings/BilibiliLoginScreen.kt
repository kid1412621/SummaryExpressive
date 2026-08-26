package me.nanova.summaryexpressive.ui.page.settings

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val BILI_LOGIN_URL = "https://passport.bilibili.com/login"
private const val SESSDATA_COOKIE_NAME = "SESSDATA"
private const val BILI_COOKIE_DOMAIN = "https://www.bilibili.com"

// BiliBili SESSDATA cookie has a 6-month expiration time.
// We can't get the exact expiration from CookieManager, so we approximate it.
private const val SESSDATA_EXPIRATION_APPROX = 6 * 30 * 24 * 60 * 60 * 1000L

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BiliBiliLoginSheetContent(
    onDismiss: () -> Unit,
    onSessDataFound: (sessData: String, expires: Long) -> Unit,
) {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val defaultHeight = with(density) { (containerSize.height * 0.7f).toDp() }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "BiliBili Login",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(defaultHeight),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            val sessData = findSessDataCookie()
                            if (sessData != null) {
                                val expires =
                                    System.currentTimeMillis() + SESSDATA_EXPIRATION_APPROX
                                onSessDataFound(sessData, expires)
                            }
                        }
                    }
                    loadUrl(BILI_LOGIN_URL)
                }
            }
        )
    }
}

private fun findSessDataCookie(): String? {
    val cookies = CookieManager.getInstance().getCookie(BILI_COOKIE_DOMAIN) ?: return null
    return cookies.split(';').firstNotNullOfOrNull { cookie ->
        val parts = cookie.trim().split('=', limit = 2)
        if (parts.size == 2 && parts[0] == SESSDATA_COOKIE_NAME) {
            parts[1]
        } else {
            null
        }
    }
}
