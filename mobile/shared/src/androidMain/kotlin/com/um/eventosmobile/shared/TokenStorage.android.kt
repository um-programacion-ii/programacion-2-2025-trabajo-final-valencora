package com.um.eventosmobile.shared

import android.content.Context

/**
 * Implementación Android de TokenStorage que requiere Context.
 * Se crea desde la app y se pasa al módulo compartido.
 * 
 * Esta clase implementa la funcionalidad de TokenStorage para Android
 * usando almacenamiento en memoria (equivalente a sessionStorage).
 * El token se pierde al cerrar la aplicación.
 */
class TokenStorageAndroid(private val context: Context) : TokenStorage {
    // Almacenamiento en memoria (no persistente)
    private var tokenInMemory: String? = null
    
    override suspend fun saveToken(token: String) {
        tokenInMemory = token
    }
    
    override suspend fun getToken(): String? {
        return tokenInMemory
    }
    
    override suspend fun clearToken() {
        tokenInMemory = null
    }
}

