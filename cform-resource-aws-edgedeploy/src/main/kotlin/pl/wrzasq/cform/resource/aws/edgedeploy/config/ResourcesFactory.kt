/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.edgedeploy.config

import pl.wrzasq.cform.commons.config.BaseResourcesFactory
import pl.wrzasq.cform.resource.aws.edgedeploy.model.ResourceModel
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.ProxyClient

/**
 * Factory for resource handler resources.
 */
interface ResourcesFactory : BaseResourcesFactory<ResourceModel> {
    /**
     * Creates AWS service client proxy.
     *
     * @return AWS Lambda client.
     */
    fun getLambdaClient(proxy: AmazonWebServicesClientProxy): ProxyClient<LambdaClient>

    /**
     * Creates AWS service client proxy.
     *
     * @return AWS S3 client.
     */
    fun getS3Client(proxy: AmazonWebServicesClientProxy): ProxyClient<S3Client>
}
