package com.printscript.ast

import com.printscript.common.Located
import com.printscript.common.Position

sealed interface Statement : Located {
    data class VariableDeclaration(
        val name: String,
        val declaredType: Type,
        val initializer: Expression?,
        val mutable: Boolean,
        override val start: Position,
        override val end: Position,
    ) : Statement

    data class Assignment(
        val name: String,
        val value: Expression,
        override val start: Position,
        override val end: Position,
    ) : Statement

    data class CallStatement(
        val callee: String,
        val argument: Expression,
        override val start: Position,
        override val end: Position,
    ) : Statement

    data class IfStatement(
        val condition: Expression,
        val consequence: List<Statement>,
        val alternative: List<Statement>?,
        override val start: Position,
        override val end: Position,
    ) : Statement
}
