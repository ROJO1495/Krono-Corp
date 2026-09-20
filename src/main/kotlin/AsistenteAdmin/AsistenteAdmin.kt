package AsistenteAdmin

import Login.User
import Negocio.ErrorLogger
import Negocio.Negocio
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.Normalizer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Modulo de Persona 4: Asistente (ChatBot), Historial de reportes, Historial de graficos
// y Nueva cuenta de administracion.
// Version beta: las respuestas del asistente son simuladas y usan datos de ejemplo,
// todavia no se conecta a Groq ni a Gemini.

@Serializable
data class RegistroHistorial(
    val id: Int,
    val autor: String,
    val titulo: String,
    val fecha: String,
    val contenido: String
)

// Datos de ejemplo para mostrar como funcionaria el asistente
private data class VentaEjemplo(val producto: String, val cantidad: Int, val total: Double)
private data class ProductoEjemplo(val nombre: String, val stock: Int)
private data class MesEjemplo(val mes: String, val total: Double)

private val ventasEjemplo = listOf(
    VentaEjemplo("Camisa", 12, 180.00),
    VentaEjemplo("Pantalon", 7, 175.00),
    VentaEjemplo("Zapatos", 4, 200.00),
    VentaEjemplo("Gorra", 15, 90.00)
)

private val inventarioEjemplo = listOf(
    ProductoEjemplo("Camisa", 40),
    ProductoEjemplo("Pantalon", 25),
    ProductoEjemplo("Zapatos", 3),
    ProductoEjemplo("Gorra", 60)
)

private val mesesEjemplo = listOf(
    MesEjemplo("Enero", 1200.00),
    MesEjemplo("Febrero", 950.00),
    MesEjemplo("Marzo", 1580.00),
    MesEjemplo("Abril", 1100.00)
)

private const val STOCK_BAJO = 5
private const val CONTRASENA_MINIMA = 4

private val archivoReportes = File("historial_reportes.json")
private val archivoGraficos = File("historial_graficos.json")
private val archivoUsuarios = File("usuario.json")
private val json = Json { prettyPrint = true }
private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

// ==============================================================
// HISTORIAL (archivos .json)
// ==============================================================

private fun cargarHistorial(archivo: File): MutableList<RegistroHistorial> {
    if (!archivo.exists() || archivo.length() == 0L) return mutableListOf()
    return try {
        json.decodeFromString<List<RegistroHistorial>>(archivo.readText()).toMutableList()
    } catch (e: Exception) {
        println("Error al leer ${archivo.name}, es probable que el archivo este corrupto")
        ErrorLogger.registrarError("No se pudo leer ${archivo.name}", e)
        mutableListOf()
    }
}

private fun guardarHistorial(archivo: File, lista: List<RegistroHistorial>) {
    try {
        archivo.writeText(json.encodeToString(lista))
    } catch (e: Exception) {
        println("Error al escribir en ${archivo.name}: ${e.message}")
        ErrorLogger.registrarError("No se pudo escribir ${archivo.name}", e)
    }
}

// El asistente guarda de forma automatica cada reporte o grafico que genera
private fun registrarEnHistorial(archivo: File, autor: String, titulo: String, contenido: String) {
    val lista = cargarHistorial(archivo)
    lista.add(
        RegistroHistorial(
            id = (lista.maxOfOrNull { it.id } ?: 0) + 1,
            autor = autor,
            titulo = titulo,
            fecha = LocalDateTime.now().format(formatoFecha),
            contenido = contenido
        )
    )
    guardarHistorial(archivo, lista)
}

private fun verHistorial(archivo: File, autor: String, nombre: String) {
    val propios = cargarHistorial(archivo).filter { it.autor.equals(autor, ignoreCase = true) }
    println("\n--- Historial de $nombre ---")
    if (propios.isEmpty()) {
        println("Aun no has generado $nombre. Pidele al asistente que genere uno.")
        return
    }
    propios.forEach { println("${it.id}. ${it.titulo} (${it.fecha})") }
    print("\nEscribe el numero para verlo completo, o ENTER para volver: ")
    val id = readlnOrNull()?.trim()?.toIntOrNull() ?: return
    val elegido = propios.find { it.id == id }
    if (elegido == null) println("No existe un registro con ese numero.")
    else println("\n${elegido.titulo} - ${elegido.fecha}\n\n${elegido.contenido}")
}

fun historialReportesNegocio(negocio: Negocio) {
    println("\n==============================================")
    println("            HISTORIAL DE REPORTES")
    println("==============================================")
    verHistorial(archivoReportes, negocio.propietarioEmail, "reportes")
}

fun historialGraficosNegocio(negocio: Negocio) {
    println("\n==============================================")
    println("            HISTORIAL DE GRAFICOS")
    println("==============================================")
    verHistorial(archivoGraficos, negocio.propietarioEmail, "graficos")
}

// ==============================================================
// ASISTENTE (preguntas estaticas + respuestas simuladas)
// ==============================================================

