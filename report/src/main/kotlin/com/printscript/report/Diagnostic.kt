package com.printscript.report

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Type
import com.printscript.common.Span

/**
 * Every way a PrintScript program can fail, as data.
 *
 * A case carries the facts of the failure — a name, a type, a lexeme — and a
 * [Span] covering the offending text from its first character to its last. It
 * never carries user-facing prose.
 *
 * Turning a case into a sentence belongs to the error renderer, the only
 * component that opens this hierarchy in full. Keeping the wording there means
 * every message is written once, in one file, and that adding a case here
 * breaks that file at compile time instead of slipping through unhandled.
 */
sealed interface Diagnostic {
    val span: Span

    // Lexical

    /**
     * a character no token can begin with
     */
    data class UnexpectedCharacter(
        val character: Char,
        override val span: Span,
    ) : Diagnostic

    /**
     * a lexeme a recognizer owns the start of, and then found broken
     */
    data class MalformedLexeme(
        val fault: LexicalFault,
        override val span: Span,
    ) : Diagnostic

    /**
     * the source itself broke while being read, so [detail] comes from the
     * outside world and is the one piece of text this hierarchy passes through
     */
    data class SourceUnreadable(
        val detail: String,
        override val span: Span,
    ) : Diagnostic

    // Syntactic

    /**
     * the grammar required [expected] here and got something else, or nothing
     */
    data class ExpectedSymbol(
        val expected: SyntaxSymbol,
        override val span: Span,
    ) : Diagnostic

    /**
     * a token that cannot appear where it does
     */
    data class UnexpectedToken(
        val lexeme: String,
        override val span: Span,
    ) : Diagnostic

    /**
     * the tokens ran out in the middle of [unit]
     */
    data class UnexpectedEndOfInput(
        val unit: SyntacticUnit,
        override val span: Span,
    ) : Diagnostic

    /**
     * a type name the language does not have
     */
    data class UnknownType(
        val name: String,
        override val span: Span,
    ) : Diagnostic

    /**
     * a numeric literal the lexer accepted but that is not a number
     */
    data class MalformedNumber(
        val text: String,
        override val span: Span,
    ) : Diagnostic

    // Semantic

    data class VariableAlreadyDeclared(
        val name: String,
        override val span: Span,
    ) : Diagnostic

    data class VariableNotDeclared(
        val name: String,
        override val span: Span,
    ) : Diagnostic

    /**
     * a variable that was declared but never given a value
     */
    data class VariableWithoutValue(
        val name: String,
        override val span: Span,
    ) : Diagnostic

    data class IncompatibleAssignment(
        val name: String,
        val declared: Type,
        val actual: Type,
        override val span: Span,
    ) : Diagnostic

    data class IncompatibleOperands(
        val operator: BinaryOperator,
        val left: Type,
        val right: Type,
        override val span: Span,
    ) : Diagnostic

    data class UnknownFunction(
        val name: String,
        override val span: Span,
    ) : Diagnostic

    data class DivisionByZero(
        override val span: Span,
    ) : Diagnostic
}

/**
 * What a token recognizer can report about a lexeme it could not finish.
 *
 * A recognizer names the problem, it does not word it: the renderer does.
 */
enum class LexicalFault {
    UNTERMINATED_STRING,
}

/**
 * The pieces of grammar the parser can demand by name.
 */
enum class SyntaxSymbol {
    LET,
    IDENTIFIER,
    COLON,
    TYPE_NAME,
    ASSIGN,
    SEMICOLON,
    LEFT_PAREN,
    RIGHT_PAREN,
    PRINTLN,
}

/**
 * What the parser was in the middle of when the input ran out.
 */
enum class SyntacticUnit {
    EXPRESSION,
    STATEMENT,
}
