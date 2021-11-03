/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.model

/**
 * CloudFormation template macro processing request structure.
 */
data class CloudFormationMacroRequest(
    /**
     * Request ID.
     */
    val requestId: String,

    /**
     * Template fragment.
     */
    val fragment: Map<String, Any>
)
