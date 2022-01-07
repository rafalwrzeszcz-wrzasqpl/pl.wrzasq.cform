/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.cloudfront.invalidation

import com.amazonaws.services.lambda.runtime.Context
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import pl.wrzasq.cform.cloudfront.invalidation.model.CodePipelineEvent
import software.amazon.awssdk.services.cloudfront.CloudFrontClient
import software.amazon.awssdk.services.codepipeline.CodePipelineClient
import software.amazon.awssdk.services.codepipeline.model.FailureType
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * API key retrieval entry point.
 *
 * Required memory: 256MB.
 *
 * @param objectMapper JSON (de)serialization handler.
 * @param cloudFront CloudFront client.
 * @param codePipeline CodePipeline client.
 */
class Handler(
    private val objectMapper: ObjectMapper,
    private val cloudFront: CloudFrontClient,
    private val codePipeline: CodePipelineClient
) {
    private val logger = LoggerFactory.getLogger(Handler::class.java)

    /**
     * Handles invocation.
     *
     * @param inputStream Request input.
     * @param outputStream Output stream.
     * @param context Execution context.
     * @throws java.io.IOException When JSON loading/dumping fails.
     */
    fun handle(inputStream: InputStream, outputStream: OutputStream, context: Context) {
        val request: CodePipelineEvent = objectMapper.readValue(inputStream)

        try {
            cloudFront.createInvalidation {
                it
                    .distributionId(
                        request
                            .codePipelineJob
                            .codePipelineActionData
                            .codePipelineActionConfiguration
                            .configuration
                            .distributionId
                    )
                    .invalidationBatch { batch ->
                        batch
                            .callerReference(UUID.randomUUID().toString())
                            .paths { paths ->
                                paths
                                    .quantity(1)
                                    .items("/*")
                            }
                    }
            }

            codePipeline.putJobSuccessResult { it.jobId(request.codePipelineJob.id) }
        } catch (error: RuntimeException) {
            logger.error("Failed to issue cache invalidation: {}.", error.message, error)
            codePipeline.putJobFailureResult {
                it
                    .jobId(request.codePipelineJob.id)
                    .failureDetails { failure ->
                        failure
                            .message(error.message)
                            .type(FailureType.JOB_FAILED)
                    }
            }
        }
    }
}
