/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.template

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
    fun ref(reference: String) = mapOf("Ref" to reference)

    /**
     * Returns !GetAtt reference call.
     *
     * @param resource Resource object ID.
     * @param attribute Attribute name.
     * @return !GetAtt call.
     */
    fun getAtt(resource: String, attribute: String) = mapOf("Fn::GetAtt" to listOf(resource, attribute))

    /**
     * Returns !ImportValue reference call.
     *
     * @param target Exported value reference.
     * @return !ImportValue call.
     */
    fun importValue(target: Any) = mapOf("Fn::ImportValue" to target)

    /**
     * Returns !Sub reference call.
     *
     * @param params Call parameters.
     * @return !Sub call.
     */
    fun sub(params: Any) = mapOf("Fn::Sub" to params)

    /**
     * Builds !If call.
     *
     * @param condition Condition name.
     * @param whenTrue Value in case of positive case.
     * @param whenFalse Value in case of negative case.
     * @return !If call.
     */
    fun fnIf(condition: String, whenTrue: Any, whenFalse: Any) = mapOf(
        "Fn::If" to listOf(condition, whenTrue, whenFalse)
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
        // note that these calls may have nested complex arguments so we don't want to go too deep
        if (current is Map<*, *> && current.size == 1) {
            val key = current.keys.first().toString()
            val value = current[key]

            if (key == "Ref" && value is String) {
                return sub(producer("\${$value}"))
            }

            if (key == "Fn::GetAtt") {
                if (value is String) {
                    return sub(producer("\${$value}"))
                } else if (value is List<*> && value[0] is String && value[1] is String) {
                    return sub(producer("\${${value[0]}.${value[1]}}"))
                }
            }

            // our simplified notation - will be expanded in post-processing
            if (key == "Fn::ImportValue" && value is String) {
                return sub(producer("\${Import:$value}"))
            }

            if (key == "Fn::Sub") {
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
