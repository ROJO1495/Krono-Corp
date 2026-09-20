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