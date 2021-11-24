/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.template

import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.template.Fn
import pl.wrzasq.cform.macro.template.asMapAlways

private const val RESOURCE_ID = "Entry"
private const val ATTRIBUTE_NAME = "Some"
private const val VAR_NAME = "Test"
private const val TEMPLATE_STRING = "\${$VAR_NAME}"

const val CALL_REF = "Ref"
const val CALL_SUB = "Fn::Sub"
const val CALL_IF = "Fn::If"

class FnTest {
    private fun producer(input: String) = input

    @Test
    fun ref() {
        val output = Fn.ref(RESOURCE_ID)
        assertEquals(1, output.size)
        assertEquals(CALL_REF, output.keys.first())
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
        val input = mapOf(CALL_SUB to RESOURCE_ID)
        val output = Fn.importValue(input)
        assertEquals(1, output.size)
        assertEquals("Fn::ImportValue", output.keys.first())
        assertEquals(input, output.values.first())
    }

    @Test
    fun sub() {
        val input = mapOf(CALL_IF to RESOURCE_ID)
        val output = Fn.sub(input)
        assertEquals(1, output.size)
        assertEquals(CALL_SUB, output.keys.first())
        assertEquals(input, output.values.first())
    }

    @Test
    fun fnIf() {
        val condition = "HasIt"
        val output = Fn.fnIf(condition, RESOURCE_ID, ATTRIBUTE_NAME)
        assertEquals(1, output.size)
        assertEquals(CALL_IF, output.keys.first())
        assertEquals(listOf(condition, RESOURCE_ID, ATTRIBUTE_NAME), output.values.first())
    }

    @Test
    fun wrapSubRef() {
        val output = Fn.wrapSub(Fn.ref(VAR_NAME), ::producer)
        assertEquals(CALL_SUB, output.keys.first())

        val value = output.values.first()
        assertEquals(TEMPLATE_STRING, value)
    }

    @Test
    fun wrapSubGetAttSingle() {
        val output = Fn.wrapSub(mapOf("Fn::GetAtt" to "Test.Param"), ::producer)
        assertEquals(CALL_SUB, output.keys.first())

        val value = output.values.first()
        assertEquals("\${$VAR_NAME.Param}", value)
    }

    @Test
    fun wrapSubGetAttComplex() {
        val output = Fn.wrapSub(Fn.getAtt(VAR_NAME, "Param"), ::producer)
        assertEquals(CALL_SUB, output.keys.first())

        val value = output.values.first()
        assertEquals("\${$VAR_NAME.Param}", value)
    }

    @Test
    fun wrapSubImportValue() {
        val output = Fn.wrapSub(Fn.importValue(VAR_NAME), ::producer)
        assertEquals(CALL_SUB, output.keys.first())

        val value = output.values.first()
        assertEquals("\${Import:$VAR_NAME}", value)
    }

    @Test
    fun wrapSubNestedSubSingle() {
        val output = Fn.wrapSub(Fn.sub(TEMPLATE_STRING), ::producer)
        assertEquals(CALL_SUB, output.keys.first())

        val value = output.values.first()
        assertEquals(TEMPLATE_STRING, value)
    }

    @Test
    fun wrapSubNestedSubComplex() {
        val input = Fn.sub(listOf(TEMPLATE_STRING, emptyMap<String, Any>()))
        val output = Fn.wrapSub(input, ::producer)
        assertEquals(CALL_SUB, output.keys.first())

        val value = output.values.first()
        //assertInstanceOf(List::class.java, value)
        assertTrue(value is List<*>)
        if (value is List<*>) {
            val params = asMapAlways(value.last())
            assertTrue(params.isEmpty())

            assertEquals(TEMPLATE_STRING, value.first())
        }
    }

    @Test
    fun wrapSubString() {
        // this is plain string not a function call - needs to be escaped
        val output = Fn.wrapSub("\${Foo}", ::producer)
        assertEquals(CALL_SUB, output.keys.first())

        val value = output.values.first()
        assertEquals("\${!Foo}", value)
    }

    @Test
    fun wrapSubMapNotCall() {
        val input = mapOf("1" to 1, "2" to 2)
        val output = Fn.wrapSub(input, ::producer)
        assertEquals(CALL_SUB, output.keys.first())

        val value = output.values.first()
        //assertInstanceOf(List::class.java, value)
        assertTrue(value is List<*>)
        if (value is List<*>) {
            val params = asMapAlways(value.last())
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
        assertEquals(CALL_SUB, output.keys.first())

        val value = output.values.first()
        //assertInstanceOf(List::class.java, value)
        assertTrue(value is List<*>)
        if (value is List<*>) {
            val params = asMapAlways(value.last())
            assertEquals(1, params.size)

            val key = params.keys.first()
            assertEquals(input, params[key])

            assertEquals("\${$key}", value.first())
        }
    }
}
