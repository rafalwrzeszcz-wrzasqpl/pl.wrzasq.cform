/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.account.config

import com.fasterxml.jackson.core.type.TypeReference
import pl.wrzasq.cform.commons.ResourceLambdaHandler
import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.commons.config.BaseLambdaResourcesFactory
import pl.wrzasq.cform.commons.model.Tag
import pl.wrzasq.cform.resource.aws.account.action.CreateHandler
import pl.wrzasq.cform.resource.aws.account.action.DeleteHandler
import pl.wrzasq.cform.resource.aws.account.action.ReadHandler
import pl.wrzasq.cform.resource.aws.account.action.UpdateHandler
import pl.wrzasq.cform.resource.aws.account.model.ResourceModel
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.organizations.OrganizationsClient
import software.amazon.cloudformation.Action
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerRequest
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Resources factory for AWS Lambda environment.
 */
class LambdaResourcesFactory : ResourcesFactory, BaseLambdaResourcesFactory<ResourceModel>() {
    private val createHandler by lazy { CreateHandler(this, updateHandler) }

    private val deleteHandler by lazy { DeleteHandler(this) }

    private val readHandler by lazy { ReadHandler(this) }

    private val updateHandler: ActionHandler<ResourceModel> by lazy { UpdateHandler(this, readHandler) }

    override val lambdaHandler by lazy {
        // we can't have it as constructor arguments of ResourceLambdaHandler as these methods are called by super
        // constructor before any of the child classes assignments occur
        object : ResourceLambdaHandler<ResourceModel>(
            configuration,
            buildHandlers()
        ) {
            override fun provideResourceDefinedTags(resourceModel: ResourceModel?) = resourceModel
                ?.tags
                ?.associateBy(Tag::key, Tag::value)

            override fun getTypeReference() = getRequestTypeReference()

            override fun getModelTypeReference() = getResourceTypeReference()
        }
    }

    override fun getRequestTypeReference() =
        object : TypeReference<HandlerRequest<ResourceModel?, StdCallbackContext>>() {}

    override fun getResourceTypeReference() = object : TypeReference<ResourceModel?>() {}

    override fun buildHandlers() = mapOf(
        Action.CREATE to createHandler,
        Action.READ to readHandler,
        Action.DELETE to deleteHandler,
        Action.UPDATE to updateHandler
    )

    override fun getClient(proxy: AmazonWebServicesClientProxy): ProxyClient<OrganizationsClient> = proxy.newProxy {
        OrganizationsClient.builder()
            .region(Region.AWS_GLOBAL)
            .build()
    }
}
