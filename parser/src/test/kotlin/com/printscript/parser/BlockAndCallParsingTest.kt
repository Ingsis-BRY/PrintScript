package com.printscript.parser

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.lexer.Lexer
import com.printscript.lexer.StringSourceReader
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.parser.expression.PrefixParselets
import com.printscript.parser.syntax.StatementSyntaxes
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.SyntaxSymbol
import com.printscript.token.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class BlockAndCallParsingTest {
    private fun tokensOf(
        source: String,
        version11: Boolean,
    ): List<Token> {
        val recognizers = if (version11) TokenRecognizers.V1_1 else TokenRecognizers.V1_0

        return Lexer(StringSourceReader(source), recognizers)
            .tokens()
            .map { assertIs<Success<Token>>(it).value }
            .toList()
    }

    private fun parse11(source: String): Result<Statement> =
        Parser(StatementSyntaxes.V1_1, PrefixParselets.V1_1).parse(tokensOf(source, true))

    private fun parse10(source: String): Result<Statement> =
        Parser(StatementSyntaxes.V1_0, PrefixParselets.V1_0).parse(tokensOf(source, false))

    private fun statement11(source: String): Statement =
        assertIs<Success<Statement>>(parse11(source)).value

    private fun error11(source: String): Diagnostic = assertIs<Failure>(parse11(source)).error

    @Test
    fun `a const declaration is a declaration that is not mutable`() {
        val declaration =
            assertIs<Statement.VariableDeclaration>(statement11("""const a: string = "x";"""))

        assertEquals("a", declaration.name)
        assertEquals(Type.StringType, declaration.declaredType)
        assertEquals(false, declaration.mutable)
    }

    @Test
    fun `a let declaration is mutable`() {
        val declaration =
            assertIs<Statement.VariableDeclaration>(statement11("""let a: string = "x";"""))

        assertEquals(true, declaration.mutable)
    }

    @Test
    fun `a const with no initializer still parses, and the interpreter is what refuses it later`() {
        val declaration = assertIs<Statement.VariableDeclaration>(statement11("const a: number;"))

        assertNull(declaration.initializer)
    }

    @Test
    fun `boolean is a type`() {
        val declaration = assertIs<Statement.VariableDeclaration>(statement11("let a: boolean;"))

        assertEquals(Type.BooleanType, declaration.declaredType)
    }

    @Test
    fun `a boolean literal is an expression`() {
        val declaration =
            assertIs<Statement.VariableDeclaration>(statement11("let a: boolean = false;"))

        assertEquals(false, assertIs<Expression.BooleanLiteral>(declaration.initializer).value)
    }

    @Test
    fun `an if holds its condition and its block`() {
        val conditional = assertIs<Statement.IfStatement>(statement11("if (a) { println(1); }"))

        assertEquals("a", assertIs<Expression.VariableReference>(conditional.condition).name)
        assertEquals(1, conditional.consequence.size)
        assertNull(conditional.alternative)
    }

    @Test
    fun `an else block is kept apart from the if block`() {
        val conditional =
            assertIs<Statement.IfStatement>(
                statement11("if (a) { println(1); } else { println(2); println(3); }"),
            )

        assertEquals(1, conditional.consequence.size)
        assertEquals(2, conditional.alternative?.size)
    }

    @Test
    fun `an empty block is a block with no statements`() {
        val conditional = assertIs<Statement.IfStatement>(statement11("if (a) { }"))

        assertEquals(emptyList(), conditional.consequence)
    }

    @Test
    fun `a block holds another if`() {
        val conditional =
            assertIs<Statement.IfStatement>(statement11("if (a) { if (b) { println(1); } }"))

        assertIs<Statement.IfStatement>(conditional.consequence.single())
    }

    @Test
    fun `an if spans from its keyword to its last brace`() {
        val conditional = assertIs<Statement.IfStatement>(statement11("if (a) { println(1); }"))

        assertEquals(1, conditional.start.column)
        assertEquals(22, conditional.end.column)
    }

    @Test
    fun `else if is refused, asking for the brace it does not have`() {
        val error = assertIs<Diagnostic.ExpectedSymbol>(error11("if (a) { } else if (b) { }"))

        assertEquals(SyntaxSymbol.LEFT_BRACE, error.expected)
    }

    @Test
    fun `an if with no block is refused`() {
        assertIs<Diagnostic.ExpectedSymbol>(error11("if (a) println(1);"))
    }

    @Test
    fun `an if whose block never closes is refused`() {
        assertIs<Diagnostic>(error11("if (a) { println(1);"))
    }

    @Test
    fun `an if with no condition is refused`() {
        assertIs<Diagnostic.ExpectedSymbol>(error11("if { }"))
    }

    @Test
    fun `readInput is a call that yields a value`() {
        val declaration =
            assertIs<Statement.VariableDeclaration>(
                statement11("""let a: string = readInput("Name:");"""),
            )

        val call = assertIs<Expression.FunctionCall>(declaration.initializer)

        assertEquals("readInput", call.callee)
        assertEquals("Name:", assertIs<Expression.StringLiteral>(call.argument).value)
    }

    @Test
    fun `readEnv is a call that yields a value`() {
        val declaration =
            assertIs<Statement.VariableDeclaration>(
                statement11("""let a: string = readEnv("HOME");"""),
            )

        assertEquals("readEnv", assertIs<Expression.FunctionCall>(declaration.initializer).callee)
    }

    @Test
    fun `a call nests inside an expression`() {
        val declaration =
            assertIs<Statement.VariableDeclaration>(
                statement11("""let a: string = "x" + readInput("y");"""),
            )

        val sum = assertIs<Expression.BinaryExpression>(declaration.initializer)

        assertIs<Expression.FunctionCall>(sum.right)
    }

    @Test
    fun `a call nests inside a println`() {
        val call = assertIs<Statement.CallStatement>(statement11("""println(readInput("y"));"""))

        assertIs<Expression.FunctionCall>(call.argument)
    }

    @Test
    fun `a name that is not callable stays a name`() {
        val declaration =
            assertIs<Statement.VariableDeclaration>(statement11("let a: string = other;"))

        assertIs<Expression.VariableReference>(declaration.initializer)
    }

    @Test
    fun `1 point 0 has no const`() {
        assertIs<Failure>(parse10("""const a: string = "x";"""))
    }

    @Test
    fun `1 point 0 has no boolean type`() {
        val error =
            assertIs<Diagnostic.ExpectedSymbol>(assertIs<Failure>(parse10("let a: boolean;")).error)

        assertEquals(SyntaxSymbol.TYPE_NAME, error.expected)
    }

    @Test
    fun `1 point 0 reads readInput as a name and then trips on the parenthesis`() {
        assertIs<Failure>(parse10("""let a: string = readInput("Name:");"""))
    }

    @Test
    fun `1 point 1 parses everything 1 point 0 does`() {
        assertEquals(
            parse10("let x: number = 5 + 4 * 3;"),
            parse11("let x: number = 5 + 4 * 3;"),
        )
    }

    @Test
    fun `a condition that is not a variable is refused`() {
        assertIs<Diagnostic.NonVariableCondition>(error11("if (true) { }"))
        assertIs<Diagnostic.NonVariableCondition>(error11("if (a + b) { }"))
        assertIs<Diagnostic.NonVariableCondition>(error11("""if (readInput("q")) { }"""))
        assertIs<Diagnostic.NonVariableCondition>(error11("if (1) { }"))
    }

    @Test
    fun `the refused condition is blamed over its own span`() {
        val error = assertIs<Diagnostic.NonVariableCondition>(error11("if (true) { }"))

        assertEquals(5, error.span.start.column)
        assertEquals(8, error.span.end.column)
    }

    @Test
    fun `a variable is the only condition the grammar takes`() {
        val conditional = assertIs<Statement.IfStatement>(statement11("if (flag) { }"))

        assertEquals("flag", assertIs<Expression.VariableReference>(conditional.condition).name)
    }
}
