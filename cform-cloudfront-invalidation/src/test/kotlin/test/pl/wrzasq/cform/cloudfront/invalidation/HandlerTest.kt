/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.cloudfront.invalidation

import com.amazonaws.services.lambda.runtime.Context
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.cloudfront.invalidation.Handler
import pl.wrzasq.cform.cloudfront.invalidation.model.CodePipelineActionConfiguration
import pl.wrzasq.cform.cloudfront.invalidation.model.CodePipelineActionData
import pl.wrzasq.cform.cloudfront.invalidation.model.CodePipelineEvent
import pl.wrzasq.cform.cloudfront.invalidation.model.CodePipelineJob
import pl.wrzasq.cform.cloudfront.invalidation.model.Request
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.cloudfront.CloudFrontClient
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationResponse
import software.amazon.awssdk.services.codepipeline.CodePipelineClient
import software.amazon.awssdk.services.codepipeline.model.PutJobFailureResultRequest
import software.amazon.awssdk.services.codepipeline.model.PutJobFailureResultResponse
import software.amazon.awssdk.services.codepipeline.model.PutJobSuccessResultRequest
import software.amazon.awssdk.services.codepipeline.model.PutJobSuccessResultResponse
import java.io.InputStream
import java.io.OutputStream
import java.util.function.Consumer

private const val JOB_ID = "123"
private const val DISTRIBUTION_ID = "ABC"

val exception: AwsServiceException = AwsServiceException.builder()
    .awsErrorDetails(
        AwsErrorDetails.builder()
            .sdkHttpResponse(
                SdkHttpResponse.builder()
                    .build()
            )
            .build()
    )
    .build()

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
    lateinit var cloudFront: CloudFrontClient

    @MockK
    lateinit var codePipeline: CodePipelineClient

    @Test
    fun handle() {
        every {
            objectMapper.readValue(inputStream, any<TypeReference<CodePipelineEvent>>())
        } returns CodePipelineEvent(
            codePipelineJob = CodePipelineJob(
                id = JOB_ID,
                codePipelineActionData = CodePipelineActionData(
                    codePipelineActionConfiguration = CodePipelineActionConfiguration(
                        configuration = Request(
                            distributionId = DISTRIBUTION_ID
                        )
                    )
                )
            )
        )

        every {
            cloudFront.createInvalidation(any<Consumer<CreateInvalidationRequest.Builder>>())
        } returns CreateInvalidationResponse.builder().build()

        every {
            codePipeline.putJobSuccessResult(any<Consumer<PutJobSuccessResultRequest.Builder>>())
        } returns PutJobSuccessResultResponse.builder().build()

        val handler = Handler(objectMapper, cloudFront, codePipeline)
        handler.handle(inputStream, outputStream, context)

        val captor = slot<Consumer<CreateInvalidationRequest.Builder>>()

        verify { cloudFront.createInvalidation(capture(captor)) }

        val builder = CreateInvalidationRequest.builder()
        captor.captured.accept(builder)

        val request = builder.build()
        assertEquals(DISTRIBUTION_ID, request.distributionId())
        assertEquals(1, request.invalidationBatch().paths().items().size)
        assertEquals("/*", request.invalidationBatch().paths().items().first())

        verify {
            codePipeline.putJobFailureResult(any<Consumer<PutJobFailureResultRequest.Builder>>()) wasNot called
        }

        val result = slot<Consumer<PutJobSuccessResultRequest.Builder>>()
        verify { codePipeline.putJobSuccessResult(capture(result)) }
        result.captured.accept(PutJobSuccessResultRequest.builder())
    }

    @Test
    fun handleFailure() {
        every {
            objectMapper.readValue(inputStream, any<TypeReference<CodePipelineEvent>>())
        } returns CodePipelineEvent(
            codePipelineJob = CodePipelineJob(
                id = JOB_ID,
                codePipelineActionData = CodePipelineActionData(
                    codePipelineActionConfiguration = CodePipelineActionConfiguration(
                        configuration = Request(
                            distributionId = DISTRIBUTION_ID
                        )
                    )
                )
            )
        )

        every {
            cloudFront.createInvalidation(any<Consumer<CreateInvalidationRequest.Builder>>())
        } throws exception

        every {
            codePipeline.putJobFailureResult(any<Consumer<PutJobFailureResultRequest.Builder>>())
        } returns PutJobFailureResultResponse.builder().build()

        val handler = Handler(objectMapper, cloudFront, codePipeline)
        handler.handle(inputStream, outputStream, context)

        val captor = slot<Consumer<CreateInvalidationRequest.Builder>>()

        verify { cloudFront.createInvalidation(capture(captor)) }

        val builder = CreateInvalidationRequest.builder()
        captor.captured.accept(builder)

        val request = builder.build()
        assertEquals(DISTRIBUTION_ID, request.distributionId())
        assertEquals(1, request.invalidationBatch().paths().items().size)
        assertEquals("/*", request.invalidationBatch().paths().items().first())

        verify {
            codePipeline.putJobSuccessResult(any<Consumer<PutJobSuccessResultRequest.Builder>>()) wasNot called
        }

        val result = slot<Consumer<PutJobFailureResultRequest.Builder>>()
        verify { codePipeline.putJobFailureResult(capture(result)) }
        result.captured.accept(PutJobFailureResultRequest.builder())
    }
}
