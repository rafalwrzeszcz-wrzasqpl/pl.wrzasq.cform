/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors.loggroups

import pl.wrzasq.cform.macro.model.ResourceDefinition

/**
 * Handler interface for handling default log groups for existing resources.
 */
interface AutomaticLogGroupHandler {
    /**
     * Property name to check in resource definition.
     */
    val propertyName: String

    /**
     * Builds log group resource.
     *
     * @param resourceId Referred resource logical ID.
     * @param retention Retention definition.
     * @return Log group name definition.
     */
    fun buildLogGroup(resourceId: String, retention: Any): ResourceDefinition
}
