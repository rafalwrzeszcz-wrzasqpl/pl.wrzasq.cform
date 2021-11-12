package test.pl.wrzasq.cform.macro.processors.types

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.processors.types.CodeBuildSetup

class CodeBuildSetupTest {
    @Test
    fun nonS3Artifacts() {
        val entry = ResourceDefinition(
            id = "Test",
            type = "AWS::CodeBuild::Project",
            properties = mapOf(
                "Artifacts" to mapOf("Local" to true)
            )
        )

        val action = CodeBuildSetup()
        val result = action.handle(entry)

        assertFalse("Type" in result)
    }

    @Test
    fun expandedCache() {
        val cache = mapOf("Type" to "S3", "Plain" to true)
        val entry = ResourceDefinition(
            id = "Test",
            type = "AWS::CodeBuild::Project",
            properties = mapOf("Cache" to cache)
        )

        val action = CodeBuildSetup()
        val result = action.handle(entry)

        assertSame(cache, result["Cache"])
    }
}
