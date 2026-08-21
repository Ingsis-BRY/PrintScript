package com.printscript.interpreter

import com.printscript.ast.Type

sealed interface Value {
    val type: Type

    data class NumberValue(val value: Double) : Value {
        override val type: Type = Type.NumberType
    }

    data class StringValue(val value: String) : Value {
        override val type: Type = Type.StringType
    }
}
