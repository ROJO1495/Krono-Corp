package AsistenteIA

import Login.emaill
import Login.listaUsuarios

//Importacion de archivos IA
import IA.GestorIA

//Importaciones para archivos .json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

//Importaciones para documentos pdf
import org.openpdf.text.Chunk
import org.openpdf.text.Document
import org.openpdf.text.Element
import org.openpdf.text.FontFactory
import org.openpdf.text.Paragraph
import org.openpdf.text.Phrase
import org.openpdf.text.pdf.PdfPCell
import org.openpdf.text.pdf.PdfPTable
import org.openpdf.text.pdf.PdfWriter
import java.io.FileOutputStream

//Importaciones para el grafico (JFreeChart)
import org.jfree.chart.ChartFactory
import org.jfree.chart.labels.StandardPieSectionLabelGenerator
import org.jfree.chart.plot.PiePlot
import org.jfree.data.general.DefaultPieDataset
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import javax.imageio.ImageIO

@Serializable
data class VentaIntegrante(
    val equipo: String,
    val integrante: String,
    @SerialName("ventas_realizadas") val ventasRealizadas: Int
)

@Serializable
data class SupervisorEquipo(
    val id: Int,
    val nombreSupervisor: String,
    val gmail: String,
    val phoneNumber: String,
    val equipo: String
)

//Variable de llamada del gestorIA
val gestorIA = GestorIA()

//Variables para llamar al archivo .json y corroborar si el archivo ya existe
val archivoSupervisores = File("supervisores.json")
val archivojsonSupervisores = Json { prettyPrint = true }

val listaSupervisores = mutableListOf<SupervisorEquipo>()

//Variable para el documento pdf
val reportePdf = "Promedios_de_ventas.pdf"

//Abrir archivo .json para leer supervisor.json
fun datosSupervisores() {
    if (archivoSupervisores.exists() && archivoSupervisores.length() > 0) {
        try {
            val contenidoJson = archivoSupervisores.readText()
            //lista de supervisores
            val supervisoresGuardados = Json.decodeFromString<List<SupervisorEquipo>>(contenidoJson)
            listaSupervisores.addAll(supervisoresGuardados)
        } catch (e: Exception) {
            println("Error al leer el archivo, es probable que el archivo supervisores.json este corrupto")
        }
    }
}

// ---------- Modelos para el reporte ----------
private class ResumenEquipo(
    val equipo: String,
    val supervisor: String,
    val integrantes: List<VentaIntegrante>
) {
    val total get() = integrantes.sumOf { it.ventasRealizadas }
    val promedio get() = if (integrantes.isEmpty()) 0.0 else total.toDouble() / integrantes.size
}

