package klox
import  klox.TokenType.*

class Scanner(private val  source: String ) {
    private val tokens = arrayListOf<Token>()
    private var start = 0
    private var current = 0
    private var line = 1

    companion object {
        val keywords = hashMapOf(
            "and" to AND,
            "class" to CLASS,
            "else" to ELSE,
            "false" to FALSE,
            "for" to FOR,
            "fun" to FUN,
            "if" to IF,
            "nil" to NIL,
            "or" to OR,
            "print" to PRINT,
            "return" to RETURN,
            "super" to SUPER,
            "this" to THIS,
            "true" to TRUE,
            "var" to VAR,
            "while" to WHILE,
        )
    }

    fun  scanTokens(): List<Token> {
        while(!isAtEnd()) {
            start = current
            scanToken()
        }
        tokens.add(Token(EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        when (val c = advance()) {
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ',' -> addToken(COMMA)
            '.' -> addToken(DOT)
            '-' -> addToken(MINUS)
            '+' -> addToken(PLUS)
            '*' -> addToken(STAR)
            ';' -> addToken(SEMICOLON)
            //two char token
            '!' -> addToken(if (match('=')) BANG_EQUAL else BANG)
            '=' -> addToken(if (match('=')) EQUAL_EQUAL else EQUAL)
            '<' -> addToken(if (match('=')) LESS_EQUAL else LESS)
            '>' -> addToken(if (match('=')) GREATER_EQUAL else GREATER)
            '/' ->
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd())
                        advance()
                } else {
                    addToken(SLASH)
                }
            ' ', '\r', '\t' -> {}
            '\n'  ->  line++
            '"'  -> string()
            else  ->  {
               if(isDigit(c)){
                   number()
               }else if(isAlpha(c)){
                   identifier()
               }else{
                   error(line, "Unexpected character.")
               }
            }
        }
    }

    private fun identifier() {
        while (isAlphaNumber(peek()))
            advance()
        val text = source.substring(start, current)
        var type = keywords[text]
        if (type == null)
            type = IDENTIFIER
        addToken(type)
    }


        private fun number() {
            while (isDigit(peek()))
                advance()
            //look for a fractional part
            if (peek() == '.' && isDigit(peekNext())) {
                //consume '.'
                advance()
                while (isDigit(peek()))
                    advance()
            }
            addToken(NUMBER, source.substring(start, current).toDouble())
        }

        private fun string() {
            while (peek() != '"' && !isAtEnd()) {
                if (peek() == '\n') line++
                advance()
            }
            if (isAtEnd()) {
                error(line, "Unterminated string.")
                return
            }
            //The closing '"'
            advance()
            //Trim the surrounding quotes
            val value = source.substring(start + 1, current - 1)
                .replace("\\n", "\n")
                .replace("\\u001b",  "\u001b")

            addToken(STRING, value)
        }

        private fun match(expected: Char): Boolean {
            return if (isAtEnd()) {
                false
            } else if (source[current] != expected) {
                false
            } else {
                current++
                true
            }
        }

        private fun peekNext(): Char {
            return if (current + 1 >= source.length) {
                '\u0000'
            } else {
                source[current + 1]
            }
        }

        private fun peek(): Char {
            return if (isAtEnd()) {
                '\u0000'
            } else {
                source[current]
            }
        }

        private fun isAlphaNumber(c: Char): Boolean {
            return isAlpha(c) || isDigit(c)
        }

        private fun isAlpha(c: Char): Boolean {
            return when (true) {
                ((c in 'a'..'z')) -> true
                ((c in 'A'..'Z')) -> true
                (c == '_') -> true
                else -> false
            }
        }

        private fun isDigit(c: Char): Boolean {
            return c in '0'..'9'
        }

        private fun isAtEnd(): Boolean {
            return current >= source.length
        }

        private fun advance(): Char {
            val c = source[current]
            current++
            return c
        }

        private fun addToken(type: TokenType) {
            addToken(type, null)
        }

        private fun addToken(type: TokenType, literal: Any?) {
            val text = source.substring(start, current)
            tokens.add(Token(type, text, literal, line))
        }

}
