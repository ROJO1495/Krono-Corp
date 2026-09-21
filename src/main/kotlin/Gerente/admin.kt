package Gerente

//Archivos del asistente (no se modifican, solo se reutilizan)
import AsistenteIA.AsistenteIA
import AsistenteIA.SupervisorEquipo
import AsistenteIA.VentaIntegrante
import AsistenteIA.datosSupervisores
import AsistenteIA.equiposLibres
import AsistenteIA.generarReportePdf
import AsistenteIA.guardarSupervisores
import AsistenteIA.listaSupervisores
import AsistenteIA.reportePdf

//Archivos de login (usuarios guardados en usuario.json)
import Login.User
import Login.archivo
import Login.archivojson


//Funcion para supervisores
import Negocio.negocioPropio

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun showHome(usuario: User, listaUsuarios: MutableList<User>) {

    println()
    println("===============================================")
    println("                    HOME")
    println("===============================================")
    println("Bienvenido: ${usuario.alias}")
    println("Rol: ${usuario.role}")
    println()

    when (usuario.role) {

        "GERENTE" -> {
            var enSesion = true
            //El menu se repite hasta que el gerente cierre sesion
            while (enSesion) {
                println()
                println("1. Evaluacion de supervisores")
                println("2. Historial de reportes")
                println("3. Reportes de Ventas")
                println("4. Asistente")
                println("5. Cerrar sesion")

                print("\nSelecciona una opcion: ")

                when (readln().trim()) {
                    "1" -> evaluacionSupervisores(listaUsuarios)
                    "2" -> historialReportes()
                    "3" -> reportesVentas()
                    "4" -> asistente()
                    "5" -> {
                        println("Sesion cerrada.")
                        enSesion = false
                    }
                    else -> println("Opcion invalida")
                }
            }
        }

        "SUPERVISOR" -> {
           negocioPropio(usuario)
        }
        
        "DUENO" -> {
           negocioPropio(usuario)
        }
    }
}

// ---------- Utilidades ----------

private fun pausar() {
    println("\nPresiona ENTER para regresar.")
    readln()
}

//supervisores.json se recarga desde cero para no duplicar la lista en memoria
private fun cargarSupervisoresEquipo() {
    listaSupervisores.clear()
    datosSupervisores()
}

//Guarda la lista de usuarios (login) en usuario.json
private fun guardarUsuarios(listaUsuarios: List<User>) {
    try {
        archivo.writeText(archivojson.encodeToString<List<User>>(listaUsuarios))
    } catch (e: Exception) {
        println("Error al escribir en el archivo: ${e.message}")
    }
}

//Lee datosNorte.json, datosCentro.json o datosSur.json segun el equipo
private fun ventasDeEquipo(equipo: String): List<VentaIntegrante> {
    val f = File("datos${equipo.replaceFirstChar { it.uppercase() }}.json")
    if (!f.exists()) return emptyList()
    return try {
        Json { ignoreUnknownKeys = true }.decodeFromString<List<VentaIntegrante>>(f.readText())
    } catch (e: Exception) {
        emptyList()
    }
}

// ---------- Evaluacion de supervisores ----------

fun evaluacionSupervisores(listaUsuarios: MutableList<User>) {

    while (true) {
        println()
        println("===============================================")
        println("      EVALUACION DE SUPERVISORES")
        println("===============================================")

        println("1. Ver Supervisores")
        println("2. Agregar nuevo supervisor")
        println("3. Eliminar supervisor")
        println("4. Volver")

        print("Selecciona una opcion: ")

        when (readln().trim()) {
            "1" -> mostrarSupervisores()
            "2" -> nuevoSupervisor(listaUsuarios)
            "3" -> eliminarSupervisor(listaUsuarios)
            "4" -> return
            else -> println("Opcion invalida")
        }
    }
}


fun mostrarSupervisores() {

    cargarSupervisoresEquipo()

    println()
    println("===============================================")
    println("          LISTA DE SUPERVISORES")
    println("===============================================")

    if (listaSupervisores.isEmpty()) {
        println("No hay supervisores registrados")
    } else {

        listaSupervisores.forEachIndexed { index, supervisor ->

            val ventas = ventasDeEquipo(supervisor.equipo)
            val total = ventas.sumOf { it.ventasRealizadas }
            val promedio = if (ventas.isEmpty()) 0.0 else total.toDouble() / ventas.size

            println()
            println("${index + 1}. ${supervisor.nombreSupervisor}")
            println("Correo: ${supervisor.gmail}")
            println("Telefono: ${supervisor.phoneNumber}")
            println("Equipo: ${supervisor.equipo}")

            if (ventas.isEmpty()) {
                println("Informacion de ventas: sin datos para este equipo")
            } else {
                println("Integrantes: ${ventas.size}")
                println("Total de ventas: $total")
                println("Promedio de ventas por integrante: ${"%.2f".format(promedio)}")
                ventas.forEach { println("   - ${it.integrante}: ${it.ventasRealizadas}") }
            }
            println("---------------------------------------")
        }
    }

    val libres = equiposLibres()
    if (libres.isNotEmpty()) {
        println("\nEquipos sin supervisor: ${libres.joinToString(", ")}")
    }

    pausar()
}


