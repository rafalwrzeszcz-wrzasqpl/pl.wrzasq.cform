/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 - 2022, 2024, 2026 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.pipeline.types

import pl.wrzasq.cform.macro.pipeline.PipelineManager
import pl.wrzasq.cform.macro.template.CALL_GET_PARAM
import pl.wrzasq.cform.macro.template.Fn
import pl.wrzasq.cform.macro.template.asMapAlways

/**
 * CloudFormation stack deploy action.
 *
 * @param name Stage name.
 * @param input Input properties.
 * @param condition Condition name.
 */
class CloudFormationDeploy(
    name: String,
    input: Map<String, Any>,
    condition: String?,
) : BaseAction(name, input, condition) {
    private val parameters = asMapAlways(properties.remove("Parameters"))
    private val compiled = mutableMapOf<String, Any>()

    init {
        val configuration = asMapAlways(properties["Configuration"])

        // try to figure out input artifacts
        configuration["TemplatePath"]?.let(::includeArtifact)
        configuration["TemplateConfiguration"]?.let(::includeArtifact)

        // parameters may use `Fn::GetParam` calls that are CloudFormation-specific
        parameters.values.filterIsInstance<Map<*, *>>().map {
            val call = it.values.first()
            if (it.size == 1 && it.keys.first() == CALL_GET_PARAM && call is List<*>) {
                inputs.add(call[0].toString())
            }
        }
    }

    override fun compile(manager: PipelineManager) {
        parameters.mapValuesTo(compiled) { processReference(it.value, manager) }
    }

    override fun buildActionTypeId() = buildAwsActionTypeId("Deploy", "CloudFormation")

    override fun buildConfiguration(configuration: MutableMap<String, Any>) {
        if (compiled.isNotEmpty()) {
            configuration["ParameterOverrides"] = Fn.toJsonString(compiled)
        }

        configuration["ActionMode"] = "CREATE_UPDATE"
        configuration["Capabilities"] = "CAPABILITY_NAMED_IAM,CAPABILITY_AUTO_EXPAND"
    }

    private fun includeArtifact(reference: Any) {
        if (reference !is Map<*, *>) {
            inputs.add(reference.toString().split("::")[0])
        }
    }
}
