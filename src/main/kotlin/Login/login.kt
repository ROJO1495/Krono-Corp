package Login

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class User(
    val name: String = "",
    val email: String,
    val phoneNumber: String,
    val password: String,
    val role: String = "EMPLEADO"
)

fun main() {
    // Variable para llamar al archivo .json y corroborar si el archivo ya existe
    val archivo = File("usuario.json")
    val archivojson = Json { prettyPrint = true }
    
    val listaUsuarios = mutableListOf<User>()
    
    if (archivo.exists() && archivo.length() > 0) {
        try {
            val contenidoJson = archivo.readText()
            // CORRECCIÓN 1: Se lee como una lista de usuarios
            val usuariosGuardados = Json.decodeFromString<List<User>>(contenidoJson)
            listaUsuarios.addAll(usuariosGuardados)
        } catch (e: Exception) {
            println("Error al leer el archivo, es probable que el archivo usuario.json este corrupto")
        }
    }
    
    // Variables para generar la informacion base del usuario
    var name = ""
    var emaill = ""
    var phoneNumberr = ""
    var passwordd = ""
    var role = "EMPLEADO"

    // Variables para seleccion inicial
    var opcion: Int
    
    // Preguntas de inicio de sesion
    println("Bienvenido a KronoCorp, tu administrador empresarial de confianza.")
    println("Presiona 1 para iniciar sesion")
    println("Presiona 2 para registrarte")
    opcion = readln().toInt()
    when (opcion) {
        1 -> {
            println("Bienvenido de nuevo")
            println("Por favor, Ingresa tu correo electronico:")
            emaill = readln().trim()
            println("Por favor, Ingresa tu contraseña:")
            passwordd = readln().trim()
            val usuarioExistente = listaUsuarios.find { it.email.equals(emaill, ignoreCase = true) && it.password.equals(passwordd, ignoreCase = true) }
            if (usuarioExistente != null) {
                println("Bienvenido de nuevo, $emaill")
                Admin.showHome(usuarioExistente, listaUsuarios)
            } else {
                print("Usuario o contraseña incorrectos.")
            }
        }
        2 -> {
            println("Bienvenido")
            println("Por favor, ingresa tu correo electronico")
            emaill = readln().trim()
            val usuarioExistente = listaUsuarios.any { it.email.equals(emaill, ignoreCase = true) }
            if (usuarioExistente) {
                println("Este usuario ya existe, por favor inicia sesion nuevamente")
                return
            }
            
            println("Por favor, ingresa tu numero de telefono")
            phoneNumberr = readln().trim()
            println("Por favor, ingresa tu contraseña")
            passwordd = readln().trim()
            
            
            //Registro del usuario en el archivo .json
            val usuario = User(
                email = emaill,
                phoneNumber = phoneNumberr,
                password = passwordd
            )
            
            //Agregamos el nuevo usuario a la lista en memoria
            listaUsuarios.add(usuario)
            
            //Convertimos a JSON la lista completa actualizada
            val jsonString = archivojson.encodeToString(listaUsuarios)
            
            try {
                archivo.writeText(jsonString)
                println("\n Su registro ha sido exitoso")
                println("El archivo usuario.json se ha creado en: ${archivo.absolutePath}")
            } catch (e: Exception) {
                println("Error al escribir en el archivo: ${e.message}")
            }
            
        }
    }
}
