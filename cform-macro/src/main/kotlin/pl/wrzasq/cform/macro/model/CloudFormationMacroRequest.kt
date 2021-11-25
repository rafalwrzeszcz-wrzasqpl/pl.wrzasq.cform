/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * CloudFormation template macro processing request structure.
 *
 * @property requestId Request ID.
 * @property fragment Template fragment.
 */
data class CloudFormationMacroRequest(
    @JsonProperty("requestId")
    val requestId: String,
    @JsonProperty("fragment")
    val fragment: Map<String, Any>
)
