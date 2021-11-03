/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors.types

import pl.wrzasq.cform.macro.model.ResourceDefinition

/**
 * Resource handler specification.
 */
interface ResourceHandler {
    /**
     * Returns list of handled resource types.
     *
     * @return Resource types.
     */
    fun handledResourceTypes(): Collection<String>

    /**
     * Processes resource transformation.
     *
     * @param entry Initial resource definition.
     * @return Transformed resource properties.
     */
    fun handle(entry: ResourceDefinition): Map<String, Any>
}