// ---------- Generador del reporte ----------
fun generarReportePdf(rutaPdf: String): Boolean {
    System.setProperty("java.awt.headless", "true") // necesario para dibujar sin ventana
    val json = Json { ignoreUnknownKeys = true }

    try {
        // 1. Leer los JSON
        val archivosEquipos = listOf("datosNorte.json", "datosCentro.json", "datosSur.json")
        val ventas = archivosEquipos.flatMap { nombre ->
            val f = File(nombre)
            if (!f.exists()) {
                println("No se encontró el archivo $nombre")
                return false
            }
            json.decodeFromString<List<VentaIntegrante>>(f.readText())
        }

        val fSup = File("supervisores.json")
        if (!fSup.exists()) {
            println("No se encontró supervisores.json")
            return false
        }
        val supervisores = json.decodeFromString<List<SupervisorEquipo>>(fSup.readText())

        // 2. Resumen por equipo
        val resumenes = listOf("Norte", "Centro", "Sur").map { eq ->
            ResumenEquipo(
                equipo = eq,
                supervisor = supervisores.find { it.equipo.equals(eq, true) }?.nombreSupervisor ?: "Sin asignar",
                integrantes = ventas.filter { it.equipo.equals(eq, true) }
            )
        }

        // 3. Gráfico de pastel (promedio de ventas diario por equipo)
        val dataset = DefaultPieDataset<String>()
        resumenes.forEach { dataset.setValue(it.equipo, it.promedio) }

        val grafico = ChartFactory.createPieChart(
            "Promedio de ventas diario por equipo", dataset, true, true, false
        )
        @Suppress("UNCHECKED_CAST")
        val plot = grafico.plot as PiePlot<String>
        plot.setSectionPaint("Norte", Color(52, 152, 219))
        plot.setSectionPaint("Centro", Color(46, 204, 113))
        plot.setSectionPaint("Sur", Color(231, 76, 60))
        plot.setLabelGenerator(
            StandardPieSectionLabelGenerator("{0}: {1} ({2})", DecimalFormat("0.00"), DecimalFormat("0%"))
        )
        plot.setBackgroundPaint(Color.WHITE)
        plot.setOutlineVisible(false)

        val baos = ByteArrayOutputStream()
        ImageIO.write(grafico.createBufferedImage(600, 400), "png", baos)
        val imagenGrafico = org.openpdf.text.Image.getInstance(baos.toByteArray()).apply {
            scaleToFit(450f, 300f)
            setAlignment(Element.ALIGN_CENTER)
        }

        // 4. Armar el PDF (Document local para poder generarlo varias veces)
        val doc = Document()
        try {
            PdfWriter.getInstance(doc, FileOutputStream(rutaPdf))
            doc.open()

            val fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20f, Color(33, 47, 61))
            val fSub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, Color(33, 47, 61))
            val fTexto = FontFactory.getFont(FontFactory.HELVETICA, 10f)
            val fHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, Color.WHITE)

            fun celda(texto: String, fuente: org.openpdf.text.Font, fondo: Color? = null): PdfPCell {
                val c = PdfPCell(Phrase(texto, fuente))
                c.setHorizontalAlignment(Element.ALIGN_CENTER)
                c.setVerticalAlignment(Element.ALIGN_MIDDLE)
                c.setPadding(6f)
                if (fondo != null) c.setBackgroundColor(fondo)
                return c
            }

            // Encabezado
            val titulo = Paragraph("Reporte de Promedios de Ventas", fTitulo)
            titulo.setAlignment(Element.ALIGN_CENTER)
            doc.add(titulo)

            val fecha = Paragraph("Fecha de generación: ${java.time.LocalDateTime.now().withNano(0)}", fTexto)
            fecha.setAlignment(Element.ALIGN_CENTER)
            fecha.setSpacingAfter(15f)
            doc.add(fecha)

            val intro = Paragraph("Resumen de las ventas realizadas por cada equipo y su supervisor a cargo.", fTexto)
            intro.setSpacingAfter(12f)
            doc.add(intro)

            // Tabla resumen
            val subResumen = Paragraph("Resumen por equipo", fSub)
            subResumen.setSpacingAfter(6f)
            doc.add(subResumen)

            val resumen = PdfPTable(5)
            resumen.setWidthPercentage(100f)
            resumen.setWidths(floatArrayOf(1.2f, 2f, 1.3f, 1.3f, 1.4f))
            resumen.setSpacingAfter(15f)

            val azul = Color(44, 62, 80)
            listOf("Equipo", "Supervisor", "Integrantes", "Total ventas", "Promedio").forEach {
                resumen.addCell(celda(it, fHeader, azul))
            }
            resumenes.forEachIndexed { i, r ->
                val fondo = if (i % 2 == 0) Color(236, 240, 241) else Color.WHITE
                resumen.addCell(celda(r.equipo, fTexto, fondo))
                resumen.addCell(celda(r.supervisor, fTexto, fondo))
                resumen.addCell(celda(r.integrantes.size.toString(), fTexto, fondo))
                resumen.addCell(celda(r.total.toString(), fTexto, fondo))
                resumen.addCell(celda("%.2f".format(r.promedio), fTexto, fondo))
            }
            doc.add(resumen)

            // Gráfico
            val subGrafico = Paragraph("Gráfico de promedios", fSub)
            subGrafico.setSpacingAfter(6f)
            doc.add(subGrafico)
            doc.add(imagenGrafico)

            // Detalle por equipo
            resumenes.forEach { r ->
                val subEquipo = Paragraph(Chunk("Equipo ${r.equipo} - Supervisor: ${r.supervisor}", fSub))
                subEquipo.setSpacingBefore(15f)
                subEquipo.setSpacingAfter(6f)
                doc.add(subEquipo)

                val detalle = PdfPTable(2)
                detalle.setWidthPercentage(60f)
                detalle.setHorizontalAlignment(Element.ALIGN_LEFT)
                detalle.addCell(celda("Integrante", fHeader, azul))
                detalle.addCell(celda("Ventas realizadas", fHeader, azul))
                r.integrantes.forEach {
                    detalle.addCell(celda(it.integrante, fTexto))
                    detalle.addCell(celda(it.ventasRealizadas.toString(), fTexto))
                }
                doc.add(detalle)
            }
        } finally {
            if (doc.isOpen) doc.close()
        }
        return true
    } catch (e: Exception) {
        println("Error al generar el PDF: ${e.message}")
        return false
    }
}

