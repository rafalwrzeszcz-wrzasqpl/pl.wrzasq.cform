/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.template

import pl.wrzasq.cform.macro.config.LambdaResourcesFactory

/**
 * Template processing which replaces nested calls to intrinsic functions to produce plain JSON representation.
 *
 * @param input Initial template state.
 */
class CompiledFragment(input: Map<String, Any>) {
    private var plainText = true
    private var index = 0
    private val params = mutableMapOf<String, Any>()
    private val json: String

    /**
     * Raw template serialization.
     *
     * @return JSON representation.
     */
    val raw: Any
        get() = when {
            plainText -> json
            params.isEmpty() -> Fn.sub(json)
            else -> Fn.sub(listOf(json, params))
        }

    init {
        val final = traverseMap(input)
        json = LambdaResourcesFactory.OBJECT_MAPPER.writeValueAsString(final)
    }

    private fun traverse(input: Any): Any = when (input) {
        is Map<*, *> -> traverseMap(asMap(input))
        is List<*> -> input.filterNotNull().map(::traverse)
        else -> input
    }

    private fun traverseMap(input: Map<String, Any>) = input.mapValuesOnly {
        if (it is Map<*, *> && it.size == 1 && isIntrinsicFunction(it.keys.first().toString())) {
            plainText = false
            val (key, value) = asMap(it).entries.first()

            when {
                key == CALL_REF || key == CALL_GET_ATT && value is String -> "\${${value}}"
                key == CALL_GET_ATT && value is List<*> -> "\${${value[0]}.${value[1]}}"
                // this is the simples one - just elevate encapsulation
                key == CALL_SUB && value is String -> value
                key == CALL_SUB && value is List<*> -> {
                    params.putAll(asMapAlways(value[1]))
                    value[0] ?: ""
                }
                // this is our notation - it will be handled afterwards
                key == CALL_IMPORT_VALUE && value is String -> "\${Import:${value}}"
                // this is specific to CodePipeline+CloudFormation but still - intrinsic function
                key == CALL_GET_PARAM -> it
                else -> generateParamPlaceholder(++index, it, params)
            }
        } else {
            traverse(it)
        }
    }

    private fun isIntrinsicFunction(key: String) = key == "Ref" || key.startsWith("Fn::")
}

private fun generateParamPlaceholder(index: Int, value: Any, params: MutableMap<String, Any>): String {
    val param = "param${index}"
    params[param] = value
    return "\${${param}}"
}
