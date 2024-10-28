/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2023 - 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro

import com.amazonaws.services.lambda.runtime.Context
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import pl.wrzasq.cform.macro.model.CloudFormationMacroRequest
import pl.wrzasq.cform.macro.model.CloudFormationMacroResponse
import java.io.InputStream
import java.io.OutputStream

typealias TemplateProcessor = (Map<String, Any>, Map<String, Any>) -> Map<String, Any>

/**
 * API key retrieval entry point.
 *
 * Required memory: 256MB.
 *
 * @param objectMapper JSON (de)serialization handler.
 * @param templateProcessors Template processors.
 */
class Handler(
    private val objectMapper: ObjectMapper,
    private val templateProcessors: Collection<TemplateProcessor>,
) {
    /**
     * Handles invocation.
     *
     * @param inputStream Request input.
     * @param outputStream Output stream.
     * @param context Execution context.
     * @throws java.io.IOException When JSON loading/dumping fails.
     */
    fun handle(inputStream: InputStream, outputStream: OutputStream, context: Context) {
        val request: CloudFormationMacroRequest = objectMapper.readValue(inputStream)

        objectMapper.writeValue(
            outputStream,
            CloudFormationMacroResponse(
                requestId = request.requestId,
                fragment =  templateProcessors.fold(request.fragment) { state, fn ->
                    fn(state, request.templateParameterValues)
                },
            ),
        )
    }
}
