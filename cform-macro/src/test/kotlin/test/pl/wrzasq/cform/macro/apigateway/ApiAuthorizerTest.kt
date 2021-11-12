/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.apigateway

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.macro.apigateway.ApiAuthorizer
import pl.wrzasq.cform.macro.apigateway.ApiGateway

@ExtendWith(MockKExtension::class)
class ApiAuthorizerTest {
    @MockK
    lateinit var api: ApiGateway

    @Test
    fun authorizationType() {
        assertEquals(
            "COGNITO_USER_POOLS",
            ApiAuthorizer(api, "Foo", mapOf("Type" to "COGNITO_USER_POOLS")).authorizationType
        )
        assertEquals(
            "CUSTOM",
            ApiAuthorizer(api, "Bar", mapOf("Type" to "TOKEN")).authorizationType
        )
    }
}
