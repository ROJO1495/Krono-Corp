package IA

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Serializable
data class GroqResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

class GroqService {

    private val apiKey = System.getenv("GROQ_API_KEY")

    private val client = HttpClient.newHttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun enviarPregunta(pregunta: String): String {

        if (apiKey.isNullOrBlank()) {
            return "Error: no se encontró la variable GROQ_API_KEY."
        }

        val jsonBody = """
            {
                "model": "openai/gpt-oss-120b",
                "messages": [
                    {
                        "role": "system",
                        "content": "Eres Krono, el asistente de inteligencia artificial de KronoCorp. Responde de forma clara, profesional y útil."
                    },
                    {
                        "role": "user",
                        "content": ${json.encodeToString(String.serializer(), pregunta)}
                    }
                ]
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "https://api.groq.com/openai/v1/chat/completions"
                )
            )
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .POST(
                HttpRequest.BodyPublishers.ofString(jsonBody, Charsets.UTF_8)
            )
            .build()

        return try {

            val response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(Charsets.UTF_8)
            )

            if (response.statusCode() !in 200..299) {
                return "Error de Groq. Código HTTP: ${response.statusCode()}"
            }

            val respuestaGroq = json.decodeFromString<GroqResponse>(
                response.body()
            )

            if (respuestaGroq.choices.isEmpty()) {
                return "Krono no recibió una respuesta válida."
            }

            respuestaGroq.choices[0].message.content

        } catch (e: Exception) {

            "Error al comunicarse con Groq: ${e.message}"
        }
    }
}