//Equipos que existen en la empresa
val equiposDisponibles = listOf("Norte", "Centro", "Sur")
 
//Guarda la lista actual de supervisores en supervisores.json
fun guardarSupervisores() {
    archivoSupervisores.writeText(
        archivojsonSupervisores.encodeToString<List<SupervisorEquipo>>(listaSupervisores)
    )
}
 
//Un equipo esta libre cuando ningun supervisor lo tiene asignado
fun equiposLibres(): List<String> =
    equiposDisponibles.filter { eq -> listaSupervisores.none { it.equipo.equals(eq, ignoreCase = true) } }
 
//Submenu de la opcion 4
fun gestionarSupervisores() {
    while (true) {
        println("\nGestion de supervisores")
        println("1. Agregar supervisor")
        println("2. Eliminar supervisor")
        println("3. Volver al menu principal")
        when (readln().trim()) {
            "1" -> agregarSupervisor()
            "2" -> eliminarSupervisor()
            "3" -> return
            else -> println("Opcion no valida.")
        }
    }
}
 
private fun agregarSupervisor() {
    val libres = equiposLibres()
    if (libres.isEmpty()) {
        println("Todos los equipos ya tienen supervisor. Elimina uno primero para liberar un equipo.")
        return
    }
 
    println("Equipos libres: ${libres.joinToString(", ")}")
    println("Digite el nombre del supervisor:")
    val nombre = readln().trim()
    println("Digite el correo del supervisor:")
    val gmail = readln().trim()
    println("Digite el numero de telefono del supervisor:")
    val telefono = readln().trim()
 
    if (nombre.isEmpty() || gmail.isEmpty() || telefono.isEmpty()) {
        println("Todos los campos son obligatorios. No se agrego el supervisor.")
        return
    }
 
    println("Digite el equipo que tendra a cargo (${libres.joinToString(", ")}):")
    val equipo = libres.find { it.equals(readln().trim(), ignoreCase = true) }
    if (equipo == null) {
        println("Equipo no valido o no disponible. No se agrego el supervisor.")
        return
    }
 
    //maxOf + 1 evita repetir ids despues de eliminar supervisores
    val nuevoId = (listaSupervisores.maxOfOrNull { it.id } ?: 0) + 1
    listaSupervisores.add(SupervisorEquipo(nuevoId, nombre, gmail, telefono, equipo))
    guardarSupervisores()
    println("Supervisor $nombre agregado al equipo $equipo (id $nuevoId).")
}
 
private fun eliminarSupervisor() {
    if (listaSupervisores.isEmpty()) {
        println("No hay supervisores registrados.")
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
        return
    }
 
    println("Seguro que desea eliminar a ${supervisor.nombreSupervisor}? (s/n)")
    if (!readln().trim().equals("s", ignoreCase = true)) {
        println("Operacion cancelada.")
        return
    }
 
    listaSupervisores.remove(supervisor)
    guardarSupervisores()
    println("Supervisor eliminado. El equipo ${supervisor.equipo} quedo libre para un nuevo supervisor.")
}

//Datos de supervisor en forma de ficha
private fun imprimirSupervisor(s: SupervisorEquipo) {
    println("-".repeat(36))
    println("  Id:        ${s.id}")
    println("  Nombre:    ${s.nombreSupervisor}")
    println("  Correo:    ${s.gmail}")
    println("  Telefono:  ${s.phoneNumber}")
    println("  Equipo:    ${s.equipo}")
    println("-".repeat(36))
}

//Historial de la conversacion actual con Krono (se borra cuando el usuario cierra el chat)
private val historialChat = mutableListOf<Pair<String, String>>()
private const val MAX_TURNOS_MEMORIA = 10
private const val MAX_CARACTERES_RESPUESTA = 800

//procesarPregunta() recibe un solo texto, asi que se le une la conversacion anterior a la nueva pregunta
private fun construirPrompt(preguntaNueva: String): String {
    if (historialChat.isEmpty()) return preguntaNueva
    val contexto = historialChat.takeLast(MAX_TURNOS_MEMORIA)
        .joinToString("\n") { (pregunta, respuesta) -> "Usuario: $pregunta\nKrono: $respuesta" }
    return "Conversacion anterior:\n$contexto\n\n" +
        "Responde a la nueva pregunta teniendo en cuenta la conversacion anterior.\n" +
        "Nueva pregunta del usuario: $preguntaNueva"
}

