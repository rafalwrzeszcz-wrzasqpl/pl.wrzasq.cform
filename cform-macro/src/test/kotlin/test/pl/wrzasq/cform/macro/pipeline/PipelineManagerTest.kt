/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.pipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.wrzasq.cform.macro.pipeline.PipelineManager
import pl.wrzasq.cform.macro.pipeline.types.buildAwsActionTypeId

class PipelineManagerTest {
    @Test
    fun resolveNotExisting() {
        assertThrows<IllegalStateException> { PipelineManager().resolve("Any") }
    }

    @Test
    fun handleStageWithoutActions() {
        val input = mapOf(
            "Name" to "Nothing",
            "Actions" to emptyMap<String, Any>(),
        )

        assertFalse(PipelineManager().handleStage(input))
    }

    @Test
    fun handleStageWithoutName() {
        val input = mapOf(
            buildActions(
                "Test" to mapOf(
                    buildActionTypeId(),
                ),
            ),
        )

        assertFalse(PipelineManager().handleStage(input))
    }

    @Test
    fun handleStageWithUnknownActionType() {
        val input = mapOf(
            buildActions(
                "Test" to mapOf(
                    "ActionType" to "Whatever",
                ),
            ),
        )

        assertThrows<IllegalArgumentException> { PipelineManager().handleStage(input) }
    }

    @Test
    fun circularDependencies() {
        val input = mapOf(
            "Name" to "Circular",
            buildActions(
                "A" to mapOf(
                    buildActionTypeId(),
                    "OutputArtifacts" to listOf("A"),
                    "InputArtifacts" to listOf("B"),
                ),
                "B" to mapOf(
                    buildActionTypeId(),
                    "OutputArtifacts" to listOf("B"),
                    "InputArtifacts" to listOf("A"),
                ),
            ),
        )

        val manager = PipelineManager()

        assertTrue(manager.handleStage(input))
        assertThrows<IllegalStateException> { manager.compile() }
    }

    @Test
    fun overlappingNamespaces() {
        val input1 = mapOf(
            "Name" to "First-Deploy",
            buildActions(
                "Db" to mapOf(buildActionTypeId()),
            ),
        )
        val input2 = mapOf(
            "Name" to "First",
            buildActions(
                "Deploy-Db" to mapOf(buildActionTypeId()),
            ),
        )

        val manager = PipelineManager()
        manager.handleStage(input1)
        manager.handleStage(input2)
        manager.compile()

        val ns = manager.resolveNamespace("First-Deploy:Db")
        val ns1 = manager.resolveNamespace("First-Deploy:Db")
        val ns2 = manager.resolveNamespace("First:Deploy-Db")

        // namespaces of same action should be the same
        assertEquals(ns1, ns)
        assertNotEquals(ns1, ns2)
        assertTrue(ns1.startsWith("first-deploy-db"))
        assertTrue(ns2.startsWith("first-deploy-db"))
    }

    @Test
    fun unhandledActions() {
        val input = mapOf(
            "Name" to "Wrong",
            buildActions("A" to mapOf()),
        )

        val manager = PipelineManager()

        assertFalse(manager.handleStage(input))
    }

    private fun buildActionTypeId() = "ActionTypeId" to buildAwsActionTypeId(category = "Source", provider = "S3")

    private fun buildActions(vararg actions: Pair<String, Map<String, Any>>) = "Actions" to mapOf(*actions)
}
