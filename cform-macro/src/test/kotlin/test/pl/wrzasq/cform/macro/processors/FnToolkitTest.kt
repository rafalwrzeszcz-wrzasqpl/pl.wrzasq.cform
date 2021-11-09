/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.processors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.processors.FnToolkit
import pl.wrzasq.cform.macro.template.Fn
import pl.wrzasq.cform.macro.template.asMap

class FnToolkitTest {
    @Test
    fun canHandle() {
        val toolkit = FnToolkit()

        assertTrue(toolkit.canHandle("Fn::Sub"))
        assertFalse(toolkit.canHandle("Ref"))
    }

    @Test
    fun resolveString() {
        val toolkit = FnToolkit()

        val output = toolkit.expand("" to "\${Import:Test}").values.first()

        assertTrue(output is List<*>)
        if (output is List<*>) {
            val params = asMap(output.last() ?: emptyMap<String, Any>())

            assertEquals("\${import0}", output.first())
            assertTrue("import0" in params)
            assertEquals(Fn.importValue("Test"), params["import0"])
        }
    }

    @Test
    fun resolveStruct() {
        val toolkit = FnToolkit()

        val output = toolkit.expand("" to listOf("\${Import:Test}", mapOf("Foo" to "Bar"))).values.first()

        assertTrue(output is List<*>)
        if (output is List<*>) {
            val params = asMap(output.last() ?: emptyMap<String, Any>())

            assertEquals("\${import0}", output.first())
            assertTrue("Foo" in params)
            assertTrue("import0" in params)
            assertEquals(Fn.importValue("Test"), params["import0"])
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
