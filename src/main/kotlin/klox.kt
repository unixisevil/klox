package klox

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.Charset
import kotlin.system.exitProcess


private  var hadError = false
private  var hadRuntimeError = false
private  val interpreter = Interpreter()

fun error(line: Int, message: String) {
      report(line, "", message)
}

fun error(token: Token, message: String) {
   if (token.type == TokenType.EOF) {
      report(token.line, " at end", message)
   }else {
      report(token.line, " at '${token.lexeme}'", message)
   }
}

fun runtimeError(error: RuntimeError) {
    System.err.println("${error.message}\n[line ${error.token.line}]")
    hadRuntimeError = true
}

private fun report(line: Int, where: String, message: String){
    System.err.println("[line $line] Error$where: $message")
    hadError = true
}

fun runFile(path: String){
    run(Files.readAllBytes(Paths.get(path)).toString(Charset.defaultCharset()))
    if (hadError) {
        exitProcess(65)
    }
    if (hadRuntimeError){
        exitProcess(70)
    }
}

fun run(source: String) {
       val lexer = Scanner(source)
       val tokens  =  lexer.scanTokens()

       val parser = Parser(tokens)
       val stmts =  parser.parse()

       if(hadError){
           return
       }

       val resolver = Resolver(interpreter)
       resolver.resolve(stmts)

       if(hadError){
          return
       }

       interpreter.interpret(stmts.map{ it!! })
}

fun runPrompt(){
    val  reader =  BufferedReader(InputStreamReader(System.`in`))
    while (true) {
        print("> ")
        val line  : String = reader.readLine() ?: break
        run(line)
        hadError = false
    }
}

fun main(args: Array<String>){
    when  {
        args.size > 1 -> {
            println("Usage: klox [script]")
            exitProcess(64)
        }
        args.size == 1 ->   runFile(args[0])
        else   ->           runPrompt()
    }
}