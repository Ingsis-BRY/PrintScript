package com.printscript.lexer.recognizer

import com.printscript.token.Token.AssignToken
import com.printscript.token.Token.BooleanLiteralToken
import com.printscript.token.Token.ColonToken
import com.printscript.token.Token.ConstToken
import com.printscript.token.Token.ElseToken
import com.printscript.token.Token.IfToken
import com.printscript.token.Token.LeftBraceToken
import com.printscript.token.Token.LeftParenToken
import com.printscript.token.Token.LetToken
import com.printscript.token.Token.MinusToken
import com.printscript.token.Token.PlusToken
import com.printscript.token.Token.RightBraceToken
import com.printscript.token.Token.RightParenToken
import com.printscript.token.Token.SemicolonToken
import com.printscript.token.Token.SlashToken
import com.printscript.token.Token.StarToken
import com.printscript.token.Token.TypeNameToken

/**
 * The recognizers the lexer runs, in priority order.
 *
 * Maximal munch decides first: the longest accepted lexeme wins. Only when two
 * recognizers accept the same length does this order break the tie, and the
 * earlier one wins.
 *
 * That is why the reserved words come before [IdentifierRecognizer]: both
 * accept `let`, so priority picks the keyword, while `lets` goes to the
 * identifier because it is longer.
 *
 * This list is the single place that knows the token set. Adding a token type
 * is one entry here plus its recognizer; nothing existing changes.
 */
object TokenRecognizers {
    // fixed one-character literals, none a prefix of another
    private val SYMBOLS: List<TokenRecognizer> =
        listOf(
            FixedLexemeRecognizer(":", ::ColonToken),
            FixedLexemeRecognizer("=", ::AssignToken),
            FixedLexemeRecognizer(";", ::SemicolonToken),
            FixedLexemeRecognizer("+", ::PlusToken),
            FixedLexemeRecognizer("-", ::MinusToken),
            FixedLexemeRecognizer("*", ::StarToken),
            FixedLexemeRecognizer("/", ::SlashToken),
            FixedLexemeRecognizer("(", ::LeftParenToken),
            FixedLexemeRecognizer(")", ::RightParenToken),
        )

    private val BRACES: List<TokenRecognizer> =
        listOf(
            FixedLexemeRecognizer("{", ::LeftBraceToken),
            FixedLexemeRecognizer("}", ::RightBraceToken),
        )

    // reserved words, ahead of the identifier so they win an equal-length tie
    private val WORDS: List<TokenRecognizer> =
        listOf(
            FixedLexemeRecognizer("let", ::LetToken),
            FixedLexemeRecognizer("number", ::TypeNameToken),
            FixedLexemeRecognizer("string", ::TypeNameToken),
        )

    private val WORDS_ADDED_IN_1_1: List<TokenRecognizer> =
        listOf(
            FixedLexemeRecognizer("const", ::ConstToken),
            FixedLexemeRecognizer("if", ::IfToken),
            FixedLexemeRecognizer("else", ::ElseToken),
            FixedLexemeRecognizer("boolean", ::TypeNameToken),
            FixedLexemeRecognizer("true") { lexeme, start, end ->
                BooleanLiteralToken(lexeme, true, start, end)
            },
            FixedLexemeRecognizer("false") { lexeme, start, end ->
                BooleanLiteralToken(lexeme, false, start, end)
            },
        )

    // open recognizers, the only ones whose automaton has a real loop
    private val OPEN: List<TokenRecognizer> =
        listOf(
            IdentifierRecognizer,
            NumberLiteralRecognizer,
            StringLiteralRecognizer,
        )

    val V1_0: List<TokenRecognizer> = SYMBOLS + WORDS + OPEN

    val V1_1: List<TokenRecognizer> = SYMBOLS + BRACES + WORDS + WORDS_ADDED_IN_1_1 + OPEN
}
