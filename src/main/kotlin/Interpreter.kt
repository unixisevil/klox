package klox
import  klox.TokenType.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Interpreter: Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    private val globals = Environment(null)
    private var environment: Environment = globals
    private val locals = hashMapOf<Expr, Int>()

    init {
        globals.define("clock", object: LoxCallable {
            override fun arity(): Int {
               return 0
            }
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Double {
                 return  System.currentTimeMillis() / 1000.0
            }
            override fun toString() = "<native fn>"
        })
        globals.define("sleep", object: LoxCallable {
            override fun arity(): Int {
                return 1
            }
            override fun call(interpreter: Interpreter, arguments: List<Any?>) {
                Thread.sleep((arguments[0] as Double).toLong())
            }
            override fun toString() = "<native fn>"
        })
        globals.define("sqrt", object: LoxCallable {
            override fun arity(): Int {
                return 1
            }
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Double {
                 return sqrt(arguments[0] as Double)
            }
            override fun toString() = "<native fn>"
        })
        globals.define("sin", object: LoxCallable {
            override fun arity(): Int {
                return 1
            }
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Double {
                return sin(arguments[0] as Double)
            }
            override fun toString() = "<native fn>"
        })
        globals.define("cos", object: LoxCallable {
            override fun arity(): Int {
                return 1
            }
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Double {
                return cos(arguments[0] as Double)
            }
            override fun toString() = "<native fn>"
        })
    }

   fun interpret(statements: List<Stmt>){
      try{
         for(stmt in statements) {
            execute(stmt)
         }
      }catch (error: RuntimeError) {
         runtimeError(error)
      }
   }

   fun resolve(expr: Expr, depth: Int){
      locals[expr] = depth
   }

    private  fun evaluate(expr: Expr): Any? {
        //println("in eval func: $expr")
        return expr.accept(this)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
       executeBlock(stmt.statements.map{ it!! }, Environment(environment))
    }

    fun executeBlock(statements: List<Stmt>, environment: Environment) {
        //println("in executeBlock: env = $environment")
        val prev = this.environment
        try{
           this.environment = environment
           for(stmt in statements) {
              execute(stmt)
           }
        }finally {
          this.environment = prev
        }
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
       var superclass: Any? = null
       if(stmt.superclass != null){
           superclass = evaluate(stmt.superclass)
           if((superclass as? LoxClass) == null){
              throw RuntimeError(stmt.superclass.name, "Superclass must be a class.")
           }else {
               //println("superclass: ${superclass::class.simpleName}")
           }
       }

       environment.define(stmt.name.lexeme, null)
       if(superclass != null){
          environment = Environment(environment)
          environment.define("super", superclass)
       }

       val methods = hashMapOf<String, LoxFunction>()
       for(method in  stmt.methods){
          val function =  LoxFunction(method, environment, method.name.lexeme == "init")
          methods[method.name.lexeme] = function
       }

       val klass = LoxClass(stmt.name.lexeme, superclass as LoxClass? , methods)
       if(superclass != null) {
          environment = environment.enclosing !!
       }
       environment.assign(stmt.name, klass)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
       evaluate(stmt.expression)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
       //println("in visit func: ${stmt.name}")
       val function = LoxFunction(stmt, environment, false )
       environment.define(stmt.name.lexeme, function)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
       if(isTruthy(evaluate(stmt.condition))){
           execute(stmt.thenBranch)
       }else if(stmt.elseBranch != null){
           execute(stmt.elseBranch)
       }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        var value:Any? = null
        if(stmt.value != null) {
            value = evaluate(stmt.value)
        }
        throw Return(value)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
       var value: Any? = null
       if(stmt.initializer != null){
          value = evaluate(stmt.initializer)
       }
       //println("in visitVarStmt(), before env: $environment")
       environment.define(stmt.name.lexeme, value)
       //println("in visitVarStmt(), after env: $environment")
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
       while(isTruthy(evaluate(stmt.condition))){
          execute(stmt.body)
       }
    }

    private fun execute(stmt: Stmt){
       stmt.accept(this)
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
       val value = evaluate(expr.value)
       val distance = locals[expr]
       if(distance != null ){
          environment.assignAt(distance, expr.name, value)
       }else{
          globals.assign(expr.name, value)
       }
       return value
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any {
       val left   = evaluate(expr.left)
       val right  = evaluate(expr.right)

       val ret: Any =
       when(expr.operator.type){
           BANG_EQUAL ->  !isEqual(left, right)
           EQUAL_EQUAL -> isEqual(left, right)
           GREATER ->  {
               checkNumberOperands(expr.operator, left, right)
               (left as Double) > (right as Double)
           }
           GREATER_EQUAL -> {
               checkNumberOperands(expr.operator, left, right)
               (left as Double) >= (right as Double)
           }
           LESS  -> {
               checkNumberOperands(expr.operator, left, right)
               (left as Double) < (right as Double)
           }
           LESS_EQUAL ->  {
               checkNumberOperands(expr.operator, left, right)
               (left as Double) <= (right as Double)
           }
           MINUS -> {
               checkNumberOperands(expr.operator, left, right)
               (left as Double) - (right as Double)
           }
           PLUS ->  {
               //println("left type = ${left::class.simpleName}, right type = ${right::class.simpleName}")
               if(left is Double && right is Double) {
                  left + right
               }else if(left is String || right is String) {
                  stringify(left) + stringify(right)
               }else {
                   throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.")
               }
           }
           SLASH -> {
               checkNumberOperands(expr.operator, left, right)
               (left as Double) / (right as Double)
           }
           STAR ->  {
               checkNumberOperands(expr.operator, left, right)
               (left as Double) * (right as Double)
           }
           else ->  {}
       }
       return ret
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)
        val arguments = mutableListOf<Any?>()
        for(arg in expr.arguments){
           arguments.add(evaluate(arg))
        }
        /*
        println("callee:  ${callee.toString()}")
        for (arg  in arguments) {
            println("arg is $arg")
        }
         */
        if(callee !is LoxCallable) {
            throw RuntimeError(expr.paren, "Can only call functions and classes.")
        }
        if(arguments.size != callee.arity()){
           throw RuntimeError(expr.paren, "Expected ${callee.arity()} arguments but got ${arguments.size}.")
        }
        return callee.call(this, arguments)
    }

    override fun visitGetExpr(expr: Expr.Get): Any? {
        val obj = evaluate(expr.obj)
        if(obj is LoxInstance){
           return obj.get(expr.name)
        }
        throw  RuntimeError(expr.name, "Only instances have properties.")
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }
    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)
        if(expr.operator.type == TokenType.OR){
           if(isTruthy(left)) return left
        }else {
           if(!isTruthy(left)) return left
        }
        return evaluate(expr.right)
    }

    override fun visitSetExpr(expr: Expr.Set): Any? {
       val obj = evaluate(expr.obj)
       if(obj !is LoxInstance){
          throw  RuntimeError(expr.name, "Only instances have fields.")
       }
       val value = evaluate(expr.value)
        obj.set(expr.name, value)
        return value
    }
    override fun visitSuperExpr(expr: Expr.Super): Any? {
       val distance = locals[expr] as Int
       val superclass =  environment.getAt(distance, "super") as LoxClass
       val obj =  environment.getAt(distance-1 , "this") as LoxInstance
       val method = superclass.findMethod(expr.method.lexeme)

       return method?.bind(obj) ?: throw  RuntimeError(expr.method, "Undefined property '${expr.method.lexeme}'.")
    }

    override fun visitThisExpr(expr: Expr.This): Any? {
        return lookUpVariable(expr.keyword, expr)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)
        return  when (expr.operator.type) {
             MINUS ->   {
                 checkNumberOperand(expr.operator, right)
                 (right as? Double)?.unaryMinus()
             }
             BANG  ->   !isTruthy(right)
             else ->   null
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
         val r = lookUpVariable(expr.name, expr)
         // println("visit var expr, name = ${expr.name}, value = $r")
        return r
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
            val distance = locals[expr]
            //println("distance = $distance")
            return if(distance != null) {
                environment.getAt(distance, name.lexeme)
            }else {
                globals.get(name)
            }
    }

    private fun checkNumberOperand(operator: Token, operand: Any?)  {
       if(operand is Double) return
       throw  RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?)  {
        if(left is Double &&  right is Double) return
        throw  RuntimeError(operator, "Operands must be numbers.")
    }

    private  fun isTruthy(obj: Any?): Boolean {
        //println("in isTruthy: $obj")
        if(obj == null)  return false
         return when(obj) {
             is Boolean -> obj
             else  ->  true
         }
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        if(a == null && b == null)  return true
        if(a == null )  return false
        return a == b
    }

    private fun stringify(obj: Any?): String {
        if(obj == null)  return "nil"
        if(obj is Double){
            var text =  obj.toString()
            if(text.endsWith(".0")){
                text = text.substring(0, text.length -2)
            }
            return text
        }
        return obj.toString()
    }

}