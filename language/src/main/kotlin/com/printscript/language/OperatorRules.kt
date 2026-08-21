package com.printscript.language

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Type

/**
* the table of which operand types each binary operator accepts, and what it
* yields. it answers only that question: it has no source position to blame, so
* the caller that does turn a `null` into the error the user sees.
*/
object OperatorRules {

    private data class RuleKey(
        val operator: BinaryOperator,
        val left: Type,
        val right: Type
    )

    private val rules: Map<RuleKey, Type> = mapOf(
        RuleKey(
            BinaryOperator.Addition,
            Type.NumberType,
            Type.NumberType
        ) to Type.NumberType,

        RuleKey(
            BinaryOperator.Addition,
            Type.StringType,
            Type.StringType
        ) to Type.StringType,

        RuleKey(
            BinaryOperator.Addition,
            Type.StringType,
            Type.NumberType
        ) to Type.StringType,

        RuleKey(
            BinaryOperator.Addition,
            Type.NumberType,
            Type.StringType
        ) to Type.StringType,

        RuleKey(
            BinaryOperator.Subtraction,
            Type.NumberType,
            Type.NumberType
        ) to Type.NumberType,

        RuleKey(
            BinaryOperator.Multiplication,
            Type.NumberType,
            Type.NumberType
        ) to Type.NumberType,

        RuleKey(
            BinaryOperator.Division,
            Type.NumberType,
            Type.NumberType
        ) to Type.NumberType
    )

    /**
    * the type the operator produces for these operands, or null if it rejects them
    */
    fun resultType(
        operator: BinaryOperator,
        left: Type,
        right: Type
    ): Type? = rules[RuleKey(operator, left, right)]
}