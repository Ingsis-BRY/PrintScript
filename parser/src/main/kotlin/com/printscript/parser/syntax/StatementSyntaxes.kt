package com.printscript.parser.syntax

object StatementSyntaxes {
    val DEFAULT: List<StatementSyntax> =
        listOf(
            VariableDeclarationSyntax,
            CallSyntax("println"),
            AssignmentSyntax,
        )
}
