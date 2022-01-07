/*
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.edgedeploy.zip

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.wrzasq.cform.resource.aws.edgedeploy.zip.ZipBuilder
import java.util.zip.ZipInputStream
import java.util.Scanner

private const val FILENAME = "test.txt"
private const val PAYLOAD = "test"

class ZipBuilderTest {
    @Test
    fun writeEntryFromBytes() {
        val zip = ZipBuilder()
        zip.writeEntry(
            FILENAME,
            "test".toByteArray()
        )
        val buffer = zip.dump()
        val stream = ZipInputStream(buffer.inputStream())
        val entry = stream.nextEntry
        val scanner = Scanner(stream)
        assertEquals(FILENAME, entry?.name)
        assertEquals(PAYLOAD, scanner.next())
    }

    @Test
    fun writeEntryFromStream() {
        val zip = ZipBuilder()
        zip.writeEntry(
            FILENAME,
            "test".byteInputStream()
        )
        val buffer = zip.dump()
        val stream = ZipInputStream(buffer.inputStream())
        val entry = stream.nextEntry
        val scanner = Scanner(stream)
        assertEquals(FILENAME, entry?.name)
        assertEquals(PAYLOAD, scanner.next())
    }

    @Test
    fun copyFrom() {
        val zip = ZipBuilder()
        zip.writeEntry(
            FILENAME,
            "test".byteInputStream()
        )
        val buffer = zip.dump()
        val destination = ZipBuilder()
        destination.copyFrom(ZipInputStream(buffer.inputStream()))
        val stream = ZipInputStream(buffer.inputStream())
        val entry = stream.nextEntry
        val scanner = Scanner(stream)
        assertEquals(FILENAME, entry?.name)
        assertEquals(PAYLOAD, scanner.next())
    }
}
