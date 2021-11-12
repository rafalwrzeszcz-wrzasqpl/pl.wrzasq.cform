/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.apigateway

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.wrzasq.cform.macro.apigateway.ApiGatewayManager
import pl.wrzasq.cform.macro.template.Fn

class ApiGatewayManagerTest {
    @Test
    fun expandUnknownApi() {
        assertThrows<IllegalStateException> { ApiGatewayManager().expand("Ref" to "RestApi:Unknown") }
    }

    @Test
    fun expandComplexArgument() {
        val input = Fn.sub(
            listOf(
                Fn.fnIf("Check", "\${Foo}", "\${Bar}")
            )
        )

        assertEquals(input, ApiGatewayManager().expand("Fn::Sub" to input.values.first()))
    }
}
