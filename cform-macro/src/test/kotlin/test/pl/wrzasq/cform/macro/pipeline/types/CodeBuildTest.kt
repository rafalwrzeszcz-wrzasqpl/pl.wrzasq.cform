/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.pipeline.types

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.pipeline.types.CodeBuild

class CodeBuildTest {
    @Test
    fun buildConfigurationWithout() {
        val configuration = mutableMapOf<String, Any>()

        CodeBuild("Test", emptyMap(), null).buildConfiguration(configuration)

        assertFalse("ProjectName" in configuration)
    }
}
