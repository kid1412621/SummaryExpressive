package me.nanova.summaryexpressive.ui.page.settings

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val BILI_LOGIN_URL = "https://passport.bilibili.com/h5-app/passport/login"
private const val SESSDATA_COOKIE_NAME = "SESSDATA"

// BiliBili SESSDATA cookie has a 6-month expiration time.
// We can't get the exact expiration from CookieManager, so we approximate it.
private const val SESSDATA_EXPIRATION_APPROX = 6 * 30 * 24 * 60 * 60 * 1000L

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BiliBiliLoginSheetContent(
    onDismiss: () -> Unit,
    onSessDataFound: (sessData: String, expires: Long) -> Unit,
) {
    fun checkAndNotifySessData() {
        val sessData = findSessDataCookie()
        if (sessData != null) {
            val expires = System.currentTimeMillis() + SESSDATA_EXPIRATION_APPROX
            onSessDataFound(sessData, expires)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000.milliseconds)
            val sessData = findSessDataCookie()
            if (sessData != null) {
                val expires = System.currentTimeMillis() + SESSDATA_EXPIRATION_APPROX
                onSessDataFound(sessData, expires)
                break
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BiliBili Login",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { context ->
                WebView(context).apply {
                    val webView = this
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        textZoom = 100
                    }
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(webView, true)
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            super.onPageCommitVisible(view, url)
                            injectLayoutFix(view)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            injectLayoutFix(view)
                            checkAndNotifySessData()
                        }

                        override fun doUpdateVisitedHistory(
                            view: WebView?,
                            url: String?,
                            isReload: Boolean,
                        ) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            injectLayoutFix(view)
                            checkAndNotifySessData()
                        }
                    }
                    loadUrl(BILI_LOGIN_URL)
                }
            }
        )
    }
}

private fun injectLayoutFix(view: WebView?) {
    val js = """
        (function() {
            function applyStyle() {
                var style = document.getElementById('bili-layout-fix');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'bili-layout-fix';
                    style.textContent = `
                        .login-wrap {
                            min-height: auto !important;
                            padding-bottom: 2.5rem !important;
                        }
                        .explain-tips {
                            position: relative !important;
                            bottom: auto !important;
                            left: auto !important;
                            margin-top: 1.2rem !important;
                            margin-bottom: 1.5rem !important;
                            padding: 0 0.64rem !important;
                        }
                    `;
                    (document.head || document.documentElement).appendChild(style);
                }
            }
            applyStyle();
            if (!window.__biliFixObserver) {
                window.__biliFixObserver = new MutationObserver(applyStyle);
                window.__biliFixObserver.observe(document.documentElement, { childList: true, subtree: true });
            }
        })();
    """.trimIndent()
    view?.evaluateJavascript(js, null)
}

private fun findSessDataCookie(): String? {
    val cookieManager = CookieManager.getInstance()
    val domains = listOf(
        "https://passport.bilibili.com",
        "https://www.bilibili.com",
        "https://bilibili.com"
    )
    for (domain in domains) {
        val cookies = cookieManager.getCookie(domain) ?: continue
        val sessData = cookies.split(';').firstNotNullOfOrNull { cookie ->
            val parts = cookie.trim().split('=', limit = 2)
            if (parts.size == 2 && parts[0] == SESSDATA_COOKIE_NAME && parts[1].isNotBlank()) {
                parts[1]
            } else {
                null
            }
        }
        if (sessData != null) return sessData
    }
    return null
}
