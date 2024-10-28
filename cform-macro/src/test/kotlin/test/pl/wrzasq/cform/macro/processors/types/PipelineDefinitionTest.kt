/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.processors.types

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.processors.types.PipelineDefinition
import pl.wrzasq.cform.macro.template.Fn

class PipelineDefinitionTest {
    @Test
    fun handleUnchanged() {
        val input = Fn.fnIf("SomeCheck", 1, 2)

        val result = PipelineDefinition().handle(
            ResourceDefinition(
                id = "Foo",
                type = "AWS::CodePipeline::Pipeline",
                properties = mapOf(
                    "ArtifactStores" to input,
                    "Stages" to input,
                ),
            ),
        )

        // these should be left untouched if not of type that is handled by processor
        assertSame(input, result["ArtifactStores"])
        assertSame(input, result["Stages"])
    }
}
