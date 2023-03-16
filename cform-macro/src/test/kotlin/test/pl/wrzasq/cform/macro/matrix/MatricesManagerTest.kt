/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2023 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.matrix

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.wrzasq.cform.macro.matrix.MatricesManager
import pl.wrzasq.cform.macro.template.CALL_REF
import pl.wrzasq.cform.macro.template.CALL_SUB
import pl.wrzasq.cform.macro.template.Fn

class MatricesManagerTest {
    @Test
    fun expandUnknownApi() {
        assertThrows<IllegalStateException> { MatricesManager().expand(CALL_REF to "Matrix:Unknown[Foo=Bar]") }
    }

    @Test
    fun expandComplexArgument() {
        val input = Fn.sub(
            listOf(
                Fn.fnIf("Check", "\${Foo}", "\${Bar}")
            )
        )

        assertEquals(input, MatricesManager().expand(CALL_SUB to input.values.first()))
    }
}
