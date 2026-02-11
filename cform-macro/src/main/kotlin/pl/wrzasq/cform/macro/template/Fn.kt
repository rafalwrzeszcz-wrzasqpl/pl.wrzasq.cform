/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 - 2024, 2026 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.template

/**
 * `Ref` definition.
 */
const val CALL_REF = "Ref"

/**
 * `Fn::GetAtt` definition.
 */
const val CALL_GET_ATT = "Fn::GetAtt"

/**
 * `Fn::Length` definition (AWS::LanguageExtensions transform).
 */
const val CALL_LENGTH = "Fn::Length"

/**
 * `Fn::ImportValue` definition.
 */
const val CALL_IMPORT_VALUE = "Fn::ImportValue"

/**
 * `Fn::Sub` definition.
 */
const val CALL_SUB = "Fn::Sub"

/**
 * `Fn::ToJsonString` definition (AWS::LanguageExtensions transform).
 */
const val CALL_TO_JSON_STRING = "Fn::ToJsonString"

/**
 * `Fn::If` definition.
 */
const val CALL_IF = "Fn::If"

/**
 * `Fn::GetParam` definition.
 */
const val CALL_GET_PARAM = "Fn::GetParam"

/**
 * CloudFormation equivalent of `null`.
 */
val NO_VALUE = Fn.ref("AWS::NoValue")

/**
 * Utilities for intrinsic CloudFormation operations.
 */
object Fn {
    /**
     * Returns !Ref reference call.
     *
     * @param reference Referred object ID.
     * @return !Ref call.
     */
    fun ref(reference: String) = mapOf(CALL_REF to reference)

    /**
     * Returns !GetAtt reference call.
     *
     * @param resource Resource object ID.
     * @param attribute Attribute name.
     * @return !GetAtt call.
     */
    fun getAtt(resource: String, attribute: String) = mapOf(CALL_GET_ATT to listOf(resource, attribute))

    /**
     * Returns !ImportValue reference call.
     *
     * @param target Exported value reference.
     * @return !ImportValue call.
     */
    fun importValue(target: Any) = mapOf(CALL_IMPORT_VALUE to target)

    /**
     * Returns !Sub reference call.
     *
     * @param params Call parameters.
     * @return !Sub call.
     */
    fun sub(params: Any) = mapOf(CALL_SUB to params)

    /**
     * Returns !ToJsonString reference call.
     *
     * @param input Input structure.
     * @return !ToJsonString call.
     */
    fun toJsonString(input: Any) = mapOf(CALL_TO_JSON_STRING to input)

    /**
     * Builds !If call.
     *
     * @param condition Condition name.
     * @param whenTrue Value in case of positive case.
     * @param whenFalse Value in case of negative case.
     * @return !If call.
     */
    fun fnIf(condition: String, whenTrue: Any, whenFalse: Any) = mapOf(
        CALL_IF to listOf(condition, whenTrue, whenFalse),
    )

    /**
     * Wraps existing expression to !Sub call.
     *
     * @param current Existing expression.
     * @param producer Template string producer.
     * @return !Sub call.
     */
    fun wrapSub(current: Any, producer: (String) -> String): Map<String, Any> {
        // handle some simple cases of existing calls
        // note that these calls may have nested complex arguments, so we don't want to go too deep
        if (current is Map<*, *> && current.size == 1) {
            val key = current.keys.first().toString()
            val value = current[key]

            if (key == CALL_REF && value is String) {
                return sub(producer("\${$value}"))
            }

            if (key == CALL_GET_ATT) {
                if (value is String) {
                    return sub(producer("\${$value}"))
                } else if (value is List<*> && value[0] is String && value[1] is String) {
                    return sub(producer("\${${value[0]}.${value[1]}}"))
                }
            }

            // our simplified notation - will be expanded in post-processing
            if (key == CALL_IMPORT_VALUE && value is String) {
                return sub(producer("\${Import:$value}"))
            }

            if (key == CALL_SUB) {
                if (value is String) {
                    return sub(producer(value))
                } else if (value is List<*> && value[0] is String) {
                    return sub(listOf(producer(value[0].toString()), value[1]))
                }
            }
        } else if (current is String) {
            // escape `${` sequences as plain string becomes part of !Sub call
            return sub(producer(current.toString().replace("\${", "\${!")))
        }

        // fallback to nested call
        return sub(listOf(producer("\${wrapped}"), mapOf("wrapped" to current)))
    }
}
