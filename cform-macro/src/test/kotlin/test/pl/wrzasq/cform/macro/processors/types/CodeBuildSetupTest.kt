package test.pl.wrzasq.cform.macro.processors.types

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.processors.types.CodeBuildSetup

class CodeBuildSetupTest {
    @Test
    fun nonS3Artifacts() {
        val entry = buildProjectDefinition("Artifacts" to mapOf("Local" to true))

        val action = CodeBuildSetup()
        val result = action.handle(entry)

        assertFalse("Type" in result)
    }

    @Test
    fun expandedCache() {
        val cache = mapOf("Type" to "S3", "Plain" to true)
        val entry = buildProjectDefinition("Cache" to cache)

        val action = CodeBuildSetup()
        val result = action.handle(entry)

        assertSame(cache, result["Cache"])
    }

    @Test
    fun plainVariables() {
        val variables = listOf(
            mapOf("Name" to "ServiceVersion", "Value" to "v1")
        )
        val entry = buildProjectDefinition("Environment" to mapOf("EnvironmentVariables" to variables))

        val action = CodeBuildSetup()
        val result = action.handle(entry)

        val environment = result["Environment"]
        //assertIsInstance()
        assertTrue(environment is Map<*, *>)
        if (environment is Map<*, *>) {
            assertSame(variables, environment["EnvironmentVariables"])
        }
    }

    private fun buildProjectDefinition(vararg properties: Pair<String, Any>) = ResourceDefinition(
        id = "Test",
        type = "AWS::CodeBuild::Project",
        properties = mapOf(*properties)
    )
}
