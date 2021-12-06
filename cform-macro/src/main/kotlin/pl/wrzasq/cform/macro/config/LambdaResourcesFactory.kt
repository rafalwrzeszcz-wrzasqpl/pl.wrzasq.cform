/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.config

import pl.wrzasq.cform.macro.Handler
import pl.wrzasq.cform.macro.processors.ApiGatewayDefinition
import pl.wrzasq.cform.macro.processors.AutomaticLogGroups
import pl.wrzasq.cform.macro.processors.DelegatingResourceProcessor
import pl.wrzasq.cform.macro.processors.FnToolkit
import pl.wrzasq.cform.macro.processors.types.CodeBuildSetup
import pl.wrzasq.cform.macro.processors.types.DynamoDbAttributesDefinitions
import pl.wrzasq.cform.macro.processors.types.IamStatements
import pl.wrzasq.cform.macro.processors.types.PipelineDefinition
import pl.wrzasq.cform.macro.template.CallsExpander
import pl.wrzasq.commons.aws.runtime.NativeLambdaApi
import pl.wrzasq.commons.aws.runtime.config.ResourcesFactory
import pl.wrzasq.commons.json.ObjectMapperFactory

/**
 * Resources factory for AWS Lambda environment.
 */
class LambdaResourcesFactory : ResourcesFactory {
    private val processors by lazy {
        listOf(
            // note that some orders matter - eg we need to processor our custom blocks first as most other processors
            // will only handle standard template sections
            apiGatewayDefinitionProcessor::process,
            // from now on we should only have standard template sections
            automaticLogGroupsProcessor::process,
            delegatingResourceProcessor::process,
            // this needs to be last one ase we rely on our custom notations in other processors
            fnToolkitProcessor::processTemplate
        )
    }

    private val apiGatewayDefinitionProcessor by lazy { ApiGatewayDefinition() }

    private val automaticLogGroupsProcessor by lazy { AutomaticLogGroups() }

    private val delegatingResourceProcessor by lazy {
        DelegatingResourceProcessor(
            codeBuildSetupProcessor,
            dynamoDbAttributesDefinitionsProcessor,
            iamStatementsProcessor,
            pipelineDefinitionProcessor
        )
    }

    private val fnToolkitProcessor by lazy { CallsExpander(FnToolkit()) }

    private val codeBuildSetupProcessor by lazy { CodeBuildSetup() }

    private val dynamoDbAttributesDefinitionsProcessor by lazy { DynamoDbAttributesDefinitions() }

    private val iamStatementsProcessor by lazy { IamStatements() }

    private val pipelineDefinitionProcessor by lazy { PipelineDefinition() }

    private val objectMapper by lazy { ObjectMapperFactory.createObjectMapper() }

    private val handler by lazy { Handler(objectMapper, processors) }

    override val lambdaApi by lazy { NativeLambdaApi(objectMapper) }

    override val lambdaCallback = handler::handle
}