// Mensaje inicial con las opciones que el usuario puede escoger
private fun mostrarOpciones(negocio: Negocio) {
    println()
    println("===============================================")
    println("            ASISTENTE KRONO (BETA)")
    println("            Negocio: ${negocio.nombreNegocio}")
    println("===============================================")
    println("Que te gustaria hacer hoy?")
    println("(Escribe la opcion, o preguntale lo que quieras al asistente)")
    if (negocio.preguntasIA.isNotEmpty()) {
        println("\nPreguntas de tu negocio:")
        negocio.preguntasIA.forEachIndexed { i, p -> println("  P${i + 1}. $p") }
    }
    println("\nOpciones generales:")
    println("  1. Ver el resumen de ventas")
    println("  2. Ver el producto mas vendido")
    println("  3. Consultar el inventario")
    println("  4. Ver productos con poco stock")
    println("  5. Generar un reporte de ventas")
    println("  6. Generar un grafico de ventas")
    println("  7. Ver historial de reportes")
    println("  8. Ver historial de graficos")
    println("  9. Salir del asistente")
    println()
    print("Selecciona una opcion: ")
}

private fun resumenVentas() {
    println("\n--- Resumen de ventas (datos de ejemplo) ---")
    ventasEjemplo.forEach {
        println("${it.producto.padEnd(10)} ${it.cantidad.toString().padStart(3)} unidades   $${"%.2f".format(it.total)}")
    }
    println("-------------------------------------------")
    println("Total vendido: $${"%.2f".format(ventasEjemplo.sumOf { it.total })}")
}

private fun productoMasVendido() {
    val top = ventasEjemplo.maxByOrNull { it.cantidad }
    println("\n--- Producto mas vendido (datos de ejemplo) ---")
    if (top == null) println("Aun no hay ventas registradas.")
    else println("${top.producto} con ${top.cantidad} unidades vendidas.")
}

private fun mesMayoresVentas() {
    val mejor = mesesEjemplo.maxByOrNull { it.total }
    println("\n--- Meses con mayores ventas (datos de ejemplo) ---")
    mesesEjemplo.sortedByDescending { it.total }.forEach {
        println("${it.mes.padEnd(10)} $${"%.2f".format(it.total)}")
    }
    if (mejor != null) println("\nEl mejor mes fue ${mejor.mes}.")
}

private fun consultarInventario() {
    println("\n--- Inventario (datos de ejemplo) ---")
    inventarioEjemplo.forEach { println("${it.nombre.padEnd(10)} ${it.stock} unidades") }
}

private fun productosPocoStock() {
    val bajos = inventarioEjemplo.filter { it.stock <= STOCK_BAJO }
    println("\n--- Productos con poco stock ($STOCK_BAJO o menos) ---")
    if (bajos.isEmpty()) println("Todos los productos tienen stock suficiente.")
    else bajos.forEach { println("${it.nombre}: quedan ${it.stock} unidades") }
}

private fun textoReporte(negocio: Negocio): String = buildString {
    appendLine("Reporte de ventas - ${negocio.nombreNegocio}")
    ventasEjemplo.forEach {
        appendLine("${it.producto}: ${it.cantidad} unidades - $${"%.2f".format(it.total)}")
    }
    append("Total: $${"%.2f".format(ventasEjemplo.sumOf { it.total })}")
}

private fun textoGrafico(): String {
    val maximo = ventasEjemplo.maxOf { it.cantidad }
    return buildString {
        appendLine("Unidades vendidas por producto")
        ventasEjemplo.forEachIndexed { i, v ->
            val barra = "#".repeat(v.cantidad * 20 / maximo)
            val linea = "${v.producto.padEnd(10)} | $barra ${v.cantidad}"
            if (i < ventasEjemplo.lastIndex) appendLine(linea) else append(linea)
        }
    }
}

private fun generarReporte(negocio: Negocio) {
    val contenido = textoReporte(negocio)
    println("\n$contenido")
    registrarEnHistorial(archivoReportes, negocio.propietarioEmail, "Reporte de ventas", contenido)
    println("\nReporte guardado en el historial de reportes.")
}

private fun generarGrafico(negocio: Negocio) {
    val contenido = textoGrafico()
    println("\n$contenido")
    registrarEnHistorial(archivoGraficos, negocio.propietarioEmail, "Grafico de ventas por producto", contenido)
    println("\nGrafico guardado en el historial de graficos.")
}

private fun sinAcentos(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

// Respuesta simulada para una pregunta escrita libremente o configurada por el negocio
// (aqui se conectara la IA mas adelante)
private fun respuestaSimulada(pregunta: String, negocio: Negocio) {
    val p = sinAcentos(pregunta)
    println()
    when {
        Regex("\\bmes(es)?\\b").containsMatchIn(p) -> mesMayoresVentas()
        "vendido" in p || "producto" in p -> productoMasVendido()
        "venta" in p -> resumenVentas()
        "inventario" in p || "stock" in p -> consultarInventario()
        "reporte" in p -> println("Puedo generar un reporte de ventas. Escribe 5 para crearlo.")
        "grafico" in p || "grafica" in p -> println("Puedo generar un grafico de ventas. Escribe 6 para crearlo.")
        else -> println("Soy Krono (beta). Todavia no puedo responder esa pregunta libremente, pero pronto podre analizar los datos de ${negocio.nombreNegocio}. Por ahora prueba con alguna opcion del menu.")
    }
}

private fun preguntaDelNegocio(indice: Int, negocio: Negocio) {
    val pregunta = negocio.preguntasIA.getOrNull(indice)
    if (pregunta == null) {
        println("\nNo existe esa pregunta. Revisa el numero que aparece en la lista.")
        return
    }
    println("\nTu pregunta: $pregunta")
    respuestaSimulada(pregunta, negocio)
}

fun asistenteNegocio(negocio: Negocio) {
    println("\nHola, soy Krono, el asistente de ${negocio.nombreNegocio}.")
    val esPregunta = Regex("^[Pp](\\d+)$")
    while (true) {
        mostrarOpciones(negocio)
        val entrada = readlnOrNull()?.trim() ?: return
        val pregunta = esPregunta.matchEntire(entrada)
        when {
            pregunta != null -> preguntaDelNegocio(pregunta.groupValues[1].toInt() - 1, negocio)
            entrada == "1" -> resumenVentas()
            entrada == "2" -> productoMasVendido()
            entrada == "3" -> consultarInventario()
            entrada == "4" -> productosPocoStock()
            entrada == "5" -> generarReporte(negocio)
            entrada == "6" -> generarGrafico(negocio)
            entrada == "7" -> verHistorial(archivoReportes, negocio.propietarioEmail, "reportes")
            entrada == "8" -> verHistorial(archivoGraficos, negocio.propietarioEmail, "graficos")
            entrada == "9" -> {
                println("Hasta pronto.")
                return
            }
            entrada.isEmpty() -> println("Por favor escribe una opcion o una pregunta.")
            else -> respuestaSimulada(entrada, negocio)
        }
        print("\nPresiona ENTER para continuar...")
        readlnOrNull()
    }
}

// ==============================================================
// NUEVA CUENTA DE ADMINISTRACION
// ==============================================================

// Lee usuario.json. Devuelve null si no se puede leer, para no sobrescribir un archivo danado.
private fun cargarUsuarios(): MutableList<User>? {
    if (!archivoUsuarios.exists()) {
        println("No se encontro usuario.json, no se puede crear la cuenta.")
        ErrorLogger.registrarError("usuario.json no existe al crear una nueva cuenta de administracion")
        return null
    }
    return try {
        Json.decodeFromString<List<User>>(archivoUsuarios.readText()).toMutableList()
    } catch (e: Exception) {
        println("Error al leer usuario.json, es probable que el archivo este corrupto. No se creo la cuenta.")
        ErrorLogger.registrarError("No se pudo leer usuario.json al crear una nueva cuenta", e)
        null
    }
}

// Crea otra cuenta de administracion para el mismo negocio, sin pedir otro correo.
// Como el login identifica por correo + contrasena, la nueva cuenta usa el mismo correo
// con su propia contrasena, y entra al mismo negocio.
fun nuevaCuentaAdministracion(usuario: User) {
    println("\n==============================================")
    println("        NUEVA CUENTA DE ADMINISTRACION")
    println("==============================================")
    println("Se creara otra cuenta para administrar este negocio con el correo ${usuario.email}.")
    println("No necesitas ingresar otro correo electronico.\n")

    print("Nombre de la nueva cuenta: ")
    val nombre = readlnOrNull()?.trim().orEmpty()
    if (nombre.isEmpty()) {
        println("El nombre no puede estar vacio. No se creo la cuenta.")
        return
    }

    print("Contrasena de la nueva cuenta (minimo $CONTRASENA_MINIMA caracteres): ")
    val password = readlnOrNull()?.trim().orEmpty()
    if (password.length < CONTRASENA_MINIMA) {
        println("La contrasena debe tener al menos $CONTRASENA_MINIMA caracteres. No se creo la cuenta.")
        return
    }

    val usuarios = cargarUsuarios() ?: return
    val mismoCorreo = usuarios.filter { it.email.equals(usuario.email, ignoreCase = true) }

    if (mismoCorreo.any { it.name.equals(nombre, ignoreCase = true) }) {
        println("Ya existe una cuenta con el nombre \"$nombre\" para este correo. No se creo la cuenta.")
        return
    }
    // El login compara la contrasena sin distinguir mayusculas, asi que se valida igual
    if (mismoCorreo.any { it.password.equals(password, ignoreCase = true) }) {
        println("Esa contrasena ya la usa otra cuenta de este correo. Elige una distinta.")
        return
    }

    usuarios.add(
        User(
            name = nombre,
            email = usuario.email,
            phoneNumber = usuario.phoneNumber,
            password = password,
            role = usuario.role
        )
    )

    try {
        archivoUsuarios.writeText(json.encodeToString(usuarios as List<User>))
        println("\nCuenta \"$nombre\" creada correctamente.")
        println("Para entrar con ella, inicia sesion con el correo ${usuario.email} y la nueva contrasena.")
    } catch (e: Exception) {
        println("Error al escribir en usuario.json: ${e.message}")
        ErrorLogger.registrarError("No se pudo guardar la nueva cuenta de administracion", e)
    }
}
