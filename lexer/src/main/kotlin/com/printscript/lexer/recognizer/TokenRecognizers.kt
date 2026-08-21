package com.printscript.lexer.recognizer

import com.printscript.token.Token.AssignToken
import com.printscript.token.Token.ColonToken
import com.printscript.token.Token.LeftParenToken
import com.printscript.token.Token.LetToken
import com.printscript.token.Token.MinusToken
import com.printscript.token.Token.PlusToken
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

    val DEFAULT: List<TokenRecognizer> = listOf(
        // fixed one-character literals, none a prefix of another
        FixedLexemeRecognizer(":", ::ColonToken),
        FixedLexemeRecognizer("=", ::AssignToken),
        FixedLexemeRecognizer(";", ::SemicolonToken),
        FixedLexemeRecognizer("+", ::PlusToken),
        FixedLexemeRecognizer("-", ::MinusToken),
        FixedLexemeRecognizer("*", ::StarToken),
        FixedLexemeRecognizer("/", ::SlashToken),
        FixedLexemeRecognizer("(", ::LeftParenToken),
        FixedLexemeRecognizer(")", ::RightParenToken),

        // reserved words, ahead of the identifier so they win an equal-length tie
        FixedLexemeRecognizer("let", ::LetToken),
        FixedLexemeRecognizer("number", ::TypeNameToken),
        FixedLexemeRecognizer("string", ::TypeNameToken),

        // open recognizers, the only ones whose automaton has a real loop
        IdentifierRecognizer,
        NumberLiteralRecognizer,
        StringLiteralRecognizer
    )
}
