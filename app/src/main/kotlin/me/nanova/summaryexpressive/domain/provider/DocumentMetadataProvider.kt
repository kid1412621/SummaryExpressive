package me.nanova.summaryexpressive.domain.provider

interface DocumentMetadataProvider {
    suspend fun getFileName(uriString: String): String?
}
