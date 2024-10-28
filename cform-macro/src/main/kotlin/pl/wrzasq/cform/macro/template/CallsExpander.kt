/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2023 - 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.template

/**
 * Template processing class that modifies specific CloudFormation calls.
 *
 * @param handler Function expansion handler.
 */
class CallsExpander(
    private val handler: ExpansionHandler,
) {
    /**
     * Applies calls modification to entire template.
     *
     * @param input Initial template state.
     * @param params Template parameter values.
     * @return Processed template.
     */
    fun processTemplate(input: Map<String, Any>, params: Map<String, Any>) = input.mapSelected(
        // these are the only sections where any function applies
        SECTION_RESOURCES to ::traverse,
        SECTION_OUTPUTS to ::traverse,
    )

    private fun traverse(input: Any): Any = when (input) {
        is Map<*, *> -> traverseMap(asMap(input))
        is List<*> -> input.filterNotNull().map(::traverse)
        else -> input
    }

    // still may need to go deeper, regardless of picked scenario - thus `.mapValues()` call
    private fun traverseMap(input: Map<String, Any>) = if (input.size == 1 && handler.canHandle(input.keys.first())) {
        handler.expand(input.entries.first().toPair())
    } else {
        input
    }.mapValuesOnly(::traverse)
}
