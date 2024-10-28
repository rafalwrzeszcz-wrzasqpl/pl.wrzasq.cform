/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2023 - 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import pl.wrzasq.cform.macro.TemplateProcessor
import pl.wrzasq.cform.macro.config.LambdaResourcesFactory

abstract class TemplateTest(
    private val templateParameterValues: Map<String, String> = emptyMap(),
) {
    private val objectMapper = LambdaResourcesFactory.OBJECT_MAPPER

    protected abstract val processor: TemplateProcessor

    protected fun processTemplate(scenario: String) {
        val input = objectMapper.readValue<Map<String, Any>>(
            this.javaClass.getResourceAsStream("/${scenario}.input.json"),
        )
        val output = objectMapper.readValue<Map<String, Any>>(
            this.javaClass.getResourceAsStream("/${scenario}.output.json"),
        )

        assertEquals(output, processor(input, templateParameterValues))
    }
}
