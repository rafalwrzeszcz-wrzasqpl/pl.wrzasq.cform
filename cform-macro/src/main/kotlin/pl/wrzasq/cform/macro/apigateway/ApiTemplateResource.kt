/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.apigateway

import pl.wrzasq.cform.macro.template.Fn

/**
 * Exposes template methods for resource.
 */
interface ApiTemplateResource {
    /**
     * Generates logical resource ID within template.
     *
     * @return CloudFormation resource ID.
     */
    val resourceId: String
}

/**
 * Generates reference to a resource.
 *
 * @return !Ref call.
 */
fun ApiTemplateResource.ref() = Fn.ref(resourceId)

/**
 * Generates reference to a resource attribute.
 *
 * @param attribute Attribute name.
 * @return !GetAtt call.
 */
fun ApiTemplateResource.getAtt(attribute: String) = Fn.getAtt(resourceId, attribute)
