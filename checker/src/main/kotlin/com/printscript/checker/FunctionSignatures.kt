package com.printscript.checker

import com.printscript.ast.Type

data class Signature(
    val argument: Type,
)

object FunctionSignatures {
    val NONE: Map<String, Signature> = emptyMap()

    val V1_1: Map<String, Signature> =
        mapOf(
            "readInput" to Signature(Type.StringType),
            "readEnv" to Signature(Type.StringType),
        )
}
