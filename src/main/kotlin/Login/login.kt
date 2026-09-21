package Login

import AsistenteIA.AsistenteIA
import Gerente.showHome

//Importaciones para archivos .json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class User(
    val alias: String,
    val email: String,
    val phoneNumber: String,
    val password: String,
    val role: String
)

// Variables para generar la informacion base del usuario
var aliass = ""
var emaill = ""
var phoneNumberr = ""
var passwordd = ""
var rolee = ""

//Registro del usuario en el archivo .json
val usuario = User(
    alias = aliass,
    email = emaill,
    phoneNumber = phoneNumberr,
    password = passwordd,
    role = rolee
)

// Variable para llamar al archivo .json y corroborar si el archivo ya existe
val archivo = File("usuario.json")
val archivojson = Json { prettyPrint = true }

val listaUsuarios = mutableListOf<User>()

fun opcionesInicio() {
    // Variables para seleccion inicial
    var opcion: Int
    println("==========================================")
    println("Presiona 1 para iniciar sesion")
    println("Presiona 2 para registrarte")
    opcion = readln().toInt()
    when (opcion) {
        1 -> {
            inicioSesion()
        }
        2 -> {
            registro()
        }
    }
}

fun inicioSesion() {
    println("==========================================")
    println("Bienvenido de nuevo")
    println("Por favor, Ingresa tu correo electronico o su nombre de usuario:")
    val credencial = readln().trim()
    val usuarioExistente = listaUsuarios.find { it.email.equals(credencial, ignoreCase = true) || it.alias.equals(credencial, ignoreCase = true)}
    if (usuarioExistente == null) {
        println("Credenciales Incorrectas. Por favor registres sus credenciales correctas o registre un nuevo usuario")
        return main()
    }
    println("Por favor, Ingresa tu contraseña:")
    passwordd = readln().trim()
    
    aliass = usuarioExistente.alias
    emaill = usuarioExistente.email
    phoneNumberr = usuarioExistente.phoneNumber
    rolee = usuarioExistente.role
    
    showHome(usuarioExistente, listaUsuarios)
}

fun registro() {
    println("==========================================")
    println("Bienvenido")
    println("Por favor ingresa tu nombre de usuario")
    aliass = readln().trim()
    val aliasExistente = listaUsuarios.any { it.alias.equals(aliass, ignoreCase = true) }
    if (aliasExistente) {
        println("Este alias electronico ya existe, por favor inicia sesion nuevamente")
        return main()
    }
    println("Por favor, ingresa tu correo electronico")
    emaill = readln().trim()
    val usuarioExistente = listaUsuarios.any { it.email.equals(emaill, ignoreCase = true) }
    if (usuarioExistente) {
        println("Este correo electronico ya existe, por favor inicia sesion nuevamente")
        return main()
    }
    
    var opcion: Int
    println("Por favor, ingresa tu numero de telefono")
    phoneNumberr = readln().trim()
    println("Por favor, ingresa tu contraseña")
    passwordd = readln().trim()
    println("Cual es tu rol?")
    println("\n- Digita 1 si eres Gerente General")
    println("\n- Digita 2 si eres Supervisor de Ventas")
    opcion = readln().toInt()
    
    fun registroUsuario(): User {
        //Registro del usuario en el archivo .json
        val nuevoUsuario = User(
            alias = aliass,
            email = emaill,
            phoneNumber = phoneNumberr,
            password = passwordd,
            role = rolee
        )
        listaUsuarios.add(nuevoUsuario)
        
        //Convertimos a JSON la lista completa actualizada
        val jsonString = archivojson.encodeToString(listaUsuarios)
        
        try {
            archivo.writeText(jsonString)
            println("\n Su registro ha sido exitoso")
        } catch (e: Exception) {
            println("Error al escribir en el archivo: ${e.message}")
        }   
        return nuevoUsuario
    }
    
    when (opcion) {
        1 -> {
                        
            rolee = "GERENTE"
            val nuevoUsuario = registroUsuario()
            showHome(nuevoUsuario, listaUsuarios)
        }
        2 -> {
            rolee = "SUPERVISOR"
            val nuevoUsuario = registroUsuario()
            showHome(nuevoUsuario, listaUsuarios)
        }
        3 -> {
            rolee = "DUENO"
            val nuevoUsuario = registroUsuario()
            showHome(nuevoUsuario, listaUsuarios)
        }
    }

    //AsistenteIA()
}

fun main() {    
    if (archivo.exists() && archivo.length() > 0) {
        try {
            val contenidoJson = archivo.readText()
            //lista de usuarios
            val usuariosGuardados = Json.decodeFromString<List<User>>(contenidoJson)
            listaUsuarios.addAll(usuariosGuardados)
        } catch (e: Exception) {
            println("Error al leer el archivo, es probable que el archivo usuario.json este corrupto")
        }
    }
    
    println("==========================================")
    // Preguntas de inicio de sesion
    println("Bienvenido a KronoCorp, tu administrador empresarial de confianza.")
    
    opcionesInicio()
}
