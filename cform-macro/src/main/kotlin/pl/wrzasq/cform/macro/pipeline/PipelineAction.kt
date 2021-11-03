/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.pipeline

/**
 * Pipeline action definition.
 */
interface PipelineAction {
    /**
     * Action identifier.
     */
    val name: String

    /**
     * Input artifacts names.
     */
    val inputs: Set<String>

    /**
     * Provided artifacts.
     */
    val outputs: Set<String>

    /**
     * Actions on which current one depends.
     */
    val dependencies: Set<String>

    /**
     * Variables namespace.
     */
    var namespace: String?

    /**
     * Execution order within stage.
     */
    var runOrder: Int?

    /**
     * Compiles action.
     *
     * @param manager Pipeline references resolver.
     */
    fun compile(manager: PipelineManager) {}

    /**
     * Builds template structure.
     *
     * @return Template fragment.
     */
    fun buildDefinition(): Map<String, Any>
}
