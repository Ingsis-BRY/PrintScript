package com.printscript.lexer.recognizer

/**
 * Where a [TokenRecognizer] ends up after walking a lexeme through its automaton.
 *
 * [Accepted] and [Pending] both mean the automaton is still alive, so the lexer
 * keeps feeding it characters; only [Rejected] takes a recognizer out of the run.
 */
sealed interface RecognizerState {
    /**
     * the lexeme is a complete token, though a longer one may still match
     */
    data object Accepted : RecognizerState

    /**
     * the lexeme is not a token yet, but a longer one could be
     */
    data object Pending : RecognizerState

    /**
     * the lexeme can never become a token, no matter what follows
     */
    data object Rejected : RecognizerState
}
