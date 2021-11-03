/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.config

import pl.wrzasq.cform.macro.TemplateProcessor
import pl.wrzasq.commons.aws.runtime.NativeLambdaApi

/**
 * Factory for resource handler resources.
 */
interface ResourcesFactory {
    /**
     * Native AWS Lambda runtime handler.
     */
    val api: NativeLambdaApi

    /**
     * Template processors that implement macros steps.
     */
    val processors: Collection<TemplateProcessor>
}
