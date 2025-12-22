package com.um.eventosmobile.shared

import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
    }

    suspend fun login(username: String, password: String): String {
        val resp: AuthResponseDto = client.post("$backendUrl/api/authenticate") {
            contentType(ContentType.Application.Json)
            setBody(AuthRequestDto(username = username, password = password, rememberMe = true))
        }.body()

        return resp.id_token
    }
}

