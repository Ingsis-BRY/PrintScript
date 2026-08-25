package com.printscript.ast

sealed interface BinaryOperator {
    data object Addition : BinaryOperator
    data object Subtraction : BinaryOperator
    data object Multiplication : BinaryOperator
    data object Division : BinaryOperator
}
