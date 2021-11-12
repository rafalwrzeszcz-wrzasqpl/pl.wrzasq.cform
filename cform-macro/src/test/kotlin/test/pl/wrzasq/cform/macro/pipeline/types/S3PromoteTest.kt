/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.pipeline.types

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.macro.pipeline.PipelineAction
import pl.wrzasq.cform.macro.pipeline.PipelineManager
import pl.wrzasq.cform.macro.pipeline.types.S3Promote

@ExtendWith(MockKExtension::class)
class S3PromoteTest {
    @MockK
    lateinit var manager: PipelineManager

    @MockK
    lateinit var action: PipelineAction

    @Test
    fun noAuthorizer() {
        val ref = "Stage:Action"

        every { manager.resolve(ref) } returns action

        assertThrows<IllegalStateException> {
            S3Promote("Test", mapOf("Source" to ref), null).compile(manager)
        }
    }
}
