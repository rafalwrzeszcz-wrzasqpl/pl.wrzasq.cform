/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.template

import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.template.PROPERTY_KEY_CONDITION
import pl.wrzasq.cform.macro.template.PROPERTY_KEY_DEPENDSON
import pl.wrzasq.cform.macro.template.PROPERTY_KEY_PROPERTIES
import pl.wrzasq.cform.macro.template.PROPERTY_KEY_TYPE
import pl.wrzasq.cform.macro.template.asDefinition
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.asMapAlways
import pl.wrzasq.cform.macro.template.build
import pl.wrzasq.cform.macro.template.createResource
import pl.wrzasq.cform.macro.template.mapSelected
import pl.wrzasq.cform.macro.template.mapValuesOnly
import pl.wrzasq.cform.macro.template.popProperty
import pl.wrzasq.cform.macro.template.rebuildResource

private const val OTHER = "Other"

private val input = ResourceDefinition(
    id = "Test1",
    type = "AWS::Test",
    properties = mapOf(
        "A" to "B",
        "C" to "D",
        "E" to "F"
    ),
    condition = "HasIt",
    dependsOn = listOf(OTHER)
)

@ExtendWith(MockKExtension::class)
class UtilsTest {
    @MockK
    lateinit var propertyHandler: (Any) -> Unit

    @Test
    fun asMapMap() {
        val output = asMap(
            mapOf(
                "Foo" to "Bar",
                5 to 3,
                "empty" to null
            )
        )

        assertFalse("empty" in output)
        assertTrue("5" in output)
        assertEquals(3, output["5"])
    }

    @Test
    fun asMapNotMap() {
        assertTrue(asMap("").isEmpty())
    }

    @Test
    fun asMapAlwaysValue() {
        val output = asMap(
            mapOf(
                "Some" to "Value"
            )
        )

        assertTrue("Some" in output)
        assertEquals("Value", output["Some"])
    }

    @Test
    fun asMapAlwaysNull() {
        assertTrue(asMapAlways(null).isEmpty())
    }

    @Test
    fun popPropertyExists() {
        every { propertyHandler(any()) } just runs

        val output = input.properties.popProperty("A", propertyHandler)

        assertFalse("A" in output)

        verify { propertyHandler("B") }
    }

    @Test
    fun popPropertyDefault() {
        every { propertyHandler(any()) } just runs

        val fallback = "Test"
        input.properties.popProperty(OTHER, propertyHandler, fallback)

        verify { propertyHandler(fallback) }
    }

    @Test
    fun popPropertyMissing() {
        input.properties.popProperty(OTHER, propertyHandler)

        verify { propertyHandler wasNot called }
    }

    @Test
    fun mapSelectedMulti() {
        val output = input.properties.mapSelected(
            "A" to { 5 },
            "C" to { 6 },
            OTHER to { 7 }
        )
        assertEquals(5, output["A"])
        assertEquals(6, output["C"])
        assertEquals("F", output["E"])
        assertFalse(OTHER in output)
    }

    @Test
    fun mapSelectedSingle() {
        val output = input.properties.mapSelected("A") { 5 }
        assertEquals(5, output["A"])
        assertEquals("D", output["C"])
    }

    @Test
    fun mapValuesOnly() {
        val output = input.properties.mapValuesOnly { "${it}X" }
        assertEquals("BX", output["A"])
    }

    @Test
    fun rebuildResourceOverrides() {
        val output = rebuildResource(input.build(), mapOf("Foo" to "Bar"))
        val properties = asMapAlways(output[PROPERTY_KEY_PROPERTIES])

        assertFalse("A" in properties)
        assertTrue("Foo" in properties)
        assertEquals("Bar", properties["Foo"])
    }

    @Test
    fun createResourceFull() {
        val output = createResource(input)

        assertEquals(input.id, output.first)
        assertEquals(input.type, output.second[PROPERTY_KEY_TYPE])
        assertTrue(PROPERTY_KEY_CONDITION in output.second)
        assertEquals(input.condition, output.second[PROPERTY_KEY_CONDITION])
        assertTrue(PROPERTY_KEY_DEPENDSON in output.second)
        assertEquals(input.dependsOn, output.second[PROPERTY_KEY_DEPENDSON])
        assertTrue(PROPERTY_KEY_PROPERTIES in output.second)
        assertEquals(input.properties, output.second[PROPERTY_KEY_PROPERTIES])
    }

    @Test
    fun createResourceWithoutOptional() {
        val output = createResource(ResourceDefinition(id = input.id, type = input.type))

        assertEquals(input.id, output.first)
        assertEquals(input.type, output.second[PROPERTY_KEY_TYPE])
        assertFalse(PROPERTY_KEY_CONDITION in output.second)
        assertFalse(PROPERTY_KEY_DEPENDSON in output.second)
        assertFalse(PROPERTY_KEY_PROPERTIES in output.second)
    }

    @Test
    fun asDefinitionWithInputs() {
        val output = asDefinition(input.id, mapOf(
            PROPERTY_KEY_TYPE to input.type,
            PROPERTY_KEY_CONDITION to input.condition,
            PROPERTY_KEY_DEPENDSON to input.dependsOn,
            PROPERTY_KEY_PROPERTIES to input.properties
        ))

        assertEquals(input.id, output.id)
        assertEquals(input.type, output.type)
        assertEquals(input.condition, output.condition)
        assertEquals(input.dependsOn, output.dependsOn)
        assertEquals(input.properties, output.properties)
    }

    @Test
    fun asDefinitionFallbacks() {
        val output = asDefinition(input.id, emptyMap<String, Any>())

        assertEquals(input.id, output.id)
        assertEquals("", output.type)
        assertNull(output.condition)
        assertTrue(output.dependsOn.isEmpty())
        assertTrue(output.properties.isEmpty())
    }

    @Test
    fun asDefinitionStrangeDependsOn() {
        val output = asDefinition(input.id, mapOf(PROPERTY_KEY_DEPENDSON to 1))
        assertTrue(output.dependsOn.isEmpty())
    }

    @Test
    fun buildOfDefinition() {
        val output = input.build()

        assertEquals(input.id, output.first)
        assertEquals(input.type, output.second[PROPERTY_KEY_TYPE])
        assertTrue(PROPERTY_KEY_DEPENDSON in output.second)
        assertEquals("D", asMapAlways(output.second[PROPERTY_KEY_PROPERTIES])["C"])
    }

    @Test
    fun popPropertyOfDefinition() {
        every { propertyHandler(any()) } just runs

        val output = input.popProperty("A", propertyHandler)

        assertEquals(input.id, output.id)
        assertFalse(output.dependsOn.isEmpty())
        assertFalse("A" in output.properties)
        assertTrue("C" in output.properties)

        verify { propertyHandler("B") }
    }
}
