/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.edgedeploy.action

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.edgedeploy.action.ReadHandler
import pl.wrzasq.cform.resource.aws.edgedeploy.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.edgedeploy.model.ResourceModel
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse
import software.amazon.awssdk.services.lambda.model.ListVersionsByFunctionRequest
import software.amazon.awssdk.services.lambda.model.ListVersionsByFunctionResponse
import software.amazon.awssdk.services.lambda.paginators.ListVersionsByFunctionIterable
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Credentials
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext
import java.util.function.Function

const val NAME = "Test"
const val ARN = "arn:aws:test"
const val DESCRIPTION = "Aloha"
const val ROLE_ARN = "arn:aws:iam"
const val RUNTIME = "nodejs14.x"
const val HANDLER = "index.hande"
const val MEMORY = 128
const val TIMEOUT = 30
const val PACKAGE_BUCKET = "some-repo"
const val PACKAGE_KEY = "sam/edge.zip"
const val TAG_NAME = "Foo"
const val TAG_VALUE = "Bar"

const val PACKAGE_ZIP = "test@zip"

private val credentials = Credentials("ID", "secret", "test")

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
class ReadHandlerTest {
    @MockK
    lateinit var logger: LoggerProxy

    @MockK
    lateinit var factory: ResourcesFactory

    @MockK
    lateinit var proxyClient: ProxyClient<LambdaClient>

    @MockK
    lateinit var listVersions: ListVersionsByFunctionIterable

    @Test
    fun handleRequest() {
        val model = ResourceModel().apply {
            name = NAME
            packageBucket = PACKAGE_BUCKET
            packageKey = PACKAGE_KEY
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionRequest::class),
                any<Function<GetFunctionRequest, GetFunctionResponse>>()
            )
        } returns GetFunctionResponse.builder()
            .configuration {
                it
                    .functionName(NAME)
                    .functionArn(ARN)
                    .description(DESCRIPTION)
                    .role(ROLE_ARN)
                    .runtime(RUNTIME)
                    .handler(HANDLER)
                    .memorySize(MEMORY)
                    .timeout(TIMEOUT)
            }
            .tags(mapOf(TAG_NAME to TAG_VALUE))
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeIterableV2(
                ofType(ListVersionsByFunctionRequest::class),
                any<Function<ListVersionsByFunctionRequest, ListVersionsByFunctionIterable>>()
            )
        } returns listVersions

        every {
            listVersions.iterator()
        } returns mutableListOf(
            ListVersionsByFunctionResponse.builder()
                .versions(
                    FunctionConfiguration.builder()
                        .version("\$LATEST")
                        .functionArn("${ARN}:\$LATEST")
                        .build(),
                    FunctionConfiguration.builder()
                        .version("1")
                        .functionArn("${ARN}:1")
                        .build(),
                    FunctionConfiguration.builder()
                        .version("2")
                        .functionArn("${ARN}:2")
                        .build(),
                    FunctionConfiguration.builder()
                        .version("10")
                        .functionArn("${ARN}:10")
                        .build()
                )
                .build()
        ).iterator()


        val result = ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(NAME, result.resourceModel?.name)
        assertEquals("${ARN}:10", result.resourceModel?.arn)
        assertEquals(DESCRIPTION, result.resourceModel?.description)
        assertEquals(ROLE_ARN, result.resourceModel?.roleArn)
        assertEquals(RUNTIME, result.resourceModel?.runtime)
        assertEquals(HANDLER, result.resourceModel?.handler)
        assertEquals(MEMORY, result.resourceModel?.memory)
        assertEquals(TIMEOUT, result.resourceModel?.timeout)
        assertEquals(PACKAGE_BUCKET, result.resourceModel?.packageBucket)
        assertEquals(PACKAGE_KEY, result.resourceModel?.packageKey)
        assertNotNull(result.resourceModel?.tags?.find { it.key == TAG_NAME && it.value == TAG_VALUE })
    }

    @Test
    fun handleRequestFailed() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionRequest::class),
                any<Function<GetFunctionRequest, GetFunctionResponse>>()
            )
        } throws exception

        assertThrows<CfnGeneralServiceException> {
            ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }

    @Test
    fun handleRequestNull() {
        val request = ResourceHandlerRequest<ResourceModel?>()

        val proxy = initializeProxy(logger, factory, proxyClient)

        assertThrows<IllegalArgumentException> {
            ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }
}

fun initializeProxy(
    logger: LoggerProxy,
    factory: ResourcesFactory,
    proxyClient: ProxyClient<LambdaClient>,
    proxyS3: ProxyClient<S3Client>? = null
): AmazonWebServicesClientProxy {
    val proxy = AmazonWebServicesClientProxy(logger, credentials) { 1L }

    every { logger.log(any()) } just runs
    every { factory.getLambdaClient(proxy) } returns proxyClient
    every { proxyClient.client().serviceName() } returns "lambda"

    if (proxyS3 != null) {
        every { factory.getS3Client(proxy) } returns proxyS3
    }

    return proxy
}
