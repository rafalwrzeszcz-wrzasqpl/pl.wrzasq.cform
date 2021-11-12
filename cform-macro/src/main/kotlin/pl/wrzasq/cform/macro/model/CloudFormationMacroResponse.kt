/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.model

/**
 * CloudFormation template macro processing response structure.
 *
 * @property requestId Request ID.
 * @property status Operation status.
 * @property fragment Template fragment.
 */
data class CloudFormationMacroResponse(
    val requestId: String,
    val status: String = "SUCCESS",
    val fragment: Map<String, Any>
)
