package com.printscript.formatter

import com.printscript.lexer.Lexer
import com.printscript.lexer.StringSourceReader
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.report.Failure
import com.printscript.report.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FormatterTest {
    private fun formatted(
        source: String,
        config: Config = Config.PRESERVING,
    ): String {
        val out = StringBuilder()
        val lexer = Lexer(StringSourceReader(source), TokenRecognizers.DEFAULT)

        assertIs<Success<Unit>>(Formatter(config).format(lexer.tokens(), out))

        return out.toString()
    }

    @Test
    fun `a source with no rules keeps the spacing it came with`() {
        val source = "let something:string = \"a really cool thing\";"

        assertEquals(source, formatted(source))
    }

    @Test
    fun `odd spacing is preserved rather than tidied up`() {
        val source = "let something:      string=\"a thing\";"

        assertEquals(source, formatted(source))
    }

    @Test
    fun `blank lines between statements are preserved`() {
        val source = "println(1);\n\n\nprintln(2);"

        assertEquals(source, formatted(source))
    }

    @Test
    fun `indentation is preserved`() {
        val source = "println(1);\n    println(2);"

        assertEquals(source, formatted(source))
    }

    @Test
    fun `whitespace before the first token is preserved`() {
        val source = "\n\n   println(1);"

        assertEquals(source, formatted(source))
    }

    @Test
    fun `several statements on one line stay on one line`() {
        val source = "println(1);println(2);println(3);"

        assertEquals(source, formatted(source))
    }

    @Test
    fun `nothing is written after the last token`() {
        assertEquals("println(1);", formatted("println(1);\n\n\n"))
    }

    @Test
    fun `an empty source produces nothing`() {
        assertEquals("", formatted(""))
    }

    @Test
    fun `space before the colon is enforced`() {
        val config = Config.PRESERVING.copy(spaceBeforeColon = true)

        assertEquals("let a : number = 1;", formatted("let a: number = 1;", config))
        assertEquals("let a :number = 1;", formatted("let a:number = 1;", config))
    }

    @Test
    fun `space after the colon is enforced`() {
        val config = Config.PRESERVING.copy(spaceAfterColon = true)

        assertEquals("let a: number = 1;", formatted("let a:number = 1;", config))
        assertEquals("let a : number = 1;", formatted("let a : number = 1;", config))
    }

    @Test
    fun `space around the equals is enforced`() {
        val config = Config.PRESERVING.copy(spaceAroundAssignment = true)

        assertEquals("let a: number = 1;", formatted("let a: number=1;", config))
        assertEquals("let a: number = 1;", formatted("let a: number   =1;", config))
    }

    @Test
    fun `no space around the equals is enforced`() {
        val config = Config.PRESERVING.copy(noSpaceAroundAssignment = true)

        assertEquals("let a: number=1;", formatted("let a: number = 1;", config))
        assertEquals("a=1;", formatted("a = 1;", config))
    }

    @Test
    fun `a single space separates every token of a statement`() {
        val config = Config.PRESERVING.copy(singleSpaceSeparation = true)

        assertEquals("let a : number = 1;", formatted("let a:number=1;", config))
    }

    @Test
    fun `a single space is not written before the semicolon`() {
        val config = Config.PRESERVING.copy(singleSpaceSeparation = true)

        assertEquals("println ( a );", formatted("println(a);", config))
    }

    @Test
    fun `a single space does not join two statements into one line`() {
        val config = Config.PRESERVING.copy(singleSpaceSeparation = true)

        assertEquals(
            "println ( a );\nprintln ( b );",
            formatted("println(a);\nprintln(b);", config),
        )
    }

    @Test
    fun `operators are surrounded by a space`() {
        val config = Config.PRESERVING.copy(spaceAroundOperators = true)

        assertEquals(
            "let a: number = 5 + 4 * 3 / 2;",
            formatted("let a: number = 5+4*3/2;", config),
        )
        assertEquals("let a: number = 5 - 4;", formatted("let a: number = 5   -   4;", config))
    }

    @Test
    fun `a line break is enforced after every statement`() {
        val config = Config.PRESERVING.copy(lineBreakAfterStatement = true)

        assertEquals("println(1);\nprintln(2);", formatted("println(1);println(2);", config))
    }

    @Test
    fun `blank lines after println are collapsed to none`() {
        val config = Config.PRESERVING.copy(blankLinesAfterPrintln = 0)

        assertEquals(
            "println(1);\nprintln(2);",
            formatted("println(1);\n\n\n\nprintln(2);", config),
        )
    }

    @Test
    fun `one blank line is added after println`() {
        val config = Config.PRESERVING.copy(blankLinesAfterPrintln = 1)

        assertEquals("println(1);\n\nprintln(2);", formatted("println(1);\nprintln(2);", config))
    }

    @Test
    fun `two blank lines are added after println`() {
        val config = Config.PRESERVING.copy(blankLinesAfterPrintln = 2)

        assertEquals("println(1);\n\n\nprintln(2);", formatted("println(1);\nprintln(2);", config))
    }

    @Test
    fun `the colon rule does not touch the equals`() {
        val config = Config.PRESERVING.copy(spaceAfterColon = true)

        assertEquals("let a : number=1;", formatted("let a :number=1;", config))
    }

    @Test
    fun `the equals rule does not touch the colon`() {
        val config = Config.PRESERVING.copy(spaceAroundAssignment = true)

        assertEquals("let a:number = 1;", formatted("let a:number=1;", config))
    }

    @Test
    fun `the statement rule does not touch spacing inside the statement`() {
        val config = Config.PRESERVING.copy(lineBreakAfterStatement = true)

        assertEquals(
            "let a:number=1;\nlet b:number=2;",
            formatted("let a:number=1;let b:number=2;", config),
        )
    }

    @Test
    fun `blank lines are added after println and not after other statements`() {
        val config = Config.PRESERVING.copy(blankLinesAfterPrintln = 1)

        assertEquals(
            "let a: number = 1;\nprintln(a);\n\nprintln(a);",
            formatted("let a: number = 1;\nprintln(a);\nprintln(a);", config),
        )
    }

    @Test
    fun `an assignment to a name that is not println does not add blank lines`() {
        val config = Config.PRESERVING.copy(blankLinesAfterPrintln = 2)

        assertEquals("a = 1;\nb = 2;", formatted("a = 1;\nb = 2;", config))
    }

    @Test
    fun `two rules apply to their own symbols without undoing each other`() {
        val config =
            Config.PRESERVING.copy(
                spaceAfterColon = true,
                noSpaceAroundAssignment = true,
            )

        assertEquals("let a: number=1;", formatted("let a:number = 1;", config))
    }

    @Test
    fun `a lexical error stops the formatting`() {
        val lexer = Lexer(StringSourceReader("let a = @;"), TokenRecognizers.DEFAULT)

        val result = Formatter(Config.PRESERVING).format(lexer.tokens(), StringBuilder())

        assertIs<Failure>(result)
    }

    @Test
    fun `a long source is formatted one token at a time`() {
        val count = 1000
        val config = Config.PRESERVING.copy(lineBreakAfterStatement = true)

        val out = formatted("println(1);".repeat(count), config)

        assertEquals(count, out.lines().size)
    }
}
