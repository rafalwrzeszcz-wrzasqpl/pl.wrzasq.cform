/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.processors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
//import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.processors.FnToolkit
import pl.wrzasq.cform.macro.template.Fn
import pl.wrzasq.cform.macro.template.asMapAlways
import test.pl.wrzasq.cform.macro.template.CALL_REF
import test.pl.wrzasq.cform.macro.template.CALL_SUB

private const val IMPORT_KEY = "import0"

class FnToolkitTest {
    @Test
    fun canHandle() {
        val toolkit = FnToolkit()

        assertTrue(toolkit.canHandle(CALL_SUB))
        assertFalse(toolkit.canHandle(CALL_REF))
    }

    @Test
    fun resolveString() {
        val toolkit = FnToolkit()

        val output = toolkit.expand("" to "\${Import:Test}").values.first()

        //assertInstanceOf(List::class.java, output)
        assertTrue(output is List<*>)
        if (output is List<*>) {
            val params = asMapAlways(output.last())

            assertEquals("\${$IMPORT_KEY}", output.first())
            assertTrue(IMPORT_KEY in params)
            assertEquals(Fn.importValue("Test"), params[IMPORT_KEY])
        }
    }

    @Test
    fun resolveStruct() {
        val toolkit = FnToolkit()

        val output = toolkit.expand("" to listOf("\${Import:Test}", mapOf("Foo" to "Bar"))).values.first()

        //assertInstanceOf(List::class.java, output)
        assertTrue(output is List<*>)
        if (output is List<*>) {
            val params = asMapAlways(output.last())

            assertEquals("\${$IMPORT_KEY}", output.first())
            assertTrue("Foo" in params)
            assertTrue(IMPORT_KEY in params)
            assertEquals(Fn.importValue("Test"), params[IMPORT_KEY])
        }
    }

    @Test
    fun resolveNoReference() {
        val toolkit = FnToolkit()

        val input = "Import:Test"
        val output = toolkit.expand("" to input)
        assertEquals(input, output.values.first())
    }
}
