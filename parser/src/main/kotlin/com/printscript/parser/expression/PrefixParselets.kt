package com.printscript.parser.expression

object PrefixParselets {
    val V1_0: List<PrefixParselet> =
        listOf(
            NumberLiteralParselet,
            StringLiteralParselet,
            VariableReferenceParselet,
            GroupParselet,
        )

    val V1_1: List<PrefixParselet> =
        listOf(
            NumberLiteralParselet,
            StringLiteralParselet,
            BooleanLiteralParselet,
            FunctionCallParselet("readInput"),
            FunctionCallParselet("readEnv"),
            VariableReferenceParselet,
            GroupParselet,
        )
}
