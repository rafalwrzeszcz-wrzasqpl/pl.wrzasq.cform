/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.commons.config

import org.json.JSONObject

/**
 * Resource handler specification.
 */
interface Configuration {
    /**
     * Resource definition schema.
     */
    val resourceSchema: JSONObject
}
