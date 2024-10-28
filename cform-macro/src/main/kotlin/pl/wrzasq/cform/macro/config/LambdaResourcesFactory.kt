/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 - 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import pl.wrzasq.cform.macro.Handler
import pl.wrzasq.cform.macro.processors.ApiGatewayDefinition
import pl.wrzasq.cform.macro.processors.AutomaticLogGroups
import pl.wrzasq.cform.macro.processors.DelegatingResourceProcessor
import pl.wrzasq.cform.macro.processors.FnToolkit
import pl.wrzasq.cform.macro.processors.MatricesExpander
import pl.wrzasq.cform.macro.processors.types.CodeBuildSetup
import pl.wrzasq.cform.macro.processors.types.ConnectContactFlow
import pl.wrzasq.cform.macro.processors.types.DynamoDbAttributesDefinitions
import pl.wrzasq.cform.macro.processors.types.IamStatements
import pl.wrzasq.cform.macro.processors.types.KinesisStreamMode
import pl.wrzasq.cform.macro.processors.types.PipelineDefinition
import pl.wrzasq.cform.macro.processors.types.SecretStructure
import pl.wrzasq.cform.macro.template.CallsExpander
import pl.wrzasq.commons.aws.runtime.NativeLambdaApi
import pl.wrzasq.commons.aws.runtime.config.ResourcesFactory

/**
 * Resources factory for AWS Lambda environment.
 */
class LambdaResourcesFactory : ResourcesFactory {
    private val processors by lazy {
        listOf(
            // note that some orders matter - e.g. we need to process our custom blocks first as most other processors
            // will only handle standard template sections
            apiGatewayDefinitionProcessor::process,
            matricesExpander::expand,
            // from now on we should only have standard template sections
            automaticLogGroupsProcessor::process,
            delegatingResourceProcessor::process,
            // this needs to be last one ase we rely on our custom notations in other processors
            fnToolkitProcessor::processTemplate,
        )
    }

    private val apiGatewayDefinitionProcessor by lazy { ApiGatewayDefinition() }

    private val matricesExpander by lazy { MatricesExpander() }

    private val automaticLogGroupsProcessor by lazy { AutomaticLogGroups() }

    private val delegatingResourceProcessor by lazy {
        DelegatingResourceProcessor(
            codeBuildSetupProcessor,
            dynamoDbAttributesDefinitionsProcessor,
            iamStatementsProcessor,
            kinesisStreamModeProcessor,
            secretStructureProcessor,
            connectContactFlowProcessor,
            pipelineDefinitionProcessor,
        )
    }

    private val fnToolkitProcessor by lazy { CallsExpander(FnToolkit()) }

    private val codeBuildSetupProcessor by lazy { CodeBuildSetup() }

    private val dynamoDbAttributesDefinitionsProcessor by lazy { DynamoDbAttributesDefinitions() }

    private val iamStatementsProcessor by lazy { IamStatements() }

    private val kinesisStreamModeProcessor by lazy { KinesisStreamMode() }

    private val secretStructureProcessor by lazy { SecretStructure() }

    private val connectContactFlowProcessor by lazy { ConnectContactFlow() }

    private val pipelineDefinitionProcessor by lazy { PipelineDefinition() }

    private val handler by lazy { Handler(OBJECT_MAPPER, processors) }

    override val lambdaApi by lazy { NativeLambdaApi(OBJECT_MAPPER) }

    override val lambdaCallback = handler::handle

    /**
     * Set of resource-related constants.
     */
    companion object {
        /**
         * Standard setup for JSON serialization handler.
         */
        val OBJECT_MAPPER by lazy {
            ObjectMapper()
                .registerModule(KotlinModule.Builder().build())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
        }
    }
}
