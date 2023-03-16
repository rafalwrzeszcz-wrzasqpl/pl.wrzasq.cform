/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2023 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro

import com.amazonaws.services.lambda.runtime.Context
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.macro.Handler
import pl.wrzasq.cform.macro.TemplateProcessor
import pl.wrzasq.cform.macro.model.CloudFormationMacroRequest
import pl.wrzasq.cform.macro.model.CloudFormationMacroResponse
import java.io.InputStream
import java.io.OutputStream

@ExtendWith(MockKExtension::class)
class HandlerTest {
    @MockK
    lateinit var inputStream: InputStream

    @MockK
    lateinit var outputStream: OutputStream

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var objectMapper: ObjectMapper

    @MockK
    lateinit var templateProcessor1: TemplateProcessor

    @MockK
    lateinit var templateProcessor2: TemplateProcessor

    @Test
    fun handle() {
        val id = "123"

        val input = mapOf("Foo" to "Bar")
        val intermediate = mapOf("Test" to "Yes")
        val output = mapOf("Wrzasq.pl" to "CForm")
        val params = mapOf("Project" to "IVMS")

        every {
            objectMapper.readValue(inputStream, any<TypeReference<CloudFormationMacroRequest>>())
        } returns CloudFormationMacroRequest(
            requestId = id,
            fragment = input,
            templateParameterValues = params,
        )

        every { templateProcessor1(input, params) } returns intermediate
        every { templateProcessor2(intermediate, params) } returns output

        val captor = slot<CloudFormationMacroResponse>()
        every { objectMapper.writeValue(outputStream, capture(captor)) } just runs

        val handler = Handler(objectMapper, listOf(templateProcessor1, templateProcessor2))
        handler.handle(inputStream, outputStream, context)

        verify { templateProcessor1(input, params) }
        verify { templateProcessor2(intermediate, params) }

        val response = captor.captured
        assertEquals(id, response.requestId)
        assertEquals(output, response.fragment)
    }
}
