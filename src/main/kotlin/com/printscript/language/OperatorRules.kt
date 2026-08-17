package com.printscript.language

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Type
import com.printscript.common.Diagnostic
import com.printscript.common.Failure
import com.printscript.common.Position
import com.printscript.common.Result
import com.printscript.common.Success

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

    fun resultType(
        operator: BinaryOperator,
        left: Type,
        right: Type
    ): Result<Type> {
        val resultType = rules[RuleKey(operator, left, right)]

        return if (resultType != null) {
            Success(resultType)
        } else {
            Failure(
                Diagnostic(
                    message = "Incompatible types for operator $operator: $left and $right",
                    position = Position(0, 0)
                )
            )
        }
    }
}