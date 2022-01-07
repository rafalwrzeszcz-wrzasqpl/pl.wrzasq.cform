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
 * @property codePipelineActionConfiguration CodePipeline action configuration.
 */
data class CodePipelineActionData(
    @JsonProperty("actionConfiguration")
    val codePipelineActionConfiguration: CodePipelineActionConfiguration
)
