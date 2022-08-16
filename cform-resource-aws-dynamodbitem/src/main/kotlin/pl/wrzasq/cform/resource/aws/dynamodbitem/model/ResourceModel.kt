/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.dynamodbitem.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.json.JSONObject
import software.amazon.awssdk.core.BytesWrapper
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest

/**
 * AWS account resource description.
 */
class ResourceModel {
    /**
     * Target table name.
     */
    @JsonProperty("TableName")
    var tableName: String? = null

    /**
     * Record identifier.
     */
    @JsonProperty("Id")
    var id: String? = null

    /**
     * Object key structure.
     */
    @JsonProperty("Key")
    var key: Map<String, Map<String, Any>>? = null

    /**
     * Additional data fields.
     */
    @JsonProperty("Data")
    var data: Map<String, Map<String, Any>> = emptyMap()

    /**
     * Persistem item properties.
     */
    @JsonProperty("Item")
    var item: Map<String, Map<String, Any>>? = null

    /**
     * Primary identifier of the resource.
     */
    @get:JsonIgnore
    val primaryIdentifier: JSONObject?
        get() {
            val identifier = JSONObject()

            if (tableName != null) {
                identifier.put(IDENTIFIER_KEY_TABLE_NAME, tableName)
            }
            if (id != null) {
                identifier.put(IDENTIFIER_KEY_ID, id)
            }

            // only return the identifier if it can be used, i.e. if all components are present
            return if (identifier.isEmpty) null else identifier
        }

    /**
     * All of the unique identifiers.
     */
    @get:JsonIgnore
    val additionalIdentifiers: List<Any>? = null

    companion object {
        /**
         * CloudFormation resource type.
         */
        @JsonIgnore
        val TYPE_NAME = "WrzasqPl::AWS::DynamoDbItem"

        /**
         * Property path to table name.
         */
        @JsonIgnore
        val IDENTIFIER_KEY_TABLE_NAME = "/properties/TableName"

        /**
         * Property path to record ID.
         */
        @JsonIgnore
        val IDENTIFIER_KEY_ID = "/properties/Id"
    }
}

/**
 * Request to read a resource.
 *
 * @return AWS service request to read resource tgs.
 */
fun ResourceModel.toReadRequest(): GetItemRequest = GetItemRequest.builder()
    .tableName(tableName)
    .key(key?.mapValues { convertToAttribute(it.value) })
    .build()

/**
 * Request to create a resource.
 *
 * @return AWS service request to create a resource.
 */
fun ResourceModel.toCreateRequest(): PutItemRequest = PutItemRequest.builder()
    .tableName(tableName)
    .item(((key ?: emptyMap()) + data).mapValues { convertToAttribute(it.value) })
    .build()

/**
 * Request to delete a resource.
 *
 * @return AWS service request to delete a resource.
 */
fun ResourceModel.toDeleteRequest(): DeleteItemRequest = DeleteItemRequest.builder()
    .tableName(tableName)
    .key(key?.mapValues { convertToAttribute(it.value) })
    .build()

/**
 * Translates resource object from SDK into a resource model.
 *
 * @param describeResponse AWS service describe resource response.
 * @param model Input model for identity properties.
 * @return Resource model.
 */
fun fromReadResponse(
    describeResponse: GetItemResponse,
    model: ResourceModel
) = ResourceModel().apply {
    tableName = model.tableName
    id = model.id
    key = model.key
    data = model.data
    item = describeResponse.item().mapValues { convertToMap(it.value) }
}

private fun convertToCollection(value: Any?) = if (value is Collection<*>) {
    value.map(Any?::toString)
} else {
    listOf(value.toString())
}

private fun convertToAttribute(value: Map<*, Any?>): AttributeValue = value
    .mapKeys { it.key.toString() }
    .entries
    .fold(AttributeValue.builder()) { accumulator, entry ->
        when (entry.key) {
            "S" -> accumulator.s(entry.value.toString())
            "N" -> accumulator.n(entry.value.toString())
            "B" -> accumulator.b(SdkBytes.fromUtf8String(entry.value.toString()))
            "SS" -> accumulator.ss(convertToCollection(entry.value))
            "NS" -> accumulator.ns(convertToCollection(entry.value))
            "BS" -> accumulator.bs(convertToCollection(entry.value).map(SdkBytes::fromUtf8String))
            "M" -> {
                val target = mutableMapOf<String, AttributeValue>()
                val nested = entry.value // used for smart-case

                if (nested is Map<*, *>) {
                    nested.forEach { (key, single) ->
                        if (single is Map<*, *>) {
                            target[key.toString()] = convertToAttribute(single)
                        }
                    }
                }

                accumulator.m(target)
            }
            "L" -> {
                val nested = entry.value // used for smart-case

                if (nested is Collection<*>) {
                    accumulator.l(
                        nested
                            .filterIsInstance<Map<*, *>>()
                            .map(::convertToAttribute)
                    )
                }

                accumulator
            }
            "BOOL" -> accumulator.bool(entry.value.toString().lowercase() != "false")
            "NUL" -> accumulator.nul(true)
            else -> accumulator
        }
    }
        .build()

private fun convertToMap(value: AttributeValue): Map<String, Any> {
    val target = mutableMapOf<String, Any>()

    value.s()?.let { target["S"] = it }
    value.n()?.let { target["N"] = it }
    value.b()?.let { target["B"] = it.asUtf8String() }
    if (value.hasSs()) target["SS"] = value.ss()
    if (value.hasNs()) target["NS"] = value.ns()
    if (value.hasBs()) target["BS"] = value.bs().map(BytesWrapper::asUtf8String)
    if (value.hasM()) target["M"] = value.m().mapValues { convertToMap(it.value) }
    if (value.hasL()) target["L"] = value.l().map(::convertToMap)
    value.bool()?.let { target["BOOL"] = it }
    value.nul()?.let { target["NUL"] = it }

    return target
}