fun nuevoSupervisor(listaUsuarios: MutableList<User>) {

    cargarSupervisoresEquipo()

    println()
    println("======================================")
    println("          NUEVO SUPERVISOR")
    println("======================================")

    val libres = equiposLibres()
    if (libres.isEmpty()) {
        println("Todos los equipos ya tienen supervisor. Elimina uno para liberar un equipo.")
        pausar()
        return
    }
    println("Equipos libres: ${libres.joinToString(", ")}")

    println("Nombre del supervisor: ")
    val alias = readln().trim()

    val aliasExiste = listaUsuarios.any { it.alias.equals(alias, ignoreCase = true) } ||
        listaSupervisores.any { it.nombreSupervisor.equals(alias, ignoreCase = true) }
    if (aliasExiste) {
        println("\nYa existe un usuario con ese nombre.")
        pausar()
        return
    }

    println("Ingrese el correo electrónico: ")
    val email = readln().trim()

    val emailExiste = listaUsuarios.any { it.email.equals(email, ignoreCase = true) } ||
        listaSupervisores.any { it.gmail.equals(email, ignoreCase = true) }
    if (emailExiste) {
        println("\nYa existe un usuario con ese correo.")
        pausar()
        return
    }

    println("Ingrese el Número de teléfono: ")
    val telefono = readln().trim()

    println(" Contraseña: ")
    val password = readln().trim()

    if (alias.isEmpty() || email.isEmpty() || telefono.isEmpty() || password.isEmpty()) {
        println("\nTodos los campos son obligatorios. No se creo el supervisor.")
        pausar()
        return
    }

    println("Equipo que tendra a cargo (${libres.joinToString(", ")}): ")
    val equipo = libres.find { it.equals(readln().trim(), ignoreCase = true) }
    if (equipo == null) {
        println("\nEquipo no valido o no disponible. No se creo el supervisor.")
        pausar()
        return
    }

    //1. Usuario para iniciar sesion (usuario.json)
    val nuevoUsuario = User(
        alias = alias,
        email = email,
        phoneNumber = telefono,
        password = password,
        role = "SUPERVISOR"
    )
    listaUsuarios.add(nuevoUsuario)
    guardarUsuarios(listaUsuarios)

    //2. Supervisor asignado a su equipo (supervisores.json, el que usa el asistente)
    val nuevoId = (listaSupervisores.maxOfOrNull { it.id } ?: 0) + 1
    listaSupervisores.add(SupervisorEquipo(nuevoId, alias, email, telefono, equipo))
    guardarSupervisores()

    println()
    println("Supervisor creado correctamente.")
    println("Nombre: ${nuevoUsuario.alias}")
    println("Correo: ${nuevoUsuario.email}")
    println("Equipo: $equipo")
    println("Rol: ${nuevoUsuario.role}")

    pausar()
}


fun eliminarSupervisor(listaUsuarios: MutableList<User>) {

    cargarSupervisoresEquipo()

    println()
    println("======================================")
    println("        ELIMINAR SUPERVISOR")
    println("======================================")

    if (listaSupervisores.isEmpty()) {
        println("No hay supervisores registrados.")
        pausar()
        return
    }

    listaSupervisores.forEach { println("Id ${it.id} - ${it.nombreSupervisor} (Equipo ${it.equipo})") }
    println("Digite el id o el nombre del supervisor que desea eliminar:")
    val busqueda = readln().trim()
    val supervisor = listaSupervisores.find {
        it.id.toString() == busqueda || it.nombreSupervisor.equals(busqueda, ignoreCase = true)
    }
    if (supervisor == null) {
        println("Supervisor no encontrado.")
        pausar()
        return
    }

    println("Seguro que desea eliminar a ${supervisor.nombreSupervisor}? (s/n)")
    if (!readln().trim().equals("s", ignoreCase = true)) {
        println("Operacion cancelada.")
        pausar()
        return
    }

    //Se elimina de supervisores.json (el equipo queda libre)...
    listaSupervisores.remove(supervisor)
    guardarSupervisores()

    //...y tambien su usuario de login, si existe
    val usuarioEliminado = listaUsuarios.removeAll {
        it.role == "SUPERVISOR" && it.email.equals(supervisor.gmail, ignoreCase = true)
    }
    if (usuarioEliminado) guardarUsuarios(listaUsuarios)

    println("Supervisor eliminado. El equipo ${supervisor.equipo} quedo libre para un nuevo supervisor.")
    pausar()
}

// ---------- Reportes ----------

fun reportesVentas() {

    println()
    println("===============================================")
    println("             REPORTES DE VENTAS")
    println("===============================================")

    //Cada reporte lleva fecha y hora en el nombre para que quede en el historial
    val marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val ruta = "Reporte_ventas_$marca.pdf"

    println("Generando $ruta ...")
    if (generarReportePdf(ruta)) {
        println("Reporte generado con exito en: ${File(ruta).absolutePath}")
    }

    pausar()
}

fun historialReportes() {

    println()
    println("===============================================")
    println("            HISTORIAL DE REPORTES")
    println("===============================================")

    //Reportes de este menu (Reporte_ventas_*.pdf) y el que genera el asistente (Promedios_de_ventas.pdf)
    val archivos = File(".").listFiles { f ->
        f.isFile && f.extension.equals("pdf", ignoreCase = true) &&
            (f.name.startsWith("Reporte_ventas_") || f.name == reportePdf)
    } ?: emptyArray<File>()
    val reportes = archivos.sortedByDescending { it.lastModified() }

    if (reportes.isEmpty()) {
        println("Aun no se ha generado ningun reporte.")
    } else {
        val formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        reportes.forEachIndexed { index, f ->
            val fecha = LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault())
            println()
            println("${index + 1}. ${f.name}")
            println("Fecha: ${fecha.format(formato)}")
            println("Tamano: ${maxOf(1L, f.length() / 1024)} KB")
        }
        println()
        println("Ubicacion: ${File(".").absoluteFile.normalize()}")
    }

    pausar()
}

// ---------- Asistente ----------

fun asistente() {
    //AsistenteIA() vuelve a cargar supervisores.json; se limpia antes para que no se dupliquen
    listaSupervisores.clear()
    AsistenteIA()
}