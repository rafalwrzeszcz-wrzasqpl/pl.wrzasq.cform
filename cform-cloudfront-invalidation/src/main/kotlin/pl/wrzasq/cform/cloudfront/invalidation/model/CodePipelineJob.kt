/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.cloudfront.invalidation.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Job configuration structure.
 *
 * @property id Job ID.
 * @property codePipelineActionData CodePipeline action data.
 */
data class CodePipelineJob(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("data")
    val codePipelineActionData: CodePipelineActionData
)
