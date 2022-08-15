/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.data.cognito.client.action

import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.data.cognito.client.model.ResourceModel
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Pass-through handler.
 *
 * @param readHandler Resource reading handler.
 */
class CreateHandler(
    private val readHandler: ActionHandler<ResourceModel>
) : ActionHandler<ResourceModel> {
    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel?>,
        callbackContext: StdCallbackContext,
        logger: Logger
    ) = readHandler.handleRequest(proxy, request, callbackContext, logger)
}
