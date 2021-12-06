/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.data.cognito.domain.config

import com.fasterxml.jackson.core.type.TypeReference
import pl.wrzasq.cform.commons.config.BaseLambdaResourcesFactory
import pl.wrzasq.cform.data.cognito.domain.action.CreateHandler
import pl.wrzasq.cform.data.cognito.domain.action.DeleteHandler
import pl.wrzasq.cform.data.cognito.domain.action.ReadHandler
import pl.wrzasq.cform.data.cognito.domain.model.ResourceModel
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.cloudformation.Action
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerRequest
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Resources factory for AWS Lambda environment.
 */
class LambdaResourcesFactory : ResourcesFactory, BaseLambdaResourcesFactory<ResourceModel>() {
    private val createHandler by lazy { CreateHandler(readHandler) }

    private val deleteHandler by lazy { DeleteHandler() }

    private val readHandler by lazy { ReadHandler(this) }

    override fun getRequestTypeReference() =
        object : TypeReference<HandlerRequest<ResourceModel?, StdCallbackContext>>() {}

    override fun getResourceTypeReference() = object : TypeReference<ResourceModel?>() {}

    override fun buildHandlers() = mapOf(
        Action.CREATE to createHandler,
        Action.READ to readHandler,
        Action.DELETE to deleteHandler
    )

    override fun getClient(proxy: AmazonWebServicesClientProxy): ProxyClient<CognitoIdentityProviderClient> =
        proxy.newProxy { CognitoIdentityProviderClient.builder().build() }
}
