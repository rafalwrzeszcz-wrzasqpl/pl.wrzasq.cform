/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors.types

import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.mapSelected

/**
 * Simplifies IAM policies declaration.
 */
class IamRoleStatements : ResourceHandler {
    override fun handledResourceTypes() = listOf(
        "AWS::IAM::Group",
        "AWS::IAM::ManagedPolicy",
        "AWS::IAM::Policy",
        "AWS::IAM::Role",
        "AWS::IAM::User"
    )

    override fun handle(entry: ResourceDefinition) = if (
        entry.type == "AWS::IAM::ManagedPolicy"
        || entry.type == "AWS::IAM::Policy"
    ) {
        entry.properties.mapSelected("PolicyDocument", ::expandPolicyDocument)
    } else {
        entry.properties.mapSelected(
            mapOf(
                "Policies" to ::expandPolicies,
                "AssumeRolePolicyDocument" to ::expandPolicyDocument
            )
        )
    }

    private fun expandPolicies(input: Any) = if (input is Map<*, *>) {
        // converts map keys to policy names - map sorting is done to ensure consistent order
        asMap(input).toSortedMap().map {
            mapOf(
                "PolicyName" to it.key,
                "PolicyDocument" to expandPolicyDocument(it.value)
            )
        }
    } else {
        input
    }

    private fun expandPolicyDocument(input: Any) = if (input is List<*>) {
        // builds policy statements envelope
        mapOf(
            "Version" to "2012-10-17",
            "Statement" to input
        )
    } else {
        input
    }
}
