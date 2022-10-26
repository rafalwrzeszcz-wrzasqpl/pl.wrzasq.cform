/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.template

import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.processors.DelegatingResourceProcessor
import pl.wrzasq.cform.macro.processors.types.ConnectContactFlow
import test.pl.wrzasq.cform.macro.TemplateTest

class ConnectContactFlowTest : TemplateTest() {
    override val processor = DelegatingResourceProcessor(ConnectContactFlow())::process

    @Test
    fun processTemplate() = processTemplate("ConnectContactFlow")
}
