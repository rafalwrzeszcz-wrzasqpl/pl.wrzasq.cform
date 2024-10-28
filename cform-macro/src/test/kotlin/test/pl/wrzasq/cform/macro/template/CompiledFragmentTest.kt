/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.macro.template

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.wrzasq.cform.macro.template.CALL_GET_PARAM
import pl.wrzasq.cform.macro.template.CALL_SUB
import pl.wrzasq.cform.macro.template.CompiledFragment
import pl.wrzasq.cform.macro.template.Fn
import pl.wrzasq.cform.macro.template.asMapAlways

class CompiledFragmentTest {
    @Test
    fun structToJsonEmpty() {
        val struct = emptyMap<String, Any>()

        assertEquals("{}", CompiledFragment(struct).raw)
    }

    @Test
    fun structToJsonPlainText() {
        val struct = mapOf("From" to "To")

        assertEquals("{\"From\":\"To\"}", CompiledFragment(struct).raw)
    }

    @Test
    fun structToJsonComplex() {
        val other = Fn.fnIf("Impossible", "True", "False")
        val struct = mapOf(
            "Plain" to "String",
            "Ref" to Fn.ref("Reference"),
            "StringAtt" to mapOf("Fn::GetAtt" to "Resource.Attr"),
            "ListAtt" to Fn.getAtt("Something", "Different"),
            "StringSub" to Fn.sub("\${Pattern}"),
            "ComplexSub" to Fn.sub(
                listOf(
                    "\${Complex}",
                    mapOf("Complex" to "NotReally"),
                ),
            ),
            "StringImport" to Fn.importValue("Export:Name"),
            "Param" to mapOf(
                CALL_GET_PARAM to listOf(
                    "artifact",
                    "file.json",
                    "Name",
                ),
            ),
            "Other" to other,
        )

        val call = CompiledFragment(struct).raw
        //assertInstanceOf(Map::class.java, call)
        assertTrue(call is Map<*, *>)

        val sub = asMapAlways(call)
        assertTrue(CALL_SUB in sub)

        val params = sub[CALL_SUB]
        //assertInstanceOf(List::class.java, params)
        assertTrue(params is List<*>)
        if (params is List<*>) {
            assertEquals(
                "{" +
                    "\"Plain\":\"String\"," +
                    "\"Ref\":\"\${Reference}\"," +
                    "\"StringAtt\":\"\${Resource.Attr}\"," +
                    "\"ListAtt\":\"\${Something.Different}\"," +
                    "\"StringSub\":\"\${Pattern}\"," +
                    "\"ComplexSub\":\"\${Complex}\"," +
                    "\"StringImport\":\"\${Import:Export:Name}\"," +
                    "\"Param\":{\"Fn::GetParam\":[\"artifact\",\"file.json\",\"Name\"]}," +
                    "\"Other\":\"\${param1}\"" +
                    "}",
                params[0],
            )
            //assertInstanceOf(Map::class.java, params[1])
            assertTrue(params[1] is Map<*, *>)

            val values = asMapAlways(params[1])
            assertEquals("NotReally", values["Complex"])
            assertSame(other, values["param1"])
        }
    }

    @Test
    fun structToJsonNested() {
        val struct = mapOf(
            "List" to listOf(
                mapOf(
                    "First" to Fn.ref("One"),
                ),
            ),
            "Dict" to mapOf(
                "Outer" to mapOf(
                    "Inner" to Fn.ref("Two"),
                ),
            ),
        )

        val call = CompiledFragment(struct).raw
        //assertInstanceOf(Map::class.java, call)
        assertTrue(call is Map<*, *>)

        val sub = asMapAlways(call)
        assertTrue(CALL_SUB in sub)

        val json = sub[CALL_SUB]
        //assertInstanceOf(List::class.java, params)
        assertTrue(json is String)
        assertEquals(
            "{" +
                "\"List\":[{" +
                "\"First\":\"\${One}\"" +
                "}]," +
                "\"Dict\":{" +
                "\"Outer\":{" +
                "\"Inner\":\"\${Two}\"" +
                "}}" +
                "}",
            json,
        )
    }
}
