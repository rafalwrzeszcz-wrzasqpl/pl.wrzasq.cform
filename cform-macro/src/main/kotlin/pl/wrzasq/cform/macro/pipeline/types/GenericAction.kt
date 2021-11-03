/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.pipeline.types

/**
 * Any action type specified directly.
 *
 * @param name Stage name.
 * @param properties Extra properties.
 * @param category Category of action.
 * @param owner Action vendor.
 * @param provider Providing service.
 * @param version Definition version.
 * @param condition Condition name.
 */
open class GenericAction(
    name: String,
    properties: Map<String, Any>,
    private val category: String,
    private val owner: String,
    private val provider: String,
    private val version: String,
    condition: String?
) : BaseAction(name, properties, condition) {
    override fun buildActionTypeId() = buildAwsActionTypeId(category, provider, owner, version)
}

/**
 * Builds action definition from template fragment.
 *
 * @param input Type definition properties.
 */
fun fromMap(input: Map<String, Any>) = { name: String, properties: Map<String, Any>, condition: String? ->
    GenericAction(
        name,
        properties,
        input["Category"].toString(),
        (input["Owner"] ?: "AWS").toString(),
        input["Provider"].toString(),
        (input["Version"] ?: "1").toString(),
        condition
    )
}
