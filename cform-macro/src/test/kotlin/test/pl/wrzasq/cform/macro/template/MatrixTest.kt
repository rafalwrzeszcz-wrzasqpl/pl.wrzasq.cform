/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2023 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.template

import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.processors.MatricesExpander
import test.pl.wrzasq.cform.macro.TemplateTest

class MatrixTest : TemplateTest(mapOf("MultipleValuesParam" to "https://wrzasq.pl,https://ivms.online")) {
    override val processor = MatricesExpander()::expand

    @Test
    fun processTemplate() = processTemplate("Matrix")
}
