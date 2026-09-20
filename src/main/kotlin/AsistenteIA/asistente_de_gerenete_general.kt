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