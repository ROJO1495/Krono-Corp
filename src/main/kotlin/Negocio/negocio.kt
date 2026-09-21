package Negocio

import Login.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ==========================================
// REGISTRO DE ERRORES (REQUERIDO POR RÚBRICA)
// ==========================================
object ErrorLogger {
    private val archivoLog = File("errores.log")

    fun registrarError(mensaje: String, excepcion: Exception? = null) {
        val hora = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val detalle = excepcion?.stackTraceToString() ?: "Sin excepción técnica"
        archivoLog.appendText("[$hora] LOG: $mensaje\nDetalle: $detalle\n----------------------------------------\n")
    }
}

// ==========================================
// MODELOS DE DATOS (POO Y SERIALIZACIÓN)
// ==========================================
@Serializable
data class Sucursal(
    val id: String,
    var nombre: String,
    var activa: Boolean = true,
    var codigoAcceso: String = "1234"
)

@Serializable
data class Negocio(
    var nombreNegocio: String,
    val propietarioEmail: String,
    val tipo: String, // "PROPIO" o "SUCURSAL"
    val preguntasIA: MutableList<String> = mutableListOf(),
    val sucursales: MutableList<Sucursal> = mutableListOf()
)

// Variables para manejo de datos en JSON
val archivoNegocios = File("negocios.json")
val jsonConfig = Json { prettyPrint = true; ignoreUnknownKeys = true }

// Roles que administran un negocio propio. Deben coincidir con los que admin.kt (showHome)
// envia a este modulo: SUPERVISOR y DUENO.
val ROLES_CON_NEGOCIO = listOf("SUPERVISOR", "DUENO")

// Cargar lista desde negocios.json
fun cargarNegocios(): MutableList<Negocio> {
    val lista = mutableListOf<Negocio>()
    if (archivoNegocios.exists() && archivoNegocios.length() > 0) {
        try {
            val contenido = archivoNegocios.readText()
            lista.addAll(jsonConfig.decodeFromString<List<Negocio>>(contenido))
        } catch (e: Exception) {
            ErrorLogger.registrarError("Fallo al decodificar negocios.json", e)
            println("Aviso: No se pudo leer negocios.json de manera correcta.")
        }
    }
    return lista
}

// Guardar lista en negocios.json
fun guardarNegocios(lista: List<Negocio>) {
    try {
        val jsonTexto = jsonConfig.encodeToString(lista)
        archivoNegocios.writeText(jsonTexto)
    } catch (e: Exception) {
        ErrorLogger.registrarError("Error crítico al guardar en negocios.json", e)
        println("Error al guardar información en el archivo: ${e.message}")
    }
}

// ==============================================================
// VALIDACIÓN Y ENTRADA AL FLUJO (ROLES SUPERVISOR Y DUENO)
// ==============================================================
fun negocioPropio(usuario: User) {
    // Validación estricta de rol
    if (ROLES_CON_NEGOCIO.none { it.equals(usuario.role, ignoreCase = true) }) {
        println("\n❌ Acceso denegado: Este módulo solo está habilitado para usuarios con rol SUPERVISOR o DUENO.")
        ErrorLogger.registrarError("Acceso denegado a usuario ${usuario.email} con rol ${usuario.role}")
        return
    }

    val listaNegocios = cargarNegocios()
    var miNegocio = listaNegocios.find {
        it.tipo == "PROPIO" && it.propietarioEmail.equals(usuario.email, ignoreCase = true)
    }

    // Si aún no tiene un negocio configurado, creamos uno inicial con las preguntas del Mockup
    if (miNegocio == null) {
        miNegocio = Negocio(
            nombreNegocio = "Mi Negocio",
            propietarioEmail = usuario.email,
            tipo = "PROPIO",
            preguntasIA = mutableListOf(
                "Sobre Productos: ¿Cuál es el producto más vendido?",
                "Sobre Ventas: ¿Cuáles son los meses con mayores ventas?"
            )
        )
        listaNegocios.add(miNegocio)
        guardarNegocios(listaNegocios)
    }

    // Entra directamente a la pantalla central HOME (Mockup Pantalla 3)
    home(usuario, miNegocio, listaNegocios)
}

// ==============================================================
// 3. HOME: ADMINISTRACIÓN DE VENTAS (Mockup Pantalla 3)
// ==============================================================
fun home(usuario: User, negocio: Negocio, listaNegocios: MutableList<Negocio>) {
    while (true) {
        println("\n==============================================")
        println("          ADMINISTRACIÓN DE VENTAS")
        println("          Bienvenido, ${if (usuario.alias.isNotEmpty()) usuario.alias else usuario.email}")
        println("          Negocio: ${negocio.nombreNegocio}")
        println("==============================================")
        println("ACCIONES RÁPIDAS:")
        println("1. Mi Negocio (Nombre y Preguntas IA)")
        println("2. Administración de Sucursales")
        println("3. Asistente (ChatBot)")
        println("4. Historial de reportes")
        println("5. Historial de Gráficos")
        println("6. Nueva cuenta de Adm.")
        println("7. Configuración")
        println("8. Cerrar Sesión")
        print("\nSelecciona una opción: ")

        when (readln().trim()) {
            "1" -> formularioMiNegocio(negocio, listaNegocios)
            "2" -> administracionSucursales(negocio, listaNegocios)
            "3" -> asistentePersona4(negocio)
            "4" -> historialReportesPersona4(negocio)
            "5" -> historialGraficosPersona4(negocio)
            "6" -> nuevaCuentaAdministracionPersona4(usuario)
            "7" -> {
                val sesionCerrada = menuConfiguracion(usuario, negocio, listaNegocios)
                if (sesionCerrada) return
            }
            "8" -> {
                println("Cerrando sesión de ${usuario.email}...")
                return
            }
            else -> println("Opción no válida. Intente nuevamente.")
        }
    }
}

// ==============================================================
// 1. FORMULARIO NEGOCIO PROPIO (Mockup Pantalla 1)
// ==============================================================
fun formularioMiNegocio(negocio: Negocio, listaNegocios: MutableList<Negocio>) {
    while (true) {
        println("\n==============================================")
        println("           MI NEGOCIO")
        println("==============================================")
        println("Nombre actual: ${negocio.nombreNegocio}")
        println("\n1. Modificar nombre del negocio")
        println("2. Ver / Configurar preguntas para la IA")
        println("3. Guardar cambios y Volver al Home")
        print("\nSelecciona una opción: ")

        when (readln().trim()) {
            "1" -> {
                print("Ingresa el nuevo nombre para el negocio: ")
                val nuevoNombre = readln().trim()
                if (nuevoNombre.isNotEmpty()) {
                    negocio.nombreNegocio = nuevoNombre
                    guardarNegocios(listaNegocios)
                    println("✔ Nombre actualizado exitosamente.")
                }
            }
            "2" -> menuPreguntasIA(negocio, listaNegocios)
            "3" -> {
                guardarNegocios(listaNegocios)
                println("✔ Cambios guardados correctamente.")
                break // Regresa al Home
            }
            else -> println("Opción no válida.")
        }
    }
}

