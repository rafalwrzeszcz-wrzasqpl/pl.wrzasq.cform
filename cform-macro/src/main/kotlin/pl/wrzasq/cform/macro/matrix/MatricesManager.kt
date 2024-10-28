/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2023 - 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.matrix

import pl.wrzasq.cform.macro.template.CALL_GET_ATT
import pl.wrzasq.cform.macro.template.CALL_LENGTH
import pl.wrzasq.cform.macro.template.CALL_REF
import pl.wrzasq.cform.macro.template.CALL_SUB
import pl.wrzasq.cform.macro.template.ExpansionHandler
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.asMapAlways

private const val REF_PATTERN = "Matrix:([^.}\\[]+)(?:\\[([^\\]]+)\\](\\.[^}]+)?)?"
private val REF_MATCH_FULL = Regex("^${REF_PATTERN}\$")
private val REF_MATCH_PATTERN = Regex("\\\$\\{${REF_PATTERN}}")

/**
 * Registry to all matrices.
 */
class MatricesManager : ExpansionHandler {
    private val matrices: MutableMap<String, Matrix> = mutableMapOf()

    /**
     * Builds new resources matrix.
     *
     * @param id Matrix resource identifier.
     * @param template Template resource definition.
     * @param matrix Matrix setup.
     * @param params Template parameter values.
     */
    fun buildMatrix(id: String, template: Map<String, Any>, matrix: Map<String, Any>, params: Map<String, Any>) {
        val map = asMapAlways(matrix["Entries"])
        // sort to make predictable order of generated entries
        val key = map.keys.sorted()
        val idPattern = matrix["LogicalIdPattern"]?.toString()
            ?: key.joinToString(prefix = id, separator = "") { "\${${it}}" }

        /**
         * Matrix:
         *      Domain:
         *          - "wrzasq.pl"
         *          - "ivms.online"
         *      Role:
         *          Static: "static"
         *          Web: "www"
         *
         * Will result in:
         *
         * Key = ["Domain", "Role"]
         * Entries keyed by: ["1", "Static"], ["1", "Web"], ["2", "Static"], ["2", "Web"]
         * Entries being:
         *  -   [{"Domain": "wrzasq.pl"}, {"Role": "static"}]
         *  -   [{"Domain": "ivms.online"}, {"Role": "static"}]
         *  -   [{"Domain": "wrzasq.pl"}, {"Role": "web"}]
         *  -   [{"Domain": "ivms.online"}, {"Role": "web"}]
         *
         * Key doesn't need to contain field names, as it's ordered.
         *
         * Each matrix entry is list of matrix params keys and maps to value mapping.
         */

        val entries = key
            // we map from sorted key for consistent order
            .map { it to map[it] }
            // initial seed of empty key mapped to empty matrix set
            .fold(listOf(emptyList<String>() to emptyMap<String, String>())) { entries, (param, values) ->
                val options = when (values) {
                    is Map<*, *> -> asMap(values)
                    is List<*> -> listToMap(values)
                    else -> listToMap(params[values].toString().split(","))
                }

                options.flatMap { (option, value) ->
                    // option key goes to entry path
                    // option value goes to substitution params
                    entries.map { it.first + option to it.second + (param to value.toString()) }
                }
            }

        matrices[id] = Matrix(id, idPattern, template, key, entries.toMap())
    }

    /**
     * Checks if there is any matrix expansion in current scope.
     *
     * @return State, whether there is any matrix definition.
     */
    fun isEmpty() = matrices.isEmpty()

    /**
     * Generates list of all resources defined in current scope.
     *
     * @return List of resource models.
     */
    fun generateResources() = matrices.toSortedMap().flatMap { it.value.generateResources() }

    override fun canHandle(function: String) = function == CALL_REF
        || function == CALL_SUB
        || function == CALL_GET_ATT
        || function == CALL_LENGTH

    override fun expand(input: Pair<String, Any>): Map<String, Any> {
        val params = input.second

        // call to anything else than Fn::Length needs to contain Matrix:$ID[$Selector]
        val value = if (input.first == CALL_LENGTH) {
            // for length, we return number of matrix elements - this is only case when we simply refer to Matrix:$ID
            REF_MATCH_FULL.find(input.second.toString())?.let { match ->
                val matrixId = match.groupValues[1]
                val matrix = checkNotNull(matrices[matrixId]) { "Checking length of unknown matrix `$matrixId`" }
                List(matrix.length) { it }
            } ?: input.second
        } else if (params is List<*> && params[0] is String) {
            listOf(
                expand(params[0].toString()),
                params[1],
            )
        } else if (params is String) {
            expand(params)
        } else {
            params
        }

        return mapOf(input.first to value)
    }

    private fun expand(input: String) = when {
        REF_MATCH_FULL.matches(input) -> REF_MATCH_FULL.replace(input) { resolve(it.groupValues) }
        REF_MATCH_PATTERN.containsMatchIn(input) -> REF_MATCH_PATTERN.replace(input) {
            "\${${resolve(it.groupValues)}}"
        }
        else -> input
    }

    private fun resolve(match: List<String?>) = "${resolve(match[1] ?: "", match[2] ?: "")}${match[3] ?: ""}"

    private fun resolve(matrixId: String, selector: String): String {
        val matrix = checkNotNull(matrices[matrixId]) { "Reference to unknown matrix `$matrixId`" }
        val parts = selector.split(",")
            .map { it.split("=") }
            .associate { it[0] to it[1] }

        return matrix.resolve(parts)
    }
}

private fun listToMap(options: List<*>) = options
    .mapIndexed { index, value -> index.toString() to value.toString() }
    .toMap()
