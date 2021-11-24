/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.pipeline.types

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
//import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.macro.pipeline.PipelineManager
import pl.wrzasq.cform.macro.pipeline.types.CloudFormationDeploy
import pl.wrzasq.cform.macro.template.Fn
import pl.wrzasq.cform.macro.template.asMapAlways

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
    fun buildConfigurationPlainText() {
        val configuration = buildConfiguration(
            mapOf(
                "From" to "To"
            )
        )

        assertEquals("{\"From\":\"To\"}", configuration[PARAMETER_OVERRIDES])
    }

    @Test
    fun buildConfiguration() {
        val other = Fn.fnIf("Impossible", "True", "False")
        val configuration = buildConfiguration(
            mapOf(
                "Plain" to "String",
                "Ref" to Fn.ref("Reference"),
                "StringAtt" to mapOf("Fn::GetAtt" to "Resource.Attr"),
                "ListAtt" to Fn.getAtt("Something", "Different"),
                "StringSub" to Fn.sub("\${Pattern}"),
                "ComplexSub" to Fn.sub(
                    listOf(
                        "\${Complex}",
                        mapOf("Complex" to "NotReally")
                    )
                ),
                "StringImport" to Fn.importValue("Export:Name"),
                "Param" to mapOf(
                    "Fn::GetParam" to listOf(
                        "artifact",
                        "file.json",
                        "Name"
                    )
                ),
                "Other" to other
            )
        )

        val call = configuration[PARAMETER_OVERRIDES]
        //assertInstanceOf(Map::class.java, call)
        assertTrue(call is Map<*, *>)

        val sub = asMapAlways(call)
        assertTrue("Fn::Sub" in sub)

        val params = sub["Fn::Sub"]
        //assertInstanceOf(List::class.java, params)
        assertTrue(params is List<*>)
        if (params is List<*>) {
            assertEquals(
                "{" +
                    "\"Plain\":\"String\"," +
                    "\"Ref\":\"\${Reference}\"," +
                    "\"StringAtt\":\"\${Resource.Attr}\"," +
                    "\"ListAtt\":\"\${Something.Different}\"," +
                    "\"StringSub\":\"\${Pattern}\"," +
                    "\"ComplexSub\":\"\${Complex}\"," +
                    "\"StringImport\":\"\${Import:Export:Name}\"," +
                    "\"Param\":{\"Fn::GetParam\":[\"artifact\",\"file.json\",\"Name\"]}," +
                    "\"Other\":\"\${param1}\"" +
                    "}",
                params[0]
            )
            //assertInstanceOf(Map::class.java, params[1])
            assertTrue(params[1] is Map<*, *>)

            val values = asMapAlways(params[1])
            assertEquals("NotReally", values["Complex"])
            assertSame(other, values["param1"])
        }
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
