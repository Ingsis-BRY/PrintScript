package com.printscript.lexer

import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.token.Token
import kotlin.test.fail

/**
 * Lexes [source] in full, keeping both tokens and errors.
 */
internal fun resultsOf(source: String): List<Result<Token>> =
    Lexer(StringSourceReader(source), TokenRecognizers.DEFAULT).tokens().toList()

/**
 * Lexes [source], failing the test if any error shows up.
 */
internal fun tokensOf(source: String): List<Token> =
    resultsOf(source).map { result ->
        when (result) {
            is Success -> result.value
            is Failure -> fail("unexpected lexical error: ${result.error}")
        }
    }

/**
 * Lexes [source] expecting exactly one token.
 */
internal fun singleTokenOf(source: String): Token =
    tokensOf(source).single()

/**
 * Reads the error of the result at [index], failing the test if it succeeded.
 */
internal fun failureAt(results: List<Result<Token>>, index: Int): Diagnostic =
    when (val result = results[index]) {
        is Failure -> result.error
        is Success -> fail("expected a failure but got ${result.value}")
    }

/**
 * Reads the token of the result at [index], failing the test if it errored.
 */
internal fun tokenAt(results: List<Result<Token>>, index: Int): Token =
    when (val result = results[index]) {
        is Success -> result.value
        is Failure -> fail("expected a token but got ${result.error}")
    }
