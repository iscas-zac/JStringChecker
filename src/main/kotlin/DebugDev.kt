import java.io.BufferedReader

fun main() {
    val c = BufferedReader::class
    val methods = c.java.methods
    for (method in methods) {
        println("const val ${method.name}_sig = \"<java.lang.BufferedReader: ${method.genericReturnType.typeName} ${method.name}(${method.parameters.joinToString(",") { it.type.typeName }})>\"")
    }
}

//const val indexOf6_sig = "<java.lang.String: int indexOf(java.lang.String,int,int)>"