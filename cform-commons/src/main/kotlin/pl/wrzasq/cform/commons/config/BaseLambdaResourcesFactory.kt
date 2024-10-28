/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.commons.config

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import pl.wrzasq.cform.commons.ResourceLambdaHandler
import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.commons.aws.runtime.NativeLambdaApi
import software.amazon.cloudformation.Action
import software.amazon.cloudformation.proxy.HandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Generic provider for resource handlers.
 */
abstract class BaseLambdaResourcesFactory<ResourceType, ConfigurationType> : BaseResourcesFactory<ResourceType, ConfigurationType> {
    override val lambdaApi by lazy { NativeLambdaApi(objectMapper) }

    override val lambdaHandler by lazy {
        // we can't have it as constructor arguments of ResourceLambdaHandler as these methods are called by super
        // constructor before any of the child classes assignments occur
        object : ResourceLambdaHandler<ResourceType, ConfigurationType>(
            configuration,
            buildHandlers(),
        ) {
            override fun getTypeReference() = getRequestTypeReference()

            override fun getModelTypeReference() = getResourceTypeReference()
        }
    }

    override val lambdaCallback
        get() = lambdaHandler::handleRequest

    protected val objectMapper by lazy {
        ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule.Builder().build())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
    }

    protected val configuration by lazy { LambdaConfiguration() }

    protected abstract fun getRequestTypeReference(): TypeReference<HandlerRequest<ResourceType?, StdCallbackContext, ConfigurationType>>

    protected abstract fun getResourceTypeReference(): TypeReference<ResourceType?>

    protected abstract fun buildHandlers(): Map<Action, ActionHandler<ResourceType>>
}
