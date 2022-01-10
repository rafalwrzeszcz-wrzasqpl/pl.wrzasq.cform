/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 - 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.pipeline.types

import pl.wrzasq.cform.macro.pipeline.PipelineManager

/**
 * S3 deploy action.
 *
 * @param name Stage name.
 * @param input Input properties.
 * @param condition Condition name.
 */
open class S3Deploy(
    name: String,
    input: Map<String, Any>,
    condition: String?
) : BaseAction(name, input, condition) {
    private var bucket: Any? = properties.remove("Bucket")
    protected var objectKey: Any? = properties.remove("ObjectKey")

    override fun compile(manager: PipelineManager) {
        bucket = bucket?.let { processReference(it, manager) }
    }

    override fun buildActionTypeId() = buildAwsActionTypeId("Deploy", "S3")

    override fun buildConfiguration(configuration: MutableMap<String, Any>) {
        bucket?.let { configuration["BucketName"] = it }
        objectKey?.let { configuration["ObjectKey"] = it }
    }
}
