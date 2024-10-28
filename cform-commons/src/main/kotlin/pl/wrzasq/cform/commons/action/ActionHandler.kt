/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.commons.action

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Interface required by CloudFormation resource operation.
 */
interface ActionHandler<ResourceModel> {
    /**
     * Handles invocation of the action.
     *
     * @param proxy Credentials access proxy to AWS services.
     * @param request Input request.
     * @param callbackContext Result state.
     * @param logger Logger.
     * @return Progress notification.
     */
    fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel?>,
        callbackContext: StdCallbackContext,
        logger: Logger,
    ): ProgressEvent<ResourceModel?, StdCallbackContext>
}
