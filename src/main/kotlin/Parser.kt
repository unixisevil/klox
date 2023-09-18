package klox

import klox.TokenType.*

class  Parser(private val tokens: List<Token>, private var current: Int = 0) {
      private class ParseError : RuntimeException()

      fun parse(): List<Stmt?>{
          val stmts = mutableListOf<Stmt?>()
          while(!isAtEnd()){
             stmts.add(declaration())
          }
          return stmts
      }

      private fun declaration(): Stmt? {
          try {
              if(match(CLASS))  {
                 return classDeclaration()
              }
              if(match(FUN)) {
                  return function("function")
              }
              if(match(VAR)) {
                  return varDeclaration()
              }
              return statement()
          } catch (ex : ParseError) {
              synchronize()
              return null
          }
      }

      private fun classDeclaration(): Stmt {
          val name = consume(IDENTIFIER, "Expect class name.")
          var superclass: Expr.Variable? = null
          if(match(LESS)) {
              consume(IDENTIFIER, "Expect superclass name.")
              superclass = Expr.Variable(previous())
          }
          consume(LEFT_BRACE, "Expect '{' before class body.")
          val  methods = mutableListOf<Stmt.Function>()
          while(!check(RIGHT_BRACE) && !isAtEnd()){
              methods.add(function("method") as Stmt.Function)
          }
          consume(RIGHT_BRACE, "Expect '}' after class body.")
          return Stmt.Class(name, superclass, methods)
      }

      private fun varDeclaration(): Stmt {
          val name = consume(IDENTIFIER, "Expect variable name.")
          var initializer: Expr? = null
          if(match(EQUAL)){
              initializer = expression()
          }
          consume(SEMICOLON, "Expect ';' after variable declaration.")
          return Stmt.Var(name, initializer)
      }

      private fun statement(): Stmt {
         return  when(true) {
              match(FOR) ->  forStatement()
              match(IF) ->  ifStatement()
              match(PRINT) -> printStatement()
              match(RETURN) -> returnStatement()
              match(WHILE)  -> whileStatement()
              match(LEFT_BRACE) -> Stmt.Block(block())
              else -> expressionStatement()
          }
      }
      private fun expressionStatement(): Stmt {
          val expr = expression()
          consume(SEMICOLON, "Expect ';' after expression.")
          return Stmt.Expression(expr)
      }

      private fun ifStatement(): Stmt {
          consume(LEFT_PAREN, "Expect '(' after 'if'.")
          val condition  = expression()
          consume(RIGHT_PAREN, "Expect ')' after if condition .")

          val thenBranch = statement()
          var elseBranch: Stmt? = null
          if(match(ELSE)) {
             elseBranch = statement()
          }
          return Stmt.If(condition, thenBranch, elseBranch)
      }

      private fun printStatement(): Stmt {
          val value = expression()
          consume(SEMICOLON, "Expect ';' after value.")
          return Stmt.Print(value)
      }

      private fun returnStatement(): Stmt {
          val keyword = previous()
          var value: Expr? = null
          if(!check(SEMICOLON)) {
              value = expression()
          }
          consume(SEMICOLON, "Expect ';' after return value.")
          return Stmt.Return(keyword, value)
      }

      private fun whileStatement(): Stmt {
          consume(LEFT_PAREN, "Expect '(' after 'while'.")
          val condition = expression()
          consume(RIGHT_PAREN, "Expect ')' after condition.")
          val body = statement()
          return Stmt.While(condition, body)
      }

      private fun forStatement(): Stmt {
          consume(LEFT_PAREN, "Expect '(' after 'for' .")

          val  initializer: Stmt?
          when    {
              match(SEMICOLON) -> initializer = null
              match(VAR) ->  initializer = varDeclaration()
              else  ->   initializer = expressionStatement()
          }

          var condition: Expr? = null
          if(!check(SEMICOLON)) {
              condition = expression()
          }
          consume(SEMICOLON, "Expect ';' after loop condition .")

          var increment: Expr? = null
          if(!check(RIGHT_PAREN)) {
              increment = expression()
          }
          consume(RIGHT_PAREN, "Expect ')' after for clauses.")

          var body = statement()
          if(increment != null ) {
              body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
          }

          if(condition == null) {
              condition = Expr.Literal(true)
          }
          body = Stmt.While(condition, body)

          if(initializer != null) {
              body = Stmt.Block(listOf(initializer, body))
          }
          return  body
      }

      private fun function(kind: String): Stmt {
          val name = consume(IDENTIFIER, "Expect $kind name")
          consume(LEFT_PAREN, "Expect '(' after $kind  name")
          val parameters = mutableListOf<Token>()
          if(!check(RIGHT_PAREN)) {
              do {
                 if(parameters.size >= 255 ) {
                     error(peek(), "Can't have more than 255 parameters." )
                 }
                  parameters.add(consume(IDENTIFIER, "Expect parameter name."))
              }while(match(COMMA))
          }
          consume(RIGHT_PAREN, "Expect ')' after parameters.")
          consume(LEFT_BRACE, "Expect '{' before $kind body.")
          val body = block()
          return Stmt.Function(name, parameters, body)
      }

