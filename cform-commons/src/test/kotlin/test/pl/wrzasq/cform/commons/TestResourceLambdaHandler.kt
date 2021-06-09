/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.commons

import com.fasterxml.jackson.core.type.TypeReference
import pl.wrzasq.cform.commons.ResourceLambdaHandler
import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.commons.config.Configuration
import software.amazon.cloudformation.Action
import software.amazon.cloudformation.proxy.HandlerRequest
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.StdCallbackContext

class TestResourceLambdaHandler(
    configuration: Configuration,
    handlers: Map<Action, ActionHandler<Any>>,
    logger: LoggerProxy
) : ResourceLambdaHandler<Any>(configuration, handlers) {
    init {
        loggerProxy = logger
    }

    public override fun provideResourceSchemaJSONObject() = super.provideResourceSchemaJSONObject()

    public override fun transform(request: HandlerRequest<Any?, StdCallbackContext>) = super.transform(request)

    override fun getTypeReference(): TypeReference<HandlerRequest<Any?, StdCallbackContext>> =
        object : TypeReference<HandlerRequest<Any?, StdCallbackContext>>() {}

    override fun getModelTypeReference(): TypeReference<Any?> = object : TypeReference<Any?>() {}
}
