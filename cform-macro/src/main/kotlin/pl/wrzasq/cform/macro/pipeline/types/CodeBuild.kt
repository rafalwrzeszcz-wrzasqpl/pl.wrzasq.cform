/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.pipeline.types

/**
 * CodeBuild execution action.
 *
 * @param name Stage name.
 * @param input Input properties.
 * @param condition Condition name.
 */
class CodeBuild(
    name: String,
    input: Map<String, Any>,
    condition: String?
) : BaseAction(name, input, condition) {
    private val project: Any? = properties.remove("Project")

    override fun buildActionTypeId() = buildAwsActionTypeId("Build", "CodeBuild")

    override fun buildConfiguration(configuration: MutableMap<String, Any>) {
        project?.let { configuration["ProjectName"] = it }
    }
}
