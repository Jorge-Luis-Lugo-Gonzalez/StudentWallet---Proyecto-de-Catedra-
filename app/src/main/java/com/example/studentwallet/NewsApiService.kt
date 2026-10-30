package com.example.studentwallet

import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object NewsApiService {

    private const val ENDPOINT = "https://jsonplaceholder.typicode.com/posts?_limit=6"

    fun fetchAcademicAndFinancialNews(forceError: Boolean = false): Result<List<NewsArticle>> {
        if (forceError) {
            return Result.failure(Exception("Sin conexión al servidor"))
        }

        return try {
            val url = URL(ENDPOINT)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }

            val code = connection.responseCode
            if (code in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                connection.disconnect()

                val jsonArray = JSONArray(response.toString())
                val articles = mapApiArticles(jsonArray)
                Result.success(articles)
            } else {
                connection.disconnect()
                Result.failure(Exception("Error HTTP $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapApiArticles(jsonArray: JSONArray): List<NewsArticle> {
        val templates = listOf(
            NewsArticle(
                1,
                "Calendario Académico UDB: Fechas de evaluaciones y matrícula",
                "Consulta la programación oficial del ciclo, periodos de pago de cuotas sin recargo y fechas clave de laboratorios.",
                "14 Ago 2026",
                "Calendario Institucional",
                "https://www.udb.edu.sv/udb/"
            ),
            NewsArticle(
                2,
                "5 consejos de ahorro para estudiantes universitarios",
                "Aprende a aplicar la regla 50/30/20 con tu mesada o beca para cubrir alimentación, transporte y materiales sin endeudarte.",
                "12 Ago 2026",
                "Educación Financiera",
                "https://www.udb.edu.sv/udb/"
            ),
            NewsArticle(
                3,
                "Convocatoria de becas y apoyo financiero estudiantil",
                "La Dirección de Proyección Social abre el proceso de renovación y solicitud de becas académicas para el próximo ciclo.",
                "10 Ago 2026",
                "Becas y Finanzas",
                "https://www.udb.edu.sv/udb/"
            ),
            NewsArticle(
                4,
                "Cómo evitar gastos hormiga en el campus universitario",
                "Pequeñas compras diarias en cafetería y transporte pueden representar hasta el 25% de tu presupuesto mensual.",
                "08 Ago 2026",
                "Consejos de Ahorro",
                "https://www.bbva.com/es/salud-financiera/que-son-los-gastos-hormiga-y-como-evitarlos/"
            ),
            NewsArticle(
                5,
                "Feria de Innovación y Emprendimiento Tecnológico DSM",
                "Inscribe tu proyecto de cátedra de Desarrollo de Software para Móviles y participa en las charlas con especialistas.",
                "05 Ago 2026",
                "Vida Académica",
                "https://www.udb.edu.sv/udb/"
            ),
            NewsArticle(
                6,
                "Descuentos con carnet estudiantil en transporte y librerías",
                "Conoce los comercios afiliados donde puedes presentar tu carnet universitario vigente para ahorrar en tus compras.",
                "01 Ago 2026",
                "Beneficios Estudiantiles",
                "https://www.udb.edu.sv/udb/"
            )
        )

        val result = mutableListOf<NewsArticle>()
        val count = minOf(jsonArray.length(), templates.size)
        for (i in 0 until count) {
            val obj = jsonArray.getJSONObject(i)
            val apiId = obj.optInt("id", i + 1)
            val item = templates[i].copy(id = apiId)
            result.add(item)
        }
        return if (result.isEmpty()) templates else result
    }
}
