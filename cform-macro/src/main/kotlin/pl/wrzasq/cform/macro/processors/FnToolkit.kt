/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 - 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors

import pl.wrzasq.cform.macro.template.CALL_SUB
import pl.wrzasq.cform.macro.template.ExpansionHandler
import pl.wrzasq.cform.macro.template.Fn
import pl.wrzasq.cform.macro.template.asMapAlways

private val PATTERN_SUB_IMPORT = Regex("\\\$\\{Import:(.*?)}")

/**
 * Custom functions.
 */
class FnToolkit : ExpansionHandler {
    override fun canHandle(function: String) = function == CALL_SUB

    override fun expand(input: Pair<String, Any>) = Fn.sub(resolve(input.second))

    private fun resolve(input: Any): Any {
        val (inputTemplate, inputParams) = if (input is List<*>) {
            input.first().toString() to asMapAlways(input.last())
        } else {
            input.toString() to emptyMap()
        }

        // look for all occurrences of import calls
        val imports = PATTERN_SUB_IMPORT.findAll(inputTemplate).map { it.groupValues[1] }.toList()

        // check if there is any need to modify existing structure - if not, simply leave it untouched
        return if (imports.isEmpty()) {
            input
        } else {
            val params = inputParams.toMutableMap()
            val template = imports.foldIndexed(inputTemplate) { index, buffer, import ->
                val key = "import$index"
                params[key] = Fn.importValue(import)

                buffer.replace("\${Import:${import}}", "\${$key}")
            }

            listOf(template, params)
        }
    }
}
