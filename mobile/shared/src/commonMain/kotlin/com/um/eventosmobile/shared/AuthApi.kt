package com.um.eventosmobile.shared

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json

class AuthApi(private val backendUrl: String) {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 10000
            requestTimeoutMillis = 30000
        }
    }

    suspend fun login(username: String, password: String): String {
        val resp: AuthResponseDto = client.post("$backendUrl/api/authenticate") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequestDto(username = username, password = password, rememberMe = true))
        }.body()

        return resp.id_token
    }

    suspend fun register(login: String, email: String, password: String, langKey: String = "es") {
        // El endpoint retorna 201 CREATED sin body
        // Consumimos la respuesta como Unit para cerrar la conexión correctamente
        // Si hay error (400, etc.), Ktor lanzará excepción automáticamente
        client.post("$backendUrl/api/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequestDto(login = login, email = email, password = password, langKey = langKey))
        }.body<Unit>()
    }
}

