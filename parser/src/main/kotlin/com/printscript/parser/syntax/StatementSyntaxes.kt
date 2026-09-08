package com.printscript.parser.syntax

import com.printscript.report.SyntaxSymbol

object StatementSyntaxes {
    val DEFAULT: List<StatementSyntax> =
        listOf(
            VariableDeclarationSyntax,
            CallSyntax("println", SyntaxSymbol.PRINTLN),
            AssignmentSyntax,
        )
}
