/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.template

import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.processors.DelegatingResourceProcessor
import pl.wrzasq.cform.macro.processors.types.SecretStructure
import test.pl.wrzasq.cform.macro.TemplateTest

class SecretStructureTest : TemplateTest() {
    override val processor = DelegatingResourceProcessor(SecretStructure())::process

    @Test
    fun processTemplate() = processTemplate("SecretStructure")
}
