/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.template

import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.processors.ApiGatewayDefinition
import test.pl.wrzasq.cform.macro.TemplateTest

class ApiGatewayTest : TemplateTest() {
    override val processor = ApiGatewayDefinition()::process

    @Test
    fun processTemplate() = processTemplate("ApiGateway")
}
