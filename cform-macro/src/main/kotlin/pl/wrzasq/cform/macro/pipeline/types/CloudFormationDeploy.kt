/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.pipeline.types

import pl.wrzasq.cform.macro.pipeline.PipelineManager
import pl.wrzasq.cform.macro.template.Fn
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.commons.json.ObjectMapperFactory

// action and stage names can contain `.` so we need to match last one
private val REFERENCE = Regex("#\\{([^:]+):([^}]+).([^.]+)}")
private val OBJECT_MAPPER = ObjectMapperFactory.createObjectMapper()

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
    condition: String?
) : BaseAction(name, input, condition) {
    private val parameters = asMap(properties.remove("Parameters") ?: emptyMap<String, Any>())
    private val compiled = mutableMapOf<String, Any>()

    init {
        val configuration = asMap(properties["Configuration"] ?: emptyMap<String, Any>())

        // try to figure out input artifacts
        configuration["TemplatePath"]?.let(::includeArtifact)
        configuration["TemplateConfiguration"]?.let(::includeArtifact)

        // parameters may use `Fn::GetParam` calls that are CloudFormation-specific
        parameters.values.filterIsInstance<Map<*, *>>().map {
            val call = it.values.first()
            if (it.size == 1 && it.keys.first() == "Fn::GetParam" && call is List<*>) {
                inputs.add(call[0].toString())
            }
        }
    }

    override fun compile(manager: PipelineManager) {
        parameters.mapValuesTo(compiled) {
            val value = it.value
            if (value is String) {
                value.replace(REFERENCE) { match ->
                    val group = match.groupValues
                    val reference = "${group[1]}:${group[2]}"

                    dependencies.add(reference)

                    "#{${manager.resolveNamespace(reference)}.${group[3]}}"
                }
            } else {
                value
            }
        }
    }

    override fun buildActionTypeId() = buildAwsActionTypeId("Deploy", "CloudFormation")

    override fun buildConfiguration(configuration: MutableMap<String, Any>) {
        if (compiled.isNotEmpty()) {
            var plainText = true
            var index = 0
            val params = mutableMapOf<String, Any>()
            val final = compiled.mapValues {
                if (it is Map<*, *> && it.size == 1) {
                    plainText = false
                    val (key, value) = asMap(it).entries.first()

                    when {
                        key == "Ref" || (key == "Fn::GetAtt" && value is String) -> "\${${value}}"
                        key == "Fn::GetAtt" && value is List<*> -> "\${${value[0]}.${value[1]}}"
                        // this is the simples one - just elevate encapsulation
                        key == "Fn::Sub" && value is String -> value
                        key == "Fn::Sub" && value is List<*> -> {
                            params.putAll(asMap(value[1] ?: emptyMap<String, Any>()))
                            value[0]
                        }
                        // this is our notation - it will be handled afterwards
                        key == "Fn::ImportValue" && value is String -> "\${Import:${value}}"
                        else -> generateParamPlaceholder(++index, value, params)
                    }
                } else {
                    it.value
                }
            }
            val json = OBJECT_MAPPER.writeValueAsString(final)

            configuration["ParameterOverrides"] = when {
                plainText -> json
                params.isEmpty() -> Fn.sub(json)
                else -> Fn.sub(listOf(json, params))
            }
        }

        configuration["ActionMode"] = "CREATE_UPDATE"
        configuration["Capabilities"] = "CAPABILITY_NAMED_IAM,CAPABILITY_AUTO_EXPAND"
    }

    private fun includeArtifact(reference: Any) {
        inputs.add(reference.toString().split("::")[0])
    }
}

private fun generateParamPlaceholder(index: Int, value: Any, params: MutableMap<String, Any>): String {
    val param = "param${index}"
    params[param] = value
    return "\${${param}}"
}
