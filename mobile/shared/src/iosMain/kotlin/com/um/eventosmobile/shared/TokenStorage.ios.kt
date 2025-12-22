package com.um.eventosmobile.shared

import platform.Foundation.NSUserDefaults

/**
 * Implementación iOS de TokenStorage usando NSUserDefaults.
 */
class TokenStorageIOS : TokenStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val tokenKey = "jwt_token"
    
    override suspend fun saveToken(token: String) {
        userDefaults.setObject(token, tokenKey)
        userDefaults.synchronize()
    }
    
    override suspend fun getToken(): String? {
        return userDefaults.stringForKey(tokenKey)
    }
    
    override suspend fun clearToken() {
        userDefaults.removeObjectForKey(tokenKey)
        userDefaults.synchronize()
    }
}

