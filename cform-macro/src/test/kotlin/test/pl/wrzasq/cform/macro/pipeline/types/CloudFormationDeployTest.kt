/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 - 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.pipeline.types

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.macro.pipeline.PipelineManager
import pl.wrzasq.cform.macro.pipeline.types.CloudFormationDeploy

private const val PARAMETER_OVERRIDES = "ParameterOverrides"

@ExtendWith(MockKExtension::class)
class CloudFormationDeployTest {
    @MockK
    lateinit var manager: PipelineManager

    @Test
    fun buildConfigurationEmpty() {
        val configuration = buildConfiguration(emptyMap())

        assertFalse(PARAMETER_OVERRIDES in configuration)
    }

    @Test
    fun buildConfigurationExists() {
        val configuration = buildConfiguration(
            mapOf(
                "From" to "To"
            )
        )

        assertEquals("{\"From\":\"To\"}", configuration[PARAMETER_OVERRIDES])
    }

    private fun buildConfiguration(input: Map<String, Any>): Map<String, Any> {
        val configuration = mutableMapOf<String, Any>()

        val action = CloudFormationDeploy(
            "Test",
            mapOf(
                "Parameters" to input
            ),
            null
        )
        action.compile(manager)
        action.buildConfiguration(configuration)
        return configuration
    }
}
