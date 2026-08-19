package com.printscript.report

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Type
import com.printscript.common.Diagnostic
import com.printscript.common.LexicalFault
import com.printscript.common.Span
import com.printscript.common.SyntacticUnit
import com.printscript.common.SyntaxSymbol

/**
 * Turns a [Diagnostic] into the line a user reads.
 *
 * Every message opens with the span the error covers, start and end, as
 * `(line:column)-(line:column)`.
 */
class ErrorRenderer {

    fun render(error: Diagnostic): String =
        "${renderSpan(error.span)} ${describe(error)}"

    /**
     * both ends are inclusive, so a one-character error repeats its position
     */
    private fun renderSpan(span: Span): String =
        "(${span.start.line}:${span.start.column})-(${span.end.line}:${span.end.column})"

    /**
     * the single place where an error case becomes prose
     */
    private fun describe(error: Diagnostic): String =
        when (error) {
            is Diagnostic.UnexpectedCharacter ->
                "Unexpected character '${error.character}'."

            is Diagnostic.MalformedLexeme ->
                describe(error.fault)

            is Diagnostic.SourceUnreadable ->
                "Could not read source: ${error.detail}."

            is Diagnostic.ExpectedSymbol ->
                "Expected ${describe(error.expected)}."

            is Diagnostic.UnexpectedToken ->
                "Unexpected token '${error.lexeme}'."

            is Diagnostic.UnexpectedEndOfInput ->
                "Unexpected end of ${describe(error.unit)}."

            is Diagnostic.UnknownType ->
                "Unknown type '${error.name}'."

            is Diagnostic.MalformedNumber ->
                "Malformed number '${error.text}'."

            is Diagnostic.VariableAlreadyDeclared ->
                "Variable '${error.name}' is already declared."

            is Diagnostic.VariableNotDeclared ->
                "Variable '${error.name}' is not declared."

            is Diagnostic.VariableWithoutValue ->
                "Variable '${error.name}' is used before it has a value."

            is Diagnostic.IncompatibleAssignment ->
                "Cannot assign a ${describe(error.actual)} value to variable " +
                    "'${error.name}' of type ${describe(error.declared)}."

            is Diagnostic.IncompatibleOperands ->
                "Cannot apply '${describe(error.operator)}' to " +
                    "${describe(error.left)} and ${describe(error.right)}."

            is Diagnostic.UnknownFunction ->
                "Unknown function '${error.name}'."

            is Diagnostic.DivisionByZero ->
                "Division by zero."
        }

    private fun describe(fault: LexicalFault): String =
        when (fault) {
            LexicalFault.UNTERMINATED_STRING -> "Unterminated string literal."
        }

    /**
     * a symbol reads as the text the user has to type, except where the
     * grammar wants a whole category rather than one spelling
     */
    private fun describe(symbol: SyntaxSymbol): String =
        when (symbol) {
            SyntaxSymbol.LET -> "'let'"
            SyntaxSymbol.IDENTIFIER -> "an identifier"
            SyntaxSymbol.COLON -> "':'"
            SyntaxSymbol.TYPE_NAME -> "a type"
            SyntaxSymbol.ASSIGN -> "'='"
            SyntaxSymbol.SEMICOLON -> "';' at the end of the statement"
            SyntaxSymbol.LEFT_PAREN -> "'('"
            SyntaxSymbol.RIGHT_PAREN -> "')'"
            SyntaxSymbol.PRINTLN -> "'println'"
        }

    private fun describe(unit: SyntacticUnit): String =
        when (unit) {
            SyntacticUnit.EXPRESSION -> "expression"
            SyntacticUnit.STATEMENT -> "statement"
        }

    private fun describe(type: Type): String =
        when (type) {
            Type.NumberType -> "number"
            Type.StringType -> "string"
        }

    private fun describe(operator: BinaryOperator): String =
        when (operator) {
            BinaryOperator.Addition -> "+"
            BinaryOperator.Subtraction -> "-"
            BinaryOperator.Multiplication -> "*"
            BinaryOperator.Division -> "/"
        }
}
