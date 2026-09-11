package com.printscript.formatter

import com.printscript.lexer.Lexer
import com.printscript.lexer.StringSourceReader
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.report.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BlockFormattingTest {
    private fun formatted(
        source: String,
        settings: String,
    ): String {
        val out = StringBuilder()
        val lexer = Lexer(StringSourceReader(source), TokenRecognizers.V1_1)
        val config = Config.read(settings.byteInputStream())

        assertIs<Success<Unit>>(Formatter(config).format(lexer.tokens(), out))

        return out.toString()
    }

    private val sameLine =
        """
        let something: boolean = true;
        if (something) {
          println("Entered if");
        }
        """.trimIndent()

    private val belowLine =
        """
        let something: boolean = true;
        if (something)
        {
          println("Entered if");
        }
        """.trimIndent()

    @Test
    fun `the brace moves up onto the if line`() {
        assertEquals(sameLine, formatted(belowLine, """{ "if-brace-same-line": true }"""))
    }

    @Test
    fun `the brace moves down onto its own line`() {
        assertEquals(belowLine, formatted(sameLine, """{ "if-brace-below-line": true }"""))
    }

    @Test
    fun `a brace already where the rule wants it is left alone`() {
        assertEquals(sameLine, formatted(sameLine, """{ "if-brace-same-line": true }"""))
    }

    @Test
    fun `with no brace rule the source decides`() {
        assertEquals(belowLine, formatted(belowLine, "{}"))
        assertEquals(sameLine, formatted(sameLine, "{}"))
    }

    @Test
    fun `a block is indented by the configured width, and so is the one inside it`() {
        val formatted =
            formatted(
                """
                let something: boolean = true;
                if (something) {
                  if (something) {
                    println("Entered two ifs");
                  }
                }
                """.trimIndent(),
                """{ "indent-inside-if": 4 }""",
            )

        assertEquals(
            """
            let something: boolean = true;
            if (something) {
                if (something) {
                    println("Entered two ifs");
                }
            }
            """.trimIndent(),
            formatted,
        )
    }

    @Test
    fun `the closing brace lines up with the if and not with the body`() {
        val formatted =
            formatted(
                "let a: boolean = true;\nif (a) {\nprintln(1);\n      }",
                """{ "indent-inside-if": 2 }""",
            )

        assertEquals("let a: boolean = true;\nif (a) {\n  println(1);\n}", formatted)
    }

    @Test
    fun `an else block is indented like the block before it`() {
        val formatted =
            formatted(
                "let a: boolean = true;\nif (a) {\nprintln(1);\n} else {\nprintln(2);\n}",
                """{ "indent-inside-if": 2 }""",
            )

        assertEquals(
            "let a: boolean = true;\nif (a) {\n  println(1);\n} else {\n  println(2);\n}",
            formatted,
        )
    }

    @Test
    fun `an indent of zero flattens the block instead of leaving it alone`() {
        val formatted =
            formatted(
                "let a: boolean = true;\nif (a) {\n        println(1);\n}",
                """{ "indent-inside-if": 0 }""",
            )

        assertEquals("let a: boolean = true;\nif (a) {\nprintln(1);\n}", formatted)
    }

    @Test
    fun `without the indent rule the source indentation is kept`() {
        val source = "let a: boolean = true;\nif (a) {\n      println(1);\n}"

        assertEquals(source, formatted(source, "{}"))
    }

    @Test
    fun `indentation does not touch a gap that stays on one line`() {
        val source = "let a: boolean = true;\nif (a) { println(1); }"

        assertEquals(source, formatted(source, """{ "indent-inside-if": 4 }"""))
    }

    @Test
    fun `a statement inside a block gets the spacing rules too`() {
        val formatted =
            formatted(
                "let a: boolean = true;\nif (a) {\nlet b:number=1+2;\n}",
                """{ "mandatory-space-surrounding-operations": true, "indent-inside-if": 2 }""",
            )

        assertEquals("let a: boolean = true;\nif (a) {\n  let b:number=1 + 2;\n}", formatted)
    }

    @Test
    fun `a forced line break after a statement is indented like the block it is in`() {
        val formatted =
            formatted(
                "let a: boolean = true;\nif (a) {\nprintln(1);println(2);\n}",
                """{ "mandatory-line-break-after-statement": true, "indent-inside-if": 2 }""",
            )

        assertEquals("let a: boolean = true;\nif (a) {\n  println(1);\n  println(2);\n}", formatted)
    }

    @Test
    fun `the 1 point 1 keys default to off`() {
        assertEquals(false, Config.read("{}".byteInputStream()).braceSameLineAsIf)
        assertEquals(false, Config.read("{}".byteInputStream()).braceBelowIfLine)
        assertEquals(null, Config.read("{}".byteInputStream()).indentInsideBlock)
    }

    @Test
    fun `an indent width the formatter will not use is refused`() {
        val error =
            kotlin.test.assertFailsWith<ConfigError> {
                Config.read("""{ "indent-inside-if": 99 }""".byteInputStream())
            }

        kotlin.test.assertContains(error.message.orEmpty(), "indent-inside-if")
    }
}
