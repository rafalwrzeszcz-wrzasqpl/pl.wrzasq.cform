/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.passwordpolicy.config

import pl.wrzasq.cform.commons.config.BaseResourcesFactory
import pl.wrzasq.cform.resource.aws.passwordpolicy.model.ResourceModel
import software.amazon.awssdk.services.iam.IamClient
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.ProxyClient

/**
 * Factory for resource handler resources.
 */
interface ResourcesFactory : BaseResourcesFactory<ResourceModel, Any?> {
    /**
     * Creates AWS service client proxy.
     *
     * @return AWS IAM client.
     */
    fun getClient(proxy: AmazonWebServicesClientProxy): ProxyClient<IamClient>
}
