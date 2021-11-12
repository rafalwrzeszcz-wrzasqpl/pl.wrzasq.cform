/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.pipeline.types

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.wrzasq.cform.macro.pipeline.types.S3Source

class S3SourceTest {
    @Test
    fun noBucket() {
        assertThrows<IllegalStateException> {
            S3Source(
                "NoBucket",
                mapOf("ObjectKey" to "test.json"),
                null
            )
        }
    }

    @Test
    fun noObjectKey() {
        assertThrows<IllegalStateException> {
            S3Source(
                "NoObjectKey",
                mapOf("Bucket" to "s3name"),
                null
            )
        }
    }
}
