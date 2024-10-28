/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.pipeline

/**
 * Pipeline stage definition.
 *
 * @param name Stage name.
 * @param actions Stage actions.
 * @param properties Extra properties.
 * @param condition Condition name.
 */
class PipelineStage(
    val name: String,
    // order of actions is not important - execution can be ordered by `RunOrder`, so can reduce the structure
    val actions: Map<String, PipelineAction>,
    // currently there is only `Blockers` but we want to be future-proof
    private val properties: Map<String, Any>,
    private val condition: String?,
) {
    /**
     * Builds template structure.
     *
     * @return Template fragment.
     */
    fun buildDefinition(): Map<String, Any> {
        val input = properties + mapOf(
            "Name" to name,
            "Actions" to actions.toSortedMap().values.map(PipelineAction::buildDefinition),
        )

        return conditional(input, condition)
    }
}
