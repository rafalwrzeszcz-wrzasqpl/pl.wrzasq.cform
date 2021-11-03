/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.pipeline.types

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
    private val bucket: Any? = properties.remove("Bucket")
    private val objectKey: Any? = properties.remove("ObjectKey")

    override fun buildActionTypeId() = buildAwsActionTypeId("Deploy", "S3")

    override fun buildConfiguration(configuration: MutableMap<String, Any>) {
        bucket?.let { configuration["BucketName"] = it }
        objectKey?.let { configuration["ObjectKey"] = it }
    }
}
