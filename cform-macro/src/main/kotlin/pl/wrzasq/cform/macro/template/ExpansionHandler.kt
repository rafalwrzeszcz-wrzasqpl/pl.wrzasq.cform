/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.template

/**
 * Specification for call expansion handler.
 */
interface ExpansionHandler {
    /**
     * Checks whether given function can be handled.
     *
     * @param function Function name.
     * @return Whether given call can be handled.
     */
    fun canHandle(function: String): Boolean

    /**
     * Modifies call.
     *
     * @param input Initial value.
     * @return Processed call.
     */
    fun expand(input: Pair<String, Any>): Map<String, Any>
}
