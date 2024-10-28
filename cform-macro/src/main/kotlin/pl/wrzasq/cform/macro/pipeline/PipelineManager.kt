/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.pipeline

import pl.wrzasq.cform.macro.pipeline.types.CloudFormationDeploy
import pl.wrzasq.cform.macro.pipeline.types.CodeBuild
import pl.wrzasq.cform.macro.pipeline.types.S3Deploy
import pl.wrzasq.cform.macro.pipeline.types.S3Promote
import pl.wrzasq.cform.macro.pipeline.types.S3Source
import pl.wrzasq.cform.macro.pipeline.types.fromMap
import pl.wrzasq.cform.macro.template.Fn.fnIf
import pl.wrzasq.cform.macro.template.NO_VALUE
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.popProperty

/**
 * Pipeline schema definition.
 */
class PipelineManager {
    // order of stages is important, so they are a list
    private val stages = mutableListOf<PipelineStage>()

    // these will be computed during compilation
    private val namespaces = mutableMapOf<String, String>()
    private val artifacts = mutableMapOf<String, String>()
    private val all = mutableMapOf<String, PipelineAction>()

    /**
     * Tries to load stage definition.
     *
     * @param input Stage definition.
     * @return Operation status.
     */
    fun handleStage(input: Map<String, Any>): Boolean {
        var name: String? = null
        var condition: String? = null
        val actions = mutableMapOf<String, PipelineAction>()

        val leftover = input
            .popProperty("Name", { name = it.toString() })
            .popProperty("Condition", { condition = it.toString() })
            .popProperty("Actions", {
                for ((key, value) in asMap(it)) {
                    handleAction(key, asMap(value))?.let { action -> actions[key] = action }
                }
            })

        if (actions.isEmpty()) {
            return false
        }

        stages.add(PipelineStage(name ?: return false, actions, leftover, condition))

        return true
    }

    private fun handleAction(name: String, input: Map<String, Any>): PipelineAction? {
        var condition: String? = null
        var creator: ((String, Map<String, Any>, String?) -> PipelineAction)? = null

        val leftover = input
            .popProperty("Condition", { condition = it.toString() })
            .popProperty("ActionTypeId", { creator = fromMap(asMap(it)) })
            .popProperty("ActionType", {
                creator = when (it) {
                    "CloudFormationDeploy" -> ::CloudFormationDeploy
                    "CodeBuild" -> ::CodeBuild
                    "S3Deploy" -> ::S3Deploy
                    "S3Promote" -> ::S3Promote
                    "S3Source" -> ::S3Source
                    else -> throw IllegalArgumentException("Unknown action type `$it`")
                }
            })

        // make sure all required properties are set
        return creator?.let {
            it(name, leftover, condition)
        }
    }

    /**
     * Resolves action by key.
     *
     * @param reference Action location (by stage and action names).
     * @return Action definition.
     */
    fun resolve(reference: String) = checkNotNull(all[reference]) {
        "Unknown action `$reference` - it may happen that you refer to action from further stage"
    }

    /**
     * Resolves namespace by key.
     *
     * @param reference Action location (by stage and action names).
     * @return Action namespace.
     */
    fun resolveNamespace(reference: String): String {
        val action = resolve(reference)
        if (action.namespace == null) {
            val prefix = reference.replace(":", "-").lowercase()
            var suffix = 0
            var namespace = prefix
            while (namespace in namespaces) {
                namespace = "$namespace${suffix++}"
            }

            // save generated
            action.namespace = namespace
            namespaces[namespace] = reference
        }

        return action.namespace.toString()
    }

    /**
     * Prepares pipeline by filling missing gaps and computing dependencies tree.
     */
    fun compile() {
        stages.forEach { compileStage(it.name, it.actions.values) }
    }

    private fun compileStage(name: String, actions: Collection<PipelineAction>) {
        // register all actions - we want to register them stage-by-stage (not in `handleStage()`) so that we can detect
        // premature references
        actions.forEach {
            val ref = "${name}:${it.name}"
            it.outputs.forEach { artifact -> artifacts[artifact] = ref }
            all[ref] = it
        }

        // allow post-processing
        actions.forEach { it.compile(this) }

        // we can't combine the loops - first all actions need to be post-processed
        val visited = mutableSetOf<PipelineAction>()
        actions.forEach { calculateActionOrder(it, name, visited) }
    }

    private fun calculateActionOrder(action: PipelineAction, stageName: String, visited: MutableSet<PipelineAction>) {
        // already calculated
        if (action.runOrder != null) {
            return
        }

        check(action !in visited) { "Circular artifact dependency for ${action.name}" }

        visited.add(action)

        // we are using set here so values will be anyway unique
        action.runOrder = (action.dependencies + action.inputs.mapNotNull(artifacts::get))
            // actions from previous steps are anyway executed upfront and actions from further steps are not yet keyed
            .filter { it.startsWith("$stageName:") }
            .mapNotNull(all::get)
            // first we need to know order of all downstream actions
            .onEach { calculateActionOrder(it, stageName, visited) }
            .maxOfOrNull { (it.runOrder ?: 1) + 1 }

        // we need to remove it from currently visited path as same node can be visited multiple times
        visited.remove(action)
    }

    /**
     * Builds template structure.
     *
     * @return Template fragment.
     */
    fun buildDefinition() = stages.map(PipelineStage::buildDefinition)
}

/**
 * Conditional condition - condition evaluation needs to be postponed to template deployment time.
 */
fun conditional(input: Map<String, Any>, condition: String?) = condition?.let { fnIf(it, input, NO_VALUE) } ?: input
