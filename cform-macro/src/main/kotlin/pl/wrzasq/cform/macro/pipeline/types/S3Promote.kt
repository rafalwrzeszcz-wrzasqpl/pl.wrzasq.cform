/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 - 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.pipeline.types

import pl.wrzasq.cform.macro.pipeline.PipelineManager

/**
 * S3 deploy action that corresponds to source action.
 *
 * @param name Stage name.
 * @param input Input properties.
 * @param condition Condition name.
 */
class S3Promote(
    name: String,
    input: Map<String, Any>,
    condition: String?
) : S3Deploy(name, input, condition) {
    private val source: String = properties.remove("Source").toString()

    override fun compile(manager: PipelineManager) {
        super.compile(manager)

        val action = manager.resolve(source)
        check(action is S3Source) { "$name refers to $source which is not S3 source" }

        objectKey = action.objectKey
        inputs.addAll(action.outputs)
    }

    override fun buildConfiguration(configuration: MutableMap<String, Any>) {
        super.buildConfiguration(configuration)

        configuration["Extract"] = false
        configuration["CannedACL"] = "bucket-owner-full-control"
    }
}
