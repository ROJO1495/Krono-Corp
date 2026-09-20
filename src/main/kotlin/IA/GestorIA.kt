package IA

class GestorIA {

    private val groqService = GroqService()

    fun procesarPregunta(pregunta: String): String {

        return groqService.enviarPregunta(pregunta)

    }
}