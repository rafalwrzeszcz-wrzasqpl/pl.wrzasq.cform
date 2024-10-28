/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.model

/**
 * Generic structure of resource in template section.
 *
 * @property id Resource logical ID.
 * @property type Resource type.
 * @property condition Condition handling.
 * @property dependsOn List of dependencies.
 * @property properties Resource properties.
 */
data class ResourceDefinition(
    val id: String,
    val type: String,
    val condition: String? = null,
    val dependsOn: List<String> = emptyList(),
    val properties: Map<String, Any> = emptyMap(),
)
