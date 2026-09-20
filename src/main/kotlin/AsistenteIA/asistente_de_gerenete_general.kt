package AsistenteIA

import Login.emaill
import Login.listaUsuarios

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

