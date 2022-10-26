/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors.types

import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.template.CompiledFragment
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.popProperty

/**
 * Converts SecretsManager structure to initial JSON.
 */
class SecretStructure : ResourceHandler {
    override fun handledResourceTypes() = listOf("AWS::SecretsManager::Secret")

    override fun handle(entry: ResourceDefinition) = processSecret(entry.properties)

    private fun processSecret(input: Map<String, Any>): Map<String, Any> {
        val output = input.toMutableMap()

        return output.popProperty("SecretContent", { output["SecretString"] = CompiledFragment(asMap(it)).raw })
    }
}
