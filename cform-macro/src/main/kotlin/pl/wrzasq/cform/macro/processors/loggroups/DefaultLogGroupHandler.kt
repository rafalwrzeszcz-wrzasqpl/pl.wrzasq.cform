/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors.loggroups

import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.template.Fn

/**
 * Generic log group handler that applies to many AWS services in a standard way.
 *
 * @param logGroupNameService Prefix for log group name.
 */
class DefaultLogGroupHandler(
    private val logGroupNameService: String
) : AutomaticLogGroupHandler {
    override val propertyName = "LogsRetentionInDays"

    override fun buildLogGroup(resourceId: String, retention: Any) = ResourceDefinition(
        id = "${resourceId}LogGroup",
        type = "AWS::Logs::LogGroup",
        properties = mapOf(
            "LogGroupName" to Fn.sub("/aws/${logGroupNameService}/\${${resourceId}}"),
            "RetentionInDays" to retention
        )
    )
}
