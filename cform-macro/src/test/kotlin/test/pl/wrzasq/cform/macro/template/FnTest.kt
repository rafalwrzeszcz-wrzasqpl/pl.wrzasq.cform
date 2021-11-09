/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.template

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.template.Fn
import pl.wrzasq.cform.macro.template.asMap

private const val RESOURCE_ID = "Entry"
private const val ATTRIBUTE_NAME = "Some"

class FnTest {
    private fun producer(input: String) = input

    @Test
    fun ref() {
        val output = Fn.ref(RESOURCE_ID)
        assertEquals(1, output.size)
        assertEquals("Ref", output.keys.first())
        assertEquals(RESOURCE_ID, output.values.first())
    }

    @Test
    fun getAtt() {
        val output = Fn.getAtt(RESOURCE_ID, ATTRIBUTE_NAME)
        assertEquals(1, output.size)
        assertEquals("Fn::GetAtt", output.keys.first())
        assertEquals(listOf(RESOURCE_ID, ATTRIBUTE_NAME), output.values.first())
    }

    @Test
    fun importValue() {
        val input = mapOf("Fn::Sub" to RESOURCE_ID)
        val output = Fn.importValue(input)
        assertEquals(1, output.size)
        assertEquals("Fn::ImportValue", output.keys.first())
        assertEquals(input, output.values.first())
    }

    @Test
    fun sub() {
        val input = mapOf("Fn::If" to RESOURCE_ID)
        val output = Fn.sub(input)
        assertEquals(1, output.size)
        assertEquals("Fn::Sub", output.keys.first())
        assertEquals(input, output.values.first())
    }

    @Test
    fun fnIf() {
        val condition = "HasIt"
        val output = Fn.fnIf(condition, RESOURCE_ID, ATTRIBUTE_NAME)
        assertEquals(1, output.size)
        assertEquals("Fn::If", output.keys.first())
        assertEquals(listOf(condition, RESOURCE_ID, ATTRIBUTE_NAME), output.values.first())
    }

    @Test
    fun wrapSubRef() {
        val output = Fn.wrapSub(Fn.ref("Test"), ::producer)
        assertEquals("Fn::Sub", output.keys.first())

        val value = output.values.first()
        assertEquals("\${Test}", value)
    }

    @Test
    fun wrapSubGetAttSingle() {
        val output = Fn.wrapSub(mapOf("Fn::GetAtt" to "Test.Param"), ::producer)
        assertEquals("Fn::Sub", output.keys.first())

        val value = output.values.first()
        assertEquals("\${Test.Param}", value)
    }

    @Test
    fun wrapSubGetAttComplex() {
        val output = Fn.wrapSub(Fn.getAtt("Test", "Param"), ::producer)
        assertEquals("Fn::Sub", output.keys.first())

        val value = output.values.first()
        assertEquals("\${Test.Param}", value)
    }

    @Test
    fun wrapSubImportValue() {
        val output = Fn.wrapSub(Fn.importValue("Test"), ::producer)
        assertEquals("Fn::Sub", output.keys.first())

        val value = output.values.first()
        assertEquals("\${Import:Test}", value)
    }

    @Test
    fun wrapSubNestedSubSingle() {
        val output = Fn.wrapSub(Fn.sub("\${Test}"), ::producer)
        assertEquals("Fn::Sub", output.keys.first())

        val value = output.values.first()
        assertEquals("\${Test}", value)
    }

    @Test
    fun wrapSubNestedSubComplex() {
        val input = Fn.sub(listOf("\${Test}", emptyMap<String, Any>()))
        val output = Fn.wrapSub(input, ::producer)
        assertEquals("Fn::Sub", output.keys.first())

        val value = output.values.first()
        assertTrue(value is List<*>)
        if (value is List<*>) {
            val params = asMap(value.last() ?: emptyMap<String, Any>())
            assertTrue(params.isEmpty())

            assertEquals("\${Test}", value.first())
        }
    }

    @Test
    fun wrapSubString() {
        // this is plain string not a function call - needs to be escaped
        val output = Fn.wrapSub("\${Foo}", ::producer)
        assertEquals("Fn::Sub", output.keys.first())

        val value = output.values.first()
        assertEquals("\${!Foo}", value)
    }

    @Test
    fun wrapSubMapNotCall() {
        val input = mapOf("1" to 1, "2" to 2)
        val output = Fn.wrapSub(input, ::producer)
        assertEquals("Fn::Sub", output.keys.first())

        val value = output.values.first()
        assertTrue(value is List<*>)
        if (value is List<*>) {
            val params = asMap(value.last() ?: emptyMap<String, Any>())
            assertEquals(1, params.size)

            val key = params.keys.first()
            assertEquals(input, params[key])

            assertEquals("\${$key}", value.first())
        }
    }

    @Test
    fun wrapSubDifferentType() {
        val input = listOf("1", "2")
        val output = Fn.wrapSub(input, ::producer)
        assertEquals("Fn::Sub", output.keys.first())

        val value = output.values.first()
        assertTrue(value is List<*>)
        if (value is List<*>) {
            val params = asMap(value.last() ?: emptyMap<String, Any>())
            assertEquals(1, params.size)

            val key = params.keys.first()
            assertEquals(input, params[key])

            assertEquals("\${$key}", value.first())
        }
    }
}
