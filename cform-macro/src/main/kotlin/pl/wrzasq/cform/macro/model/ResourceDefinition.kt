/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.model

/**
 * Generic structure of resource in template section.
 */
data class ResourceDefinition(
    /**
     * Resource logical ID.
     */
    val id: String,

    /**
     * Resource type.
     */
    val type: String,

    /**
     * Condition handling.
     */
    val condition: String? = null,

    /**
     * List of dependencies.
     */
    val dependsOn: List<String> = emptyList(),

    /**
     * Resource properties.
     */
    val properties: Map<String, Any> = emptyMap()
)
