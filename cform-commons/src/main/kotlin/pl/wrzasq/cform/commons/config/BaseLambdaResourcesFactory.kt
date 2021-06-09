/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.commons.config

import com.fasterxml.jackson.core.type.TypeReference
import pl.wrzasq.cform.commons.ResourceLambdaHandler
import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.commons.aws.runtime.NativeLambdaApi
import pl.wrzasq.commons.json.ObjectMapperFactory
import software.amazon.cloudformation.Action
import software.amazon.cloudformation.proxy.HandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Generic provider for resource handlers.
 */
abstract class BaseLambdaResourcesFactory<ResourceType> : BaseResourcesFactory<ResourceType> {
    override val api: NativeLambdaApi by lazy { NativeLambdaApi(objectMapper) }

    override val lambdaHandler by lazy {
        // we can't have it as constructor arguments of ResourceLambdaHandler as these methods are called by super
        // constructor before any of the child classes assignments occur
        object : ResourceLambdaHandler<ResourceType>(
            configuration,
            buildHandlers()
        ) {
            override fun getTypeReference() = getRequestTypeReference()

            override fun getModelTypeReference() = getResourceTypeReference()
        }
    }

    private val objectMapper by lazy { ObjectMapperFactory.createObjectMapper() }

    private val configuration by lazy { LambdaConfiguration() }

    protected abstract val createHandler: ActionHandler<ResourceType>

    protected abstract val deleteHandler: ActionHandler<ResourceType>

    protected abstract val listHandler: ActionHandler<ResourceType>

    protected abstract val readHandler: ActionHandler<ResourceType>

    protected abstract val updateHandler: ActionHandler<ResourceType>

    protected abstract fun getRequestTypeReference(): TypeReference<HandlerRequest<ResourceType?, StdCallbackContext>>

    protected abstract fun getResourceTypeReference(): TypeReference<ResourceType?>

    private fun buildHandlers() = mapOf(
        Action.CREATE to createHandler,
        Action.READ to readHandler,
        Action.UPDATE to updateHandler,
        Action.DELETE to deleteHandler,
        Action.LIST to listHandler
    )
}
