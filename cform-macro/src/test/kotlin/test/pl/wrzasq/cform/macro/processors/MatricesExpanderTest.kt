/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2023 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.processors

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.processors.MatricesExpander

class MatricesExpanderTest {
    @Test
    fun processNoNeed() {
        val processor = MatricesExpander()
        val input = emptyMap<String, Any>()

        assertSame(input, processor.expand(input, emptyMap()))
    }
}