//Chat con Krono: sigue respondiendo y recordando la conversacion hasta que el usuario escriba "salir"
private fun chatConKrono(primerMensaje: String) {
    println("\nHola soy Krono")
    println("(Escribe \"salir\" cuando quieras cerrar el chat y volver al menu)")
    var texto = primerMensaje
    while (true) {
        if (texto.equals("salir", ignoreCase = true)) {
            historialChat.clear()
            println("\nKrono: Chat cerrado. Vuelvo al menu.\n")
            return
        }
        if (texto.isEmpty()) {
            println("\nKrono: Escribe tu pregunta o \"salir\" para cerrar el chat.")
        } else {
            try {
                val respuesta = gestorIA.procesarPregunta(construirPrompt(texto)).toString()
                println("\nKrono:")
                println(respuesta)
                historialChat.add(texto to respuesta.take(MAX_CARACTERES_RESPUESTA))
            } catch (e: Exception) {
                println("\nKrono: No pude responder en este momento (${e.message}). Intenta de nuevo.")
            }
        }
        print("\nTu: ")
        texto = readlnOrNull()?.trim() ?: return
    }
}

//Preguntas estaticas y dinamicas del Asistente
fun preguntasEstaticas() {
    println("Que te gustaria hacer hoy? (Escoge el numero de la opcion que deseas realizar, sino puedes preguntarle lo que gustes al asistente IA)")
    println("\n 1. Consultar datos de los supervisores de ventas")
    println("\n 2. Buscar supervisor de venta")
    println("\n 3. Generar un reporte pdf de los supervisores de ventas con sus promedios de ventas")
    println("\n 4. Quitar o agregar algun supervisor de ventas")
    println("\n 5. Cerrar el programa")

    //Respuestas que dara el usuario a las preguntas estaticas
    val entrada = readln().trim()
    val opcion = entrada.toIntOrNull()

    //Si el usuario no digita un numero, ese texto es su primera pregunta y empieza el chat con Krono
    if (opcion == null) {
        chatConKrono(entrada)
        return preguntasEstaticas()
    }

    when (opcion) {
        1 -> {
            println("\n===== Supervisores de ventas =====")
            if (listaSupervisores.isEmpty()) {
                println("No hay supervisores registrados.")
            } else {
                println("%-4s %-14s %-28s %-10s %-8s".format("ID", "Nombre", "Correo", "Telefono", "Equipo"))
                println("=".repeat(68))
                listaSupervisores.forEach {
                    println("%-4s %-14s %-28s %-10s %-8s".format(it.id, it.nombreSupervisor, it.gmail, it.phoneNumber, it.equipo))
                }
            }
            val libres = equiposLibres()
            if (libres.isNotEmpty()) {
                println("\nEquipos sin supervisor: ${libres.joinToString(", ")}")
            }
            println()
            return preguntasEstaticas()
        }
        2 -> {
            println("Digite el nombre, id, correo o numero de telefono del supervisor que desea encontrar")
            val busqueda = readln().trim()
            //filter (en vez de find) para mostrar todas las coincidencias, ej. dos supervisores con el mismo telefono
            val encontrados = listaSupervisores.filter {
                it.nombreSupervisor.equals(busqueda, ignoreCase = true) ||
                    it.id.toString() == busqueda ||
                    it.phoneNumber == busqueda ||
                    it.gmail.equals(busqueda, ignoreCase = true)
            }
            if (encontrados.isEmpty()) {
                println("\nNo se encontro ningun supervisor con \"$busqueda\".")
            } else {
                println("\nSe encontro ${encontrados.size} supervisor(es):")
                encontrados.forEach { imprimirSupervisor(it) }
            }
            println()
            return preguntasEstaticas()
        }
        3 -> {
            println("Generando $reportePdf ...")
            if (generarReportePdf(reportePdf)) {
                println("Reporte generado con exito en: ${File(reportePdf).absolutePath}")
            }
            return preguntasEstaticas()
        }
        4 -> {
            gestionarSupervisores()
            return preguntasEstaticas()
        }
        5 -> {
            return println("Gracias por haber usado Krono, tu asistente Empresarial favorito, te esperamos pronto!")
        }
        else -> {
            println("\nOpcion no valida. Escoge un numero del 1 al 5 o escribe tu pregunta.\n")
            return preguntasEstaticas()
        }
    }
}

fun AsistenteIA() {
    datosSupervisores()
    //Preguntas estaticas
    println("Bienvenido de nuevo, $emaill")
    preguntasEstaticas()

    //Preguntas dinamicas
    //En esta parte se aplicaran las IAs como Gemini y Grok pero sera para casos especificos
    //como por ejemplo una pregunta que el usuario realice y que no este disponible en el menu,
    //Generar reportes o graficos.

}
