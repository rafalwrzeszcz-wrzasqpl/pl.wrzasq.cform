/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.processors

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.processors.ApiGatewayDefinition

class ApiGatewayDefinitionTest {
    @Test
    fun processNoNeed() {
        val processor = ApiGatewayDefinition()
        val input = emptyMap<String, Any>()

        assertSame(input, processor.process(input))
    }
}
