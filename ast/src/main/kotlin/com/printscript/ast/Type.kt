package com.printscript.ast

sealed interface Type {
    data object NumberType : Type

    data object StringType : Type

    data object BooleanType : Type
}
