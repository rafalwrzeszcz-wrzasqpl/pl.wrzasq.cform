/*
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.edgedeploy.zip

import com.fasterxml.jackson.databind.ObjectMapper
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val BUFFER_SIZE = 1024

/**
 * Utility class for building ZIP archive stream.
 */
class ZipBuilder {
    private fun interface ContentWriter {
        /**
         * Performs write operation.
         *
         * @throws IOException When write to stream fails.
         */
        fun write()
    }

    private val output = ByteArrayOutputStream()

    private val zip = ZipOutputStream(output)

    /**
     * Creates new ZIP archive entry.
     *
     * @param name Entry stackSetName.
     * @param content Binary content of the file.
     * @throws IOException When writing content to archive fails.
     */
    fun writeEntry(name: String, content: ByteArray) {
        writeEntry(name) { zip.write(content) }
    }

    /**
     * Creates new ZIP archive entry.
     *
     * @param name Entry stackSetName.
     * @param stream Content source.
     * @throws IOException When writing content to archive fails.
     */
    fun writeEntry(name: String, stream: InputStream) {
        writeEntry(name) {
            val buffer = ByteArray(BUFFER_SIZE)
            var count: Int
            while (stream.read(buffer).also { count = it } > 0) {
                zip.write(buffer, 0, count)
            }
        }
    }

    private fun writeEntry(name: String, handler: ContentWriter) {
        zip.putNextEntry(
            ZipEntry(name)
        )
        handler.write()
        zip.closeEntry()
    }

    /**
     * Copies another archive into current one.
     *
     * @param archive Source.
     * @throws IOException When reading source archive fails.
     */
    fun copyFrom(archive: ZipInputStream) {
        // copy entire package content
        generateSequence(archive::getNextEntry).forEach { writeEntry(it.name, archive) }
    }

    /**
     * Closes active stream.
     *
     * @return Archive binary content.
     * @throws IOException When dumping ZIP stream fails.
     */
    fun dump(): ByteArray {
        zip.close()
        output.close()
        return output.toByteArray()
    }
}

/**
 * Builds deployment ZIP package.
 *
 * @param configFile Destination config filename.
 * @param config Function setup.
 * @param objectMapper JSON serializer.
 * @return ZIP file buffer.
 */
fun ResponseInputStream<GetObjectResponse>.toEdgePackage(
    configFile: String,
    config: Map<String, Any>,
    objectMapper: ObjectMapper
): ByteArray {
    val zip = ZipBuilder()
    ZipInputStream(this).use { archive ->
        zip.copyFrom(archive)

        // dump custom configuration from request
        zip.writeEntry(
            configFile,
            objectMapper.writeValueAsBytes(config)
        )
        return zip.dump()
    }
}
