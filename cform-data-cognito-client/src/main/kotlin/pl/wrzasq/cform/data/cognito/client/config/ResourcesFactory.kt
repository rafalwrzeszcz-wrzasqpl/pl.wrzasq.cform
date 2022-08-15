/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.data.cognito.client.config

import pl.wrzasq.cform.commons.config.BaseResourcesFactory
import pl.wrzasq.cform.data.cognito.client.model.ResourceModel
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.ProxyClient

/**
 * Factory for resource handler resources.
 */
interface ResourcesFactory : BaseResourcesFactory<ResourceModel> {
    /**
     * Creates AWS service client proxy.
     *
     * @return AWS Cognito IDP client.
     */
    fun getClient(proxy: AmazonWebServicesClientProxy): ProxyClient<CognitoIdentityProviderClient>
}
