package com.printscript.ast

import com.printscript.common.Position

sealed interface Expression {
    val start: Position
    val end: Position

    data class NumberLiteral(
        val value: Double,
        override val start: Position,
        override val end: Position
    ) : Expression

    data class StringLiteral(
        val value: String,
        override val start: Position,
        override val end: Position
    ) : Expression

    data class VariableReference(
        val name: String,
        override val start: Position,
        override val end: Position
    ) : Expression

    data class BinaryExpression(
        val left: Expression,
        val operator: BinaryOperator,
        val right: Expression,
        override val start: Position,
        override val end: Position
    ) : Expression
}