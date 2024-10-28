/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.commons.config

import org.json.JSONObject
import org.json.JSONTokener

private const val SCHEMA_DEFAULT_FILENAME = "schema.json"

/**
 * Resource handler configuration for running in AWS Lambda environment.
 *
 * @param schemaFilename Resource filename to read schema from.
 */
class LambdaConfiguration(
    private val schemaFilename: String = SCHEMA_DEFAULT_FILENAME,
) : Configuration {
    override val resourceSchema: JSONObject by lazy {
        JSONObject(JSONTokener(javaClass.classLoader.getResourceAsStream(schemaFilename)))
    }
}
