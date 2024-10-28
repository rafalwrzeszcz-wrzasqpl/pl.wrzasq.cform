/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.passwordpolicy.action

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.passwordpolicy.action.ReadHandler
import pl.wrzasq.cform.resource.aws.passwordpolicy.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.passwordpolicy.model.ResourceModel
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.iam.IamClient
import software.amazon.awssdk.services.iam.model.GetAccountPasswordPolicyRequest
import software.amazon.awssdk.services.iam.model.GetAccountPasswordPolicyResponse
import software.amazon.awssdk.services.iam.model.PasswordPolicy
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Credentials
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext
import java.util.function.Function

const val PHYSICAL_ID = "password-policy"
const val MAX_PASSWORD_AGE = 4

private val CREDENTIALS = Credentials("ID", "secret", "test")

val EXCEPTION: AwsServiceException = AwsServiceException.builder()
    .awsErrorDetails(
        AwsErrorDetails.builder()
            .sdkHttpResponse(
                SdkHttpResponse.builder()
                    .build(),
            )
            .build(),
    )
    .build()

@ExtendWith(MockKExtension::class)
class ReadHandlerTest {
    @MockK
    lateinit var logger: LoggerProxy

    @MockK
    lateinit var factory: ResourcesFactory

    @MockK
    lateinit var proxyClient: ProxyClient<IamClient>

    @Test
    fun handleRequest() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetAccountPasswordPolicyRequest::class),
                any<Function<GetAccountPasswordPolicyRequest, GetAccountPasswordPolicyResponse>>(),
            )
        } returns GetAccountPasswordPolicyResponse.builder()
            .passwordPolicy(
                PasswordPolicy.builder()
                    .maxPasswordAge(MAX_PASSWORD_AGE)
                    .build(),
            )
            .build()

        val result = ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(PHYSICAL_ID, result.resourceModel?.physicalId)
        assertEquals(MAX_PASSWORD_AGE, result.resourceModel?.maxPasswordAge)
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
                ofType(GetAccountPasswordPolicyRequest::class),
                any<Function<GetAccountPasswordPolicyRequest, GetAccountPasswordPolicyResponse>>(),
            )
        } throws EXCEPTION

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
    proxyClient: ProxyClient<IamClient>,
): AmazonWebServicesClientProxy {
    val proxy = AmazonWebServicesClientProxy(logger, CREDENTIALS) { 1L }

    every { logger.log(any()) } just runs
    every { factory.getClient(proxy) } returns proxyClient
    every { proxyClient.client().serviceName() } returns "iam"

    return proxy
}