// Submenú para administrar las preguntas de la IA
fun menuPreguntasIA(negocio: Negocio, listaNegocios: MutableList<Negocio>) {
    while (true) {
        println("\n--- CONFIGURACIÓN DE PREGUNTAS PARA LA IA ---")
        if (negocio.preguntasIA.isEmpty()) {
            println("(No hay preguntas registradas)")
        } else {
            negocio.preguntasIA.forEachIndexed { indice, pregunta ->
                println("${indice + 1}. $pregunta")
            }
        }
        println("\n[A] Agregar nueva pregunta")
        println("[E] Editar pregunta existente")
        println("[B] Borrar pregunta")
        println("[V] Volver al menú de Mi Negocio")
        print("Elige una opción: ")

        when (readln().trim().uppercase()) {
            "A" -> {
                print("Ingresa la nueva pregunta para la IA: ")
                val nueva = readln().trim()
                if (nueva.isNotEmpty()) {
                    negocio.preguntasIA.add(nueva)
                    guardarNegocios(listaNegocios)
                    println("✔ Pregunta agregada.")
                }
            }
            "E" -> {
                print("Número de pregunta a editar: ")
                val num = readln().trim().toIntOrNull()
                if (num != null && num in 1..negocio.preguntasIA.size) {
                    print("Escribe la nueva redacción: ")
                    val texto = readln().trim()
                    if (texto.isNotEmpty()) {
                        negocio.preguntasIA[num - 1] = texto
                        guardarNegocios(listaNegocios)
                        println("✔ Pregunta actualizada.")
                    }
                } else {
                    println("Número inválido.")
                }
            }
            "B" -> {
                print("Número de pregunta a eliminar: ")
                val num = readln().trim().toIntOrNull()
                if (num != null && num in 1..negocio.preguntasIA.size) {
                    negocio.preguntasIA.removeAt(num - 1)
                    guardarNegocios(listaNegocios)
                    println("✔ Pregunta eliminada.")
                } else {
                    println("Número inválido.")
                }
            }
            "V" -> break
            else -> println("Opción no válida.")
        }
    }
}

// ==============================================================
// 2. ADMINISTRACIÓN DE SUCURSALES (Mockup Pantalla 2)
// ==============================================================
fun administracionSucursales(negocio: Negocio, listaNegocios: MutableList<Negocio>) {
    while (true) {
        println("\n==============================================")
        println("         ADMINISTRACIÓN DE SUCURSALES")
        println("==============================================")
        if (negocio.sucursales.isEmpty()) {
            println("No tienes sucursales registradas todavía.")
        } else {
            negocio.sucursales.forEachIndexed { i, suc ->
                val estado = if (suc.activa) "[ACTIVA]" else "[INACTIVA]"
                println("${i + 1}. ${suc.nombre} $estado (Código: ${suc.codigoAcceso})")
            }
        }

        println("\n1. Agregar Sucursal")
        println("[E] Editar nombre de Sucursal")
        println("[T] Activar / Desactivar Sucursal")
        println("[B] Eliminar Sucursal")
        println("[V] Volver al Home")
        print("\nSelecciona una opción: ")

        when (readln().trim().uppercase()) {
            "1" -> {
                print("Ingresa el nombre de la sucursal (ej. Sucursal 1 - Centro): ")
                val nom = readln().trim()
                if (nom.isNotEmpty()) {
                    print("Define la contraseña de acceso para esta sucursal: ")
                    val pass = readln().trim()
                    // Se usa el mayor numero existente + 1 (no el tamano de la lista) para que
                    // el id no se repita despues de eliminar una sucursal
                    val siguiente = (negocio.sucursales
                        .mapNotNull { it.id.removePrefix("SUC-").toIntOrNull() }
                        .maxOrNull() ?: 0) + 1
                    val nuevoId = "SUC-$siguiente"
                    val nuevaSucursal = Sucursal(
                        id = nuevoId,
                        nombre = nom,
                        codigoAcceso = if (pass.isEmpty()) "1234" else pass
                    )
                    negocio.sucursales.add(nuevaSucursal)
                    guardarNegocios(listaNegocios)
                    println("✔ Sucursal registrada con éxito.")
                }
            }
            "E" -> {
                print("Número de sucursal a modificar: ")
                val num = readln().trim().toIntOrNull()
                if (num != null && num in 1..negocio.sucursales.size) {
                    print("Nuevo nombre de la sucursal: ")
                    val nuevoNom = readln().trim()
                    if (nuevoNom.isNotEmpty()) {
                        negocio.sucursales[num - 1].nombre = nuevoNom
                        guardarNegocios(listaNegocios)
                        println("✔ Sucursal modificada.")
                    }
                } else {
                    println("Número incorrecto.")
                }
            }
            "T" -> {
                print("Número de sucursal a activar/desactivar: ")
                val num = readln().trim().toIntOrNull()
                if (num != null && num in 1..negocio.sucursales.size) {
                    val suc = negocio.sucursales[num - 1]
                    suc.activa = !suc.activa
                    guardarNegocios(listaNegocios)
                    println("✔ ${suc.nombre} ahora está ${if (suc.activa) "ACTIVA" else "INACTIVA"}.")
                } else {
                    println("Número incorrecto.")
                }
            }
            "B" -> {
                print("Número de sucursal a eliminar: ")
                val num = readln().trim().toIntOrNull()
                if (num != null && num in 1..negocio.sucursales.size) {
                    negocio.sucursales.removeAt(num - 1)
                    guardarNegocios(listaNegocios)
                    println("✔ Sucursal eliminada.")
                } else {
                    println("Número incorrecto.")
                }
            }
            "V" -> break // Regresa al Home
            else -> println("Opción no válida.")
        }
    }
}

