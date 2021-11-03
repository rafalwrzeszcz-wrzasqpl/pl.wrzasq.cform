/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.model

/**
 * CloudFormation template macro processing response structure.
 */
data class CloudFormationMacroResponse(
    /**
     * Request ID.
     */
    val requestId: String,

    /**
     * Operation status.
     */
    val status: String = "SUCCESS",

    /**
     * Template fragment.
     */
    val fragment: Map<String, Any>
)
