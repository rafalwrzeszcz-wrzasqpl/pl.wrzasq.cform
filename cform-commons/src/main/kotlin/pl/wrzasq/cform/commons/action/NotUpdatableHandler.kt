/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.commons.action

import software.amazon.cloudformation.exceptions.CfnNotUpdatableException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Default handler for `UPDATE` operation on resource that can not be altered.
 *
 * @param typeName Name of the resource type.
 */
class NotUpdatableHandler<ResourceModel>(
    private val typeName: String
) : ActionHandler<ResourceModel> {
    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel?>,
        callbackContext: StdCallbackContext,
        logger: Logger
    ): ProgressEvent<ResourceModel?, StdCallbackContext> = throw CfnNotUpdatableException(
        typeName,
        request.logicalResourceIdentifier
    )
}