// ==============================================================
// 4. MENÚ DE CONFIGURACIÓN (Mockup Pantalla 4)
// ==============================================================
fun menuConfiguracion(usuario: User, negocio: Negocio, listaNegocios: MutableList<Negocio>): Boolean {
    while (true) {
        println("\n==============================================")
        println("                CONFIGURACIÓN")
        println("   Gestiona tu cuenta y preferencias de la app")
        println("==============================================")
        println("1. Mi Cuenta")
        println("2. Seguridad y Contraseña")
        println("3. Notificaciones")
        println("4. Idioma")
        println("5. Reportar un fallo")
        println("6. Cerrar Sesión")
        println("7. Volver al Home")
        print("\nSelecciona una opción: ")

        when (readln().trim()) {
            "1" -> {
                println("\n--- MI CUENTA ---")
                println("Nombre: ${if (usuario.alias.isNotEmpty()) usuario.alias else "No especificado"}")
                println("Correo: ${usuario.email}")
                println("Teléfono: ${usuario.phoneNumber}")
                println("Rol actual: ${usuario.role}")
            }
            "2" -> println("\n[Seguridad y Contraseña] Próximamente en la versión móvil con Appwrite.")
            "3" -> println("\n[Notificaciones] Notificaciones push: ACTIVADAS.")
            "4" -> println("\n[Idioma] Idioma de la interfaz: Español.")
            "5" -> {
                print("\nDescribe el fallo o error presentado: ")
                val detalle = readln().trim()
                if (detalle.isNotEmpty()) {
                    ErrorLogger.registrarError("Reporte de usuario (${usuario.email}): $detalle")
                    println("✔ Gracias. El error ha sido registrado en errores.log.")
                }
            }
            "6" -> {
                println("Cerrando sesión...")
                return true // Cierra sesión y sale al login
            }
            "7" -> return false // Vuelve al Home
            else -> println("Opción no válida.")
        }
    }
}

// ==============================================================
// ESPACIO RESERVADO PARA PERSONA 4
// ==============================================================

// La implementación está en AsistenteAdmin/AsistenteAdmin.kt (Persona 4)
fun asistentePersona4(negocio: Negocio) = AsistenteAdmin.asistenteNegocio(negocio)

fun historialReportesPersona4(negocio: Negocio) = AsistenteAdmin.historialReportesNegocio(negocio)

fun historialGraficosPersona4(negocio: Negocio) = AsistenteAdmin.historialGraficosNegocio(negocio)

fun nuevaCuentaAdministracionPersona4(usuario: User) = AsistenteAdmin.nuevaCuentaAdministracion(usuario)