package com.printscript.interpreter

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Type
import com.printscript.common.Diagnostic
import com.printscript.common.Failure
import com.printscript.common.Position
import com.printscript.common.Result
import com.printscript.common.Success
import com.printscript.common.flatMap
import com.printscript.language.NumberCodec
import com.printscript.language.OperatorRules

/**
* applies a binary operator to two values.
* the resulting type is decided by [OperatorRules], so the rule of `+` mixing
* string and number lives in a single place and is not rewritten here.
*/
class ValueOps {

    fun apply(
        operator: BinaryOperator,
        left: Value,
        right: Value,
        position: Position
    ): Result<Value> =
        OperatorRules.resultType(operator, left.type, right.type).flatMap { resultType ->
            when (resultType) {
                Type.StringType -> Success(Value.StringValue(render(left) + render(right)))
                Type.NumberType -> arithmetic(operator, left, right, position)
            }
        }

    // a number result is only produced for number + number, so both are numbers here
    private fun arithmetic(
        operator: BinaryOperator,
        left: Value,
        right: Value,
        position: Position
    ): Result<Value> {
        val a = (left as Value.NumberValue).value
        val b = (right as Value.NumberValue).value

        return when (operator) {
            BinaryOperator.Addition -> Success(Value.NumberValue(a + b))
            BinaryOperator.Subtraction -> Success(Value.NumberValue(a - b))
            BinaryOperator.Multiplication -> Success(Value.NumberValue(a * b))
            BinaryOperator.Division ->
                if (b == 0.0) {
                    Failure(Diagnostic("Division by zero.", position))
                } else {
                    Success(Value.NumberValue(a / b))
                }
        }
    }
}

/**
* renders a value to its printable text, using [NumberCodec] for numbers so
* `3.0` prints as `3`. shared by [ValueOps] concatenation and `println`.
*/
internal fun render(value: Value): String =
    when (value) {
        is Value.NumberValue -> NumberCodec.render(value.value)
        is Value.StringValue -> value.value
    }
