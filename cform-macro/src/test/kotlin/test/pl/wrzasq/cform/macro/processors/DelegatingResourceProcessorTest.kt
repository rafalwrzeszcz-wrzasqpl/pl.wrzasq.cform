/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2023 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.processors

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.processors.DelegatingResourceProcessor
import pl.wrzasq.cform.macro.template.PROPERTY_KEY_PROPERTIES
import pl.wrzasq.cform.macro.template.PROPERTY_KEY_TYPE
import pl.wrzasq.cform.macro.template.SECTION_RESOURCES
import pl.wrzasq.cform.macro.template.asMapAlways

class DelegatingResourceProcessorTest {
    @Test
    fun unknownHandler() {
        val input = mapOf(
            PROPERTY_KEY_TYPE to "AWS::Some::Type",
            PROPERTY_KEY_PROPERTIES to mapOf("Property" to "Value")
        )
        val processor = DelegatingResourceProcessor()

        val output = processor.process(
            mapOf(
                SECTION_RESOURCES to mapOf(
                    "Id" to input
                )
            ),
            emptyMap()
        )

        assertSame(input, asMapAlways(output[SECTION_RESOURCES])["Id"])
    }
}
