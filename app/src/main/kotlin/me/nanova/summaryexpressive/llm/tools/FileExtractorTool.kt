package me.nanova.summaryexpressive.llm.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.util.Xml
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.nanova.summaryexpressive.model.ExtractedContent
import org.xmlpull.v1.XmlPullParser
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.zip.ZipInputStream

private interface TextExtractor {
    suspend fun extract(context: Context, uri: Uri): String
}

private object PdfTextExtractor : TextExtractor {
    override suspend fun extract(context: Context, uri: Uri): String =
        withContext(Dispatchers.IO) {
            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val extractedText = StringBuilder()

            context.contentResolver.openFileDescriptor(uri, "r")?.use { parcelFileDescriptor ->
                PdfRenderer(parcelFileDescriptor).use { pdfRenderer ->
                    for (pageNumber in 0 until pdfRenderer.pageCount) {
                        pdfRenderer.openPage(pageNumber).use { page ->
                            val pageImage = createBitmap(page.width, page.height)
                            page.render(
                                pageImage,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )

                            // Try OCR first
                            val inputImage = InputImage.fromBitmap(pageImage, 0)
                            val ocrResult = textRecognizer.process(inputImage).await()
                            var pageText = ocrResult.text.trim()

                            // fallback to get content directly from text layer
                            if (pageText.isBlank()) {
                                try {
                                    val textContents =
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                                            page.getTextContents()
                                        } else {
                                            // maybe can use this: https://developer.android.com/jetpack/androidx/releases/pdf
                                            TODO()
                                        }

                                    if (textContents.isNotEmpty()) {
                                        val pageLayerText =
                                            textContents.joinToString(" ") { it.text }.trim()
                                        if (pageLayerText.isNotBlank()) {
                                            pageText = pageLayerText
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w(
                                        "PdfTextExtractor",
                                        "Failed to extract text from PDF's text layer (might be API limitation or other issue)",
                                        e
                                    )
                                    // Ignore, fallback only
                                }
                            }

                            if (pageText.isNotBlank()) {
                                extractedText.append(pageText)
                                extractedText.append("\n")
                            }
                        }
                    }
                }
            } ?: throw FileNotFoundException("Cannot open PDF file descriptor for URI: $uri")

            val resultText = extractedText.toString().trim()
            resultText
        }
}

private object DocxTextExtractor : TextExtractor {
    override suspend fun extract(context: Context, uri: Uri): String =
        withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw FileNotFoundException("Can't open InputStream for the URI: $uri")

            inputStream.use { stream ->
                ZipInputStream(stream).use { zis ->
                    generateSequence { zis.nextEntry }
                        .find { it.name == "word/document.xml" }
                        ?.let {
                            return@withContext parseDocxXml(zis)
                        }
                }
            }

            throw FileNotFoundException("word/document.xml not found in the DOCX file.")
        }

    private fun parseDocxXml(xmlInputStream: InputStream): String {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(xmlInputStream, null)
        }

        val extractedText = StringBuilder()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "w:t" && parser.next() == XmlPullParser.TEXT) {
                        extractedText.append(parser.text)
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "w:p") {
                        extractedText.append("\n")
                    }
                }
            }
            eventType = parser.next()
        }
        return extractedText.toString().trim()
    }
}

private object ImageTextExtractor : TextExtractor {
    override suspend fun extract(context: Context, uri: Uri): String =
        withContext(Dispatchers.IO) {
            // Initialize the text recognizer
            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            // Load the image from the URI
            val inputImage = InputImage.fromFilePath(context, uri)

            // Recognize text from the image
            val result = textRecognizer.process(inputImage).await()

            // Return the extracted text
            result.text
        }
}

@Serializable
data class File(
    @property:LLMDescription("The content URI of the file.") val fileUriString: String,
)

class FileExtractorTool(private val context: Context) : Tool<File, ExtractedContent>(
    argsType = typeToken<File>(),
    resultType = typeToken<ExtractedContent>(),
    name = "extract_text_from_file_uri",
    description = "Extracts text from a file (PDF, DOCX, image) given its content URI."
) {

    override suspend fun execute(args: File): ExtractedContent {
        val uri = args.fileUriString.toUri()
        val filename = getFileName(context, uri)
        val mimeType = context.contentResolver.getType(uri)

        val extractor: TextExtractor? = when {
            filename.endsWith(".pdf", ignoreCase = true) -> PdfTextExtractor
            filename.endsWith(".docx", ignoreCase = true) -> DocxTextExtractor
            mimeType?.startsWith("image/") == true ||
                    filename.endsWith(".jpg", ignoreCase = true) ||
                    filename.endsWith(".jpeg", ignoreCase = true) ||
                    filename.endsWith(".png", ignoreCase = true)
            -> ImageTextExtractor
            else -> null
        }

        val content = extractor?.extract(context, uri)
            ?: throw Exception("Unsupported file type for URI: $uri. Mime type: $mimeType, Filename: $filename")

        if (content.isBlank()) {
            throw Exception("Extracted text from file is empty.")
        }

        return ExtractedContent(filename, "File System", content)
    }

}

suspend fun getFileName(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    var name = ""
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        it.moveToFirst()
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index != -1) {
            name = cursor.getString(index)
        }
    }
    name
}
