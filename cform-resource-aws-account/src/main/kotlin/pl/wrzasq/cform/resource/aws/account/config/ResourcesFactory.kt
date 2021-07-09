/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.account.config

import pl.wrzasq.cform.commons.config.BaseResourcesFactory
import pl.wrzasq.cform.resource.aws.account.model.ResourceModel
import software.amazon.awssdk.services.organizations.OrganizationsClient
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.ProxyClient

/**
 * Factory for resource handler resources.
 */
interface ResourcesFactory : BaseResourcesFactory<ResourceModel> {
    /**
     * Creates AWS service client proxy.
     *
     * @return AWS Organizations client.
     */
    fun getClient(proxy: AmazonWebServicesClientProxy): ProxyClient<OrganizationsClient>
}
