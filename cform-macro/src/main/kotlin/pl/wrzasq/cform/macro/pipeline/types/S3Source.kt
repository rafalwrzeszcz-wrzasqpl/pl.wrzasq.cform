/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.pipeline.types

/**
 * S3 source action.
 *
 * @param name Stage name.
 * @param input Input properties.
 * @param condition Condition name.
 */
class S3Source(
    name: String,
    input: Map<String, Any>,
    condition: String?
) : BaseAction(name, input, condition) {
    private val bucket: Any = properties.remove("Bucket")
        ?: throw IllegalStateException("$name action misses bucket definition")

    /**
     * S3 object key.
     */
    val objectKey: Any = properties.remove("ObjectKey")
        ?: throw IllegalStateException("$name action misses object key definition")

    override fun buildActionTypeId() = buildAwsActionTypeId("Source", "S3")

    override fun buildConfiguration(configuration: MutableMap<String, Any>) {
        configuration["S3Bucket"] = bucket
        configuration["S3ObjectKey"] = objectKey
    }
}
