/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * CloudFormation template macro processing response structure.
 *
 * @property requestId Request ID.
 * @property status Operation status.
 * @property fragment Template fragment.
 */
data class CloudFormationMacroResponse(
    @JsonProperty("requestId")
    val requestId: String,
    @JsonProperty("status")
    val status: String = "SUCCESS",
    @JsonProperty("fragment")
    val fragment: Map<String, Any>
)
