package klox

class Environment(
   val enclosing: Environment?
) {
    private val values = hashMapOf<String, Any?>()

    fun get(name: Token): Any? {
       return if(values.containsKey(name.lexeme)){
          values.get(name.lexeme)
       }else {
           enclosing?.get(name) ?: throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
       }
    }

    fun assign(name:Token, value:Any?){
        return if(values.containsKey(name.lexeme)){
            values.put(name.lexeme, value)
            return
        }else {
            enclosing?.assign(name, value) ?: throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
    }

    fun define(name:String, value:Any?){
        values.put(name, value)
    }

    fun ancestor(distance: Int): Environment? {
       var env: Environment? = this
       for(i in 0 until  distance) {
           env = env?.enclosing
       }
       return env
    }

    fun getAt(distance:Int, name: String): Any?{
       return ancestor(distance)?.values?.get(name)
    }

    fun assignAt(distance: Int, name:Token, value: Any?) {
        ancestor(distance)?.values?.put(name.lexeme, value)
    }

    override  fun toString(): String {
       var result = values.toString()
       if(enclosing != null) {
           result += " ->  ${enclosing.toString()}"
       }
       return result
    }

}