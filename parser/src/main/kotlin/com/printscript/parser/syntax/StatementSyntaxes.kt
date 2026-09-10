package com.printscript.parser.syntax

import com.printscript.report.SyntaxSymbol
import com.printscript.token.Token

object StatementSyntaxes {
    val V1_0: List<StatementSyntax> =
        listOf(
            letDeclaration(),
            CallSyntax("println"),
            AssignmentSyntax,
        )

    val V1_1: List<StatementSyntax> =
        listOf(
            letDeclaration(),
            constDeclaration(),
            IfSyntax,
            CallSyntax("println"),
            AssignmentSyntax,
        )

    private fun letDeclaration(): StatementSyntax =
        VariableDeclarationSyntax(SyntaxSymbol.LET, mutable = true) { it is Token.LetToken }

    private fun constDeclaration(): StatementSyntax =
        VariableDeclarationSyntax(SyntaxSymbol.CONST, mutable = false) { it is Token.ConstToken }
}
