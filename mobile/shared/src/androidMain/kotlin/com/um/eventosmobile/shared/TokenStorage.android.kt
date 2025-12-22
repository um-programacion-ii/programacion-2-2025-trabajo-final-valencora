package com.um.eventosmobile.shared

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementación Android de TokenStorage que requiere Context.
 * Se crea desde la app y se pasa al módulo compartido.
 * 
 * Esta clase implementa la funcionalidad de TokenStorage para Android
 * usando EncryptedSharedPreferences para almacenamiento seguro.
 */
class TokenStorageAndroid(private val context: Context) : TokenStorage {
    private val prefs: SharedPreferences by lazy {
        try {
            // Intentar usar EncryptedSharedPreferences para seguridad
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback a SharedPreferences normal si falla
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    override suspend fun saveToken(token: String) {
        withContext(Dispatchers.IO) {
            prefs.edit().putString(TOKEN_KEY, token).apply()
        }
    }
    
    override suspend fun getToken(): String? {
        return withContext(Dispatchers.IO) {
            prefs.getString(TOKEN_KEY, null)
        }
    }
    
    override suspend fun clearToken() {
        withContext(Dispatchers.IO) {
            prefs.edit().remove(TOKEN_KEY).apply()
        }
    }
    
    companion object {
        private const val TOKEN_KEY = "jwt_token"
        private const val PREFS_NAME = "eventos_mobile_prefs"
    }
}