      private fun block(): List<Stmt?> {
         val statements = mutableListOf<Stmt?>()
         while(!check(RIGHT_BRACE) && !isAtEnd()){
            statements.add(declaration())
         }
          /*
         for(st in statements) {
             println("in block(), got $st")
         }
           */
          consume(RIGHT_BRACE,  "Expect '}' after block.")
          return statements
      }

      private fun expression(): Expr {
        return assignment()
      }

      private fun assignment(): Expr {
          val expr = or()
          if(match(EQUAL)) {
              val equals = previous()
              val value = assignment()
              when(expr) {
                  is Expr.Variable -> {
                      return Expr.Assign(expr.name, value)
                  }
                  is Expr.Get ->  {
                      return Expr.Set(expr.obj, expr.name, value)
                  }
                  else -> error(equals,  "Invalid assignment target.")
              }
          }
          return  expr
      }

      private fun or(): Expr {
          var expr = and()
          while (match(OR)) {
              val operator = previous()
              val right = and()
              expr = Expr.Logical(expr, operator, right)
          }
          return expr
      }

      private fun and(): Expr {
          var expr = equality()
          while (match(AND)) {
              val operator = previous()
              val right = equality()
              expr = Expr.Logical(expr,  operator, right)
          }
          return expr
      }

      private fun equality(): Expr {
          var expr = comparison()
          while (match(BANG_EQUAL, EQUAL_EQUAL)){
              val operator = previous()
              val right = comparison()
              expr = Expr.Binary(expr, operator, right)
          }
          return expr
      }

      private fun comparison(): Expr {
          var expr = term()
          while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)){
              val operator = previous()
              val right =  term()
              expr = Expr.Binary(expr, operator, right)
          }
          return expr
      }

      private fun term(): Expr {
          var expr = factor()
          while(match(MINUS, PLUS)) {
              val operator = previous()
              val right = factor()
              expr = Expr.Binary(expr, operator, right)
          }
          return expr
      }

      private fun factor() : Expr {
          var  expr = unary()
          while (match(SLASH, STAR)) {
              val operator = previous()
              val  right = unary()
              expr = Expr.Binary(expr, operator, right)
          }
          return expr
      }

      private fun unary(): Expr {
          if(match(BANG, MINUS)) {
              val operator = previous()
              val right =  unary()
              return Expr.Unary(operator, right)
          }
          return call()
      }

      private fun call(): Expr {
          var expr = primary()
          while(true)  {
             if(match(LEFT_PAREN)) {
                expr = finishCall(expr)
             }else if(match(DOT)) {
                val name = consume(IDENTIFIER, "Expect property name after '.'." )
                 expr = Expr.Get(expr, name)
             }else {
                 break
             }
          }
          return expr
      }

      private fun finishCall(callee: Expr): Expr {
           val  arguments = mutableListOf<Expr>()
           if(! check(RIGHT_PAREN)) {
               do {
                   if(arguments.size >= 255) {
                       error(peek(),  "Can't have more than 255 arguments.")
                   }
                   arguments.add(expression())
               }while(match(COMMA))
           }
           val paren = consume(RIGHT_PAREN,"Expect ')' after arguments.")
           return Expr.Call(callee, paren, arguments)
      }

      private fun primary(): Expr {
          when {
              match(FALSE) ->  return Expr.Literal(false)
              match(TRUE)  ->  return Expr.Literal(true)
              match(NIL)   ->  return Expr.Literal(null)
              match(NUMBER, STRING) ->  return Expr.Literal(previous().literal)
              match(SUPER)     ->  {
                  val keyword = previous()
                  consume(DOT, "Expect '.' after 'super'.")
                  val method = consume(IDENTIFIER, "Expect superclass method name.")
                  return Expr.Super(keyword, method)
              }
              match(THIS) ->  return Expr.This(previous())
              match(IDENTIFIER) -> return Expr.Variable(previous())
              match(LEFT_PAREN) -> {
                    val expr = expression()
                    consume(RIGHT_PAREN, "Expect ')' after expression.")
                    return Expr.Grouping(expr)
              }
              else ->  throw  error(peek(), "Expect expression.")
          }
      }

      private fun synchronize() {
          advance()
          while (!isAtEnd()) {
             if(previous().type == SEMICOLON)
                 return
              when(peek().type) {
                  CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN  -> return
                  else ->   advance()
              }
          }
      }

      private fun error(token: Token, message: String): ParseError {
            klox.error(token, message)
            return ParseError()
      }

      private fun match(vararg  types: TokenType): Boolean {
           for(type in types ) {
                if(check(type)) {
                     advance()
                     return true
                }
           }
            return false
      }

      private  fun consume(type: TokenType, message: String): Token {
          if(check(type))
              return advance()
          else
              throw error(peek(), message)
      }

      private fun advance(): Token {
           if(!isAtEnd())
                 current++
            return previous()
      }

      private fun check(type: TokenType): Boolean {
            return if(isAtEnd())
                  false
            else
                  peek().type == type
      }

      private  fun peek(): Token {
          return tokens[current]
      }

      private fun previous(): Token {
            return tokens[current - 1]
      }

      private fun isAtEnd(): Boolean {
            return peek().type == EOF
      }

}