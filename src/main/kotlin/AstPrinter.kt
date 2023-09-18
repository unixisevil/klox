package klox

class AstPrinter : Expr.Visitor<String> {
    fun  print(expr: Expr) : String {
        return expr.accept(this)
    }

    override  fun visitAssignExpr(expr: Expr.Assign): String {
        return parenthesize2("=", expr.name.lexeme, expr.value)
    }

    override  fun visitBinaryExpr(expr: Expr.Binary): String {
        return parenthesize(expr.operator.lexeme,  expr.left, expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call): String {
        return parenthesize2("call", expr.callee, expr.arguments)
    }

    override  fun visitGetExpr(expr: Expr.Get): String {
        return parenthesize2(".", expr.obj, expr.name.lexeme)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        return parenthesize("group", expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String {
        return if(expr.value == null)  "nil" else expr.value.toString()
    }

    override fun visitLogicalExpr(expr: Expr.Logical): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitSetExpr(expr: Expr.Set): String {
        return parenthesize2("=", expr.obj, expr.name.lexeme, expr.value)
    }

    override fun visitSuperExpr(expr: Expr.Super): String {
        return parenthesize2("super", expr.method)
    }

    override fun visitThisExpr(expr: Expr.This): String {
        return "this"
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable): String {
        return expr.name.lexeme
    }

    private  fun parenthesize(name: String, vararg exprs: Expr): String {
        val buf = StringBuilder()
        buf.append("(").append(name)
        for(expr in exprs){
            buf.append(" ")
            buf.append(expr.accept(this))
        }
        buf.append(")")
        return buf.toString()
    }

    private fun parenthesize2(name: String, vararg parts: Any): String {
        val buf = StringBuilder()
        buf.append("(").append(name)
        transform(buf, parts)
        buf.append(")")
        return buf.toString()
    }

    private fun transform(buf: StringBuilder, vararg parts: Any) {
        for(part in parts){
            buf.append(" ")
            when(part){
                is Expr -> buf.append(part.accept(this))
                is Token -> buf.append(part.lexeme)
                is List<*>  -> transform(buf, part.toTypedArray())
                else ->   buf.append(part)
            }
        }
    }
}