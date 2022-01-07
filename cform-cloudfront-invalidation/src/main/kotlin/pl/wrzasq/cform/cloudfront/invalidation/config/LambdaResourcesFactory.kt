/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.cloudfront.invalidation.config

import pl.wrzasq.cform.cloudfront.invalidation.Handler
import pl.wrzasq.commons.aws.runtime.NativeLambdaApi
import pl.wrzasq.commons.aws.runtime.config.ResourcesFactory
import pl.wrzasq.commons.json.ObjectMapperFactory
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudfront.CloudFrontClient
import software.amazon.awssdk.services.codepipeline.CodePipelineClient

/**
 * Resources factory for AWS Lambda environment.
 */
class LambdaResourcesFactory : ResourcesFactory {
    private val cloudFront by lazy {
        CloudFrontClient.builder()
            .region(Region.AWS_GLOBAL)
            .build()
    }

    private val codePipeline by lazy { CodePipelineClient.create() }

    private val objectMapper by lazy { ObjectMapperFactory.createObjectMapper() }

    private val handler by lazy { Handler(objectMapper, cloudFront, codePipeline) }

    override val lambdaApi by lazy { NativeLambdaApi(objectMapper) }

    override val lambdaCallback = handler::handle
}
