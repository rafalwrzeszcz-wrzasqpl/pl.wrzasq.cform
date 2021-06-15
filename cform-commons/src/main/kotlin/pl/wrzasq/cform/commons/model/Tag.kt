/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.commons.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Generic resource tag model.
 */
class Tag {
    /**
     * Tag name.
     */
    @JsonProperty("Key")
    lateinit var key: String

    /**
     * Tag value.
     */
    @JsonProperty("Value")
    lateinit var value: String
}
