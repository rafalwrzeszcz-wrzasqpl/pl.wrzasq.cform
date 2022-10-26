/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors.types

import pl.wrzasq.cform.macro.model.ResourceDefinition

/**
 * Automatically set Kinesis Stream mode when no capacity is specified.
 */
class KinesisStreamMode : ResourceHandler {
    override fun handledResourceTypes() = listOf("AWS::Kinesis::Stream")

    override fun handle(entry: ResourceDefinition) = processStream(entry.properties)

    private fun processStream(input: Map<String, Any>): Map<String, Any> {
        val output = input.toMutableMap()

        // if no shards count is specified use on-demand scaling by default
        if ("ShardCount" !in input) {
            output.putIfAbsent("StreamModeDetails", mapOf("StreamMode" to "ON_DEMAND"))
        }

        return output
    }
}
