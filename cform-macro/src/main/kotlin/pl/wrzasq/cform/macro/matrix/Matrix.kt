/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2023 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.matrix

import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.mapValuesOnly

/**
 * Single matrix container.
 *
 * @param id Matrix ID.
 * @param idPattern Logical pattern ID for matrix entries.
 * @param template Resource template.
 * @param key Key entries order.
 * @param entries Matrix entries.
 */
class Matrix(
    private val id: String,
    private val idPattern: String,
    private val template: Map<String, Any>,
    private val key: List<String>,
    private val entries: Map<List<String>, Map<String, String>>
) {
    /**
     * Returns number of defined matrices.
     *
     * @return Number of matrices in current template.
     */
    val length
        get() = entries.count()

    /**
     * Generates list of all resources defined in this matrx.
     *
     * @return List of resource models.
     */
    fun generateResources() = entries
        .map { generateLogicalId(it.key) to generateValue(template, it.value) }
        .sortedBy { it.first }

    /**
     * Resolves logical ID of matrix element.
     *
     * @param selector Element key parameters.
     * @return Expected logical ID.
     */
    fun resolve(selector: Map<String, String>) = generateLogicalId(
        key.map { checkNotNull(selector[it]) { "Missing $it part of $id matrix selector." } }
    )

    private fun generateLogicalId(params: List<String>) = key.foldIndexed(idPattern) { index, value, param ->
        value.replace("\${$param}", params[index])
    }

    private fun generateValue(input: Any, params: Map<String, String>): Any = when (input) {
        is Map<*, *> -> asMap(input).mapValuesOnly { generateValue(it, params) }
        is List<*> -> input.filterNotNull().map { generateValue(it, params) }
        is String -> params.entries.fold(input) { value, entry ->
            value.replace("\${Each:${entry.key}}", entry.value)
        }
        else -> input
    }
}
