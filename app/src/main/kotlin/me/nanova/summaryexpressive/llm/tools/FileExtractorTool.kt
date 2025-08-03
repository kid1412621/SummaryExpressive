package me.nanova.summaryexpressive.llm.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
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
import kotlinx.serialization.serializer
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
            // Open the PDF file
            val pdfRenderer =
                PdfRenderer(context.contentResolver.openFileDescriptor(uri, "r")!!)

            // Initialize the text recognizer
            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            // Initialize the StringBuilder
            val extractedText = StringBuilder()

            // Iterate through the pages of the PDF
            for (pageNumber in 0 until pdfRenderer.pageCount) {
                // Get the page as an image
                val page = pdfRenderer.openPage(pageNumber)
                val pageImage = createBitmap(page.width, page.height)
                page.render(pageImage, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // Create an input image from the page image
                val inputImage = InputImage.fromBitmap(pageImage, 0)

                // Recognize text from the page image
                val result = textRecognizer.process(inputImage).await()
                extractedText.append(result.text)

                // Close the page
                page.close()
            }

            // Close the PDF file
            pdfRenderer.close()

            // Return the extracted text
            extractedText.toString()
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
data class File(val fileUriString: String) : ToolArgs

class FileExtractorTool(private val context: Context) : SimpleTool<File>() {
    override val argsSerializer = serializer<File>()
    override val descriptor = ToolDescriptor(
        name = "extract_text_from_file_uri",
        description = "Extracts text from a file (PDF, DOCX, image) given its content URI.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "fileUriString",
                description = "The content URI of the file.",
                type = ToolParameterType.String
            )
        )
    )

    override suspend fun doExecute(args: File): String {
        return try {
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

            extractor?.extract(context, uri)
                ?: "Error: Unsupported file type for URI: $uri. Mime type: $mimeType, Filename: $filename"
        } catch (e: Exception) {
            "Error extracting text from file: ${e.message}"
        }
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