/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.template

import pl.wrzasq.cform.macro.model.ResourceDefinition

/**
 * Parameters section key.
 */
const val SECTION_PARAMETERS = "Parameters"

/**
 * Conditions section key.
 */
const val SECTION_CONDITIONS = "Conditions"

/**
 * Resources section key.
 */
const val SECTION_RESOURCES = "Resources"

/**
 * Resources section key.
 */
const val SECTION_OUTPUTS = "Outputs"

/**
 * Type key.
 */
const val PROPERTY_KEY_TYPE = "Type"

/**
 * DependsOn key.
 */
const val PROPERTY_KEY_DEPENDSON = "DependsOn"

/**
 * Condition key.
 */
const val PROPERTY_KEY_CONDITION = "Condition"

/**
 * Properties key.
 */
const val PROPERTY_KEY_PROPERTIES = "Properties"

/**
 * Safely converts value to a typed map.
 *
 * @param input Input object.
 * @return Typed map.
 */
fun asMap(input: Any): Map<String, Any> {
    val output = mutableMapOf<String, Any>()
    if (input is Map<*, *>) {
        for ((key, value) in input) {
            if (value != null) {
                output[key.toString()] = value
            }
        }
    }
    return output
}

/**
 * Converts optional value to a typed map.
 *
 * @param input Input object.
 * @return Typed map.
 */
fun asMapAlways(input: Any?) = input?.let(::asMap) ?: emptyMap()

/**
 * Handles optional property by removing it from generic properties pool.
 *
 * @param key Property key.
 * @param then Property action.
 * @param defaultValue Default property value.
 * @return Map without consumed key.
 */
fun Map<String, Any>.popProperty(key: String, then: (Any) -> Unit, defaultValue: Any? = null): Map<String, Any> {
    if (containsKey(key)) {
        this[key]?.let(then)
        return filterKeys { it != key }
    } else if (defaultValue != null) {
        then(defaultValue)
    }
    return this
}

/**
 * Performs mapping only for selected entries in the map.
 *
 * @param handlers Mappers for selected keys.
 * @return Mapped structure.
 */
fun Map<String, Any>.mapSelected(vararg handlers: Pair<String, (Any) -> Any>) = mapValues {
    mapOf(*handlers)[it.key]?.invoke(it.value) ?: it.value
}

/**
 * Performs mapping only for single selected key in map.
 *
 * @param key Desired key.
 * @param handler Mapping function.
 * @return Mapped structure.
 */
fun Map<String, Any>.mapSelected(key: String, handler: (Any) -> Any) = mapSelected(key to handler)

/**
 * Performs mapping of only values of each pair.
 *
 * @param transform Mapping function.
 * @return Mapped structure.
 */
fun Map<String, Any>.mapValuesOnly(transform: (Any) -> Any) = mapValues { transform(it.value) }

/**
 * Rebuilds resource definition with different properties.
 *
 * @param input Initial resource definition.
 * @param properties New properties.
 * @return New definition.
 */
fun rebuildResource(
    input: Any,
    properties: Map<String, Any>
) = asMap(input) + mapOf(PROPERTY_KEY_PROPERTIES to properties)

/**
 * Creates resource definition structure.
 *
 * @param model Resource model.
 * @return Resource structure.
 */
fun createResource(model: ResourceDefinition): Pair<String, Map<String, Any>> {
    val resource = mutableMapOf<String, Any>(PROPERTY_KEY_TYPE to model.type)
    if (!model.condition.isNullOrEmpty()) {
        resource[PROPERTY_KEY_CONDITION] = model.condition
    }
    if (model.dependsOn.isNotEmpty()) {
        resource[PROPERTY_KEY_DEPENDSON] = model.dependsOn
    }
    if (model.properties.isNotEmpty()) {
        resource[PROPERTY_KEY_PROPERTIES] = model.properties
    }
    return model.id to resource
}

/**
 * Converts template fragment to resource model.
 *
 * @param id Logical ID.
 * @param input Template fragment.
 * @return Model structure.
 */
fun asDefinition(id: String, input: Any): ResourceDefinition {
    val data = asMap(input)

    return ResourceDefinition(
        id = id,
        // dummy fallback, should never be needed
        type = data[PROPERTY_KEY_TYPE]?.toString() ?: "",
        condition = data[PROPERTY_KEY_CONDITION]?.toString(),
        dependsOn = data[PROPERTY_KEY_DEPENDSON]?.let {
            if (it is List<*>) it.filterNotNull().map(Any::toString) else emptyList()
        } ?: emptyList(),
        properties = data[PROPERTY_KEY_PROPERTIES]?.let(::asMap) ?: emptyMap()
    )
}

/**
 * Model method binding.
 *
 * @return Plain map structure.
 */
fun ResourceDefinition.build() = createResource(this)

/**
 * Model method binding.
 *
 * @param key Property key.
 * @param then Property action.
 * @param defaultValue Default property value.
 * @return Map without consumed key.
 */
fun ResourceDefinition.popProperty(key: String, then: (Any) -> Unit, defaultValue: Any? = null) = copy(
    properties = properties.popProperty(key, then, defaultValue)
)
