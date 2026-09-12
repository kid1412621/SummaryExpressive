package me.nanova.summaryexpressive.ui.page.history

import me.nanova.summaryexpressive.ui.component.extractDomain
import me.nanova.summaryexpressive.ui.component.extractRootDomain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FaviconResolutionTest {

    @Test
    fun `extractRootDomain should correctly resolve root domains`() {
        assertEquals("medium.com", extractRootDomain("cloudwithazeem.medium.com"))
        assertEquals("medium.com", extractRootDomain("medium.com"))
        assertEquals("medium.com", extractRootDomain("www.medium.com"))
        assertEquals("google.com", extractRootDomain("blog.google.com"))
        assertEquals("google.co.uk", extractRootDomain("blog.google.co.uk"))
        assertEquals("bbc.co.uk", extractRootDomain("news.bbc.co.uk"))
        assertEquals("github.io", extractRootDomain("user.github.io"))
        assertEquals("nytimes.com", extractRootDomain("www.nytimes.com"))
    }

    @Test
    fun `favicon url format should use domain_url for https support`() {
        val domain = "cloudwithazeem.medium.com"
        val rootDomain = extractRootDomain(domain)
        val faviconUrl = "https://www.google.com/s2/favicons?domain_url=https://$domain&sz=128"
        val rootFaviconUrl = "https://www.google.com/s2/favicons?domain_url=https://$rootDomain&sz=128"

        assertEquals(
            "https://www.google.com/s2/favicons?domain_url=https://cloudwithazeem.medium.com&sz=128",
            faviconUrl
        )
        assertEquals(
            "https://www.google.com/s2/favicons?domain_url=https://medium.com&sz=128",
            rootFaviconUrl
        )
    }
}
