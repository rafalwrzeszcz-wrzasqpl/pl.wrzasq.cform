/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.apigateway

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.macro.apigateway.ApiGateway
import pl.wrzasq.cform.macro.apigateway.ApiMethod

@ExtendWith(MockKExtension::class)
class ApiMethodTest {
    @MockK
    lateinit var api: ApiGateway

    @Test
    fun noAuthorizer() {
        every { api.authorizers } returns mutableMapOf()

        assertThrows<IllegalStateException> {
            ApiMethod(
                api,
                "Test",
                "Parent",
                "GET",
                mapOf("Authorizer" to "Not")
            )
        }
    }
}
