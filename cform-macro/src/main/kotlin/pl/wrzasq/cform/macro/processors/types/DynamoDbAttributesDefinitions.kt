/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors.types

import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.template.asMap

/**
 * Populates DynamoDB key attributes definition.
 */
class DynamoDbAttributesDefinitions : ResourceHandler {
    override fun handledResourceTypes() = listOf("AWS::DynamoDB::GlobalTable", "AWS::DynamoDB::Table")

    override fun handle(entry: ResourceDefinition) = processTable(entry.properties)

    private fun processTable(input: Map<String, Any>): Map<String, Any> {
        val attributesDefinitions = input.getList("AttributeDefinitions").toMutableList()
        // both index types have same structures
        val indices = input.getList("GlobalSecondaryIndexes") + input.getList("LocalSecondaryIndexes")
        val keys = input.extractKeyAttributeNames() + indices.flatMap { asMap(it).extractKeyAttributeNames() }

        val existing = attributesDefinitions.extractAttributeNames()
        // we sort names to ensure predictable order
        (keys.toSet() subtract existing.toSet()).sorted().forEach {
            attributesDefinitions.add(
                mapOf(
                    "AttributeName" to it,
                    "AttributeType" to "S",
                ),
            )
        }

        return input + mapOf("AttributeDefinitions" to attributesDefinitions)
    }
}

private fun Map<String, Any>.getList(key: String): List<Any> {
    val value = this[key]
    if (value is List<*>) {
        return value.filterNotNull()
    }

    // default fallback
    return emptyList()
}

private fun List<Any>.extractAttributeNames() = this.map { asMap(it)["AttributeName"].toString() }

private fun Map<String, Any>.extractKeyAttributeNames() = this.getList("KeySchema").extractAttributeNames()
