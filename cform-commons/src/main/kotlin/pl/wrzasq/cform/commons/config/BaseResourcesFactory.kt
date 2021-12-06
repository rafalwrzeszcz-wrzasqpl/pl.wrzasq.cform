/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.commons.config

import pl.wrzasq.commons.aws.runtime.config.ResourcesFactory
import software.amazon.cloudformation.LambdaWrapper
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Common base for factories required by all resource handlers.
 */
interface BaseResourcesFactory<ResourceType>: ResourcesFactory {
    /**
     * Lambda logic invocation handler.
     */
    val lambdaHandler: LambdaWrapper<ResourceType?, StdCallbackContext>
}
