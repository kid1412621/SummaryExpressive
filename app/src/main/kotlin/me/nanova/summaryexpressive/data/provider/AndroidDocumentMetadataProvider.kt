package me.nanova.summaryexpressive.data.provider

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import me.nanova.summaryexpressive.domain.provider.DocumentMetadataProvider
import me.nanova.summaryexpressive.llm.tools.getFileName
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidDocumentMetadataProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : DocumentMetadataProvider {
    override suspend fun getFileName(uriString: String): String? {
        return getFileName(context, uriString.toUri())
    }
}
