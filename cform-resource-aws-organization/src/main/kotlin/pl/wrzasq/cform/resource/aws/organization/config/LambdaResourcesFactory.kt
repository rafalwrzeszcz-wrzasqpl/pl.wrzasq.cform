/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.organization.config

import com.fasterxml.jackson.core.type.TypeReference
import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.commons.action.NotUpdatableHandler
import pl.wrzasq.cform.commons.config.BaseLambdaResourcesFactory
import pl.wrzasq.cform.resource.aws.organization.action.CreateHandler
import pl.wrzasq.cform.resource.aws.organization.action.DeleteHandler
import pl.wrzasq.cform.resource.aws.organization.action.ListHandler
import pl.wrzasq.cform.resource.aws.organization.action.ReadHandler
import pl.wrzasq.cform.resource.aws.organization.model.ResourceModel
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.organizations.OrganizationsClient
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerRequest
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Resources factory for AWS Lambda environment.
 */
class LambdaResourcesFactory : ResourcesFactory, BaseLambdaResourcesFactory<ResourceModel>() {
    override val createHandler by lazy { CreateHandler(this, readHandler) }

    override val deleteHandler by lazy { DeleteHandler(this) }

    override val listHandler by lazy { ListHandler() }

    override val readHandler by lazy { ReadHandler(this) }

    override val updateHandler: ActionHandler<ResourceModel> by lazy { NotUpdatableHandler(ResourceModel.TYPE_NAME) }

    override fun getRequestTypeReference() =
        object : TypeReference<HandlerRequest<ResourceModel?, StdCallbackContext>>() {}

    override fun getResourceTypeReference() = object : TypeReference<ResourceModel?>() {}

    override fun getClient(proxy: AmazonWebServicesClientProxy): ProxyClient<OrganizationsClient> = proxy.newProxy {
        OrganizationsClient.builder()
            .region(Region.AWS_GLOBAL)
            .build()
    }

    companion object {
        // TODO: move it to pl.wrzasq.commons:commons-aws and handle via annotations/_HANDLER param
        /**
         * Shell entry point.
         *
         * @param args Runtime arguments.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val factory = LambdaResourcesFactory()
            factory.api.run(factory.lambdaHandler::handleRequest)
        }
    }
}
