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
import pl.wrzasq.cform.macro.apigateway.ApiGateway

private const val PROPERTY_RESOURCES = "Resources"
private const val UNKNOWN = "Unknown"

class ApiGatewayTest {
    @Test
    fun getValidatorBodyOnly() {
        val validator = buildApiGateway().getValidator("BODY_ONLY").generateResource()

        assertEquals(true, validator.properties["ValidateRequestBody"])
        assertEquals(false, validator.properties["ValidateRequestParameters"])
    }

    @Test
    fun getValidatorInvalid() {
        assertThrows<IllegalArgumentException> { buildApiGateway().getValidator("INVALID") }
    }

    @Test
    fun initTreeIllegalCharacter() {
        assertThrows<java.lang.IllegalArgumentException> {
            buildApiGateway(
                mapOf(
                    PROPERTY_RESOURCES to mapOf(
                        "/this" to mapOf(
                            "Illegal" to true
                        )
                    )
                )
            )
        }
    }

    @Test
    fun resolveUnknownAuthorizer() {
        assertThrows<IllegalArgumentException> { buildApiGateway().resolve(listOf("Authorizer", UNKNOWN)) }
    }

    @Test
    fun resolveUnknownValidator() {
        assertThrows<IllegalArgumentException> { buildApiGateway().resolve(listOf("Validator", UNKNOWN)) }
    }

    @Test
    fun resolveUnknownModel() {
        assertThrows<IllegalArgumentException> { buildApiGateway().resolve(listOf("Model", UNKNOWN)) }
    }

    @Test
    fun resolveUnknownResource() {
        assertThrows<IllegalArgumentException> { buildApiGateway().resolve(listOf("Resource", UNKNOWN)) }
    }

    @Test
    fun resolveResource() {
        val api = buildApiGateway(
            mapOf(
                PROPERTY_RESOURCES to mapOf(
                    "/{name}" to emptyMap<String, Any>()
                )
            )
        )

        assertEquals("ApiGatewayTestApiResourceName", api.resolve(listOf("Resource", "/%name%")))
    }

    @Test
    fun resolveUnknownMethod() {
        assertThrows<IllegalArgumentException> { buildApiGateway().resolve(listOf("Method", UNKNOWN)) }
    }

    @Test
    fun resolveMethod() {
        val api = buildApiGateway(
            mapOf(
                PROPERTY_RESOURCES to mapOf(
                    "/{name}" to mapOf(
                        "@GET" to emptyMap<String, Any>()
                    )
                )
            )
        )

        assertEquals("ApiGatewayTestApiMethodGETName", api.resolve(listOf("Method", "GET/%name%")))
    }

    @Test
    fun resolveUnknown() {
        assertThrows<IllegalArgumentException> { buildApiGateway().resolve(listOf("Something", UNKNOWN)) }
    }

    private fun buildApiGateway(input: Map<String, Any> = emptyMap()) = ApiGateway("TestApi", input)
}
