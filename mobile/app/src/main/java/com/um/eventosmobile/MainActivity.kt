package com.um.eventosmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.um.eventosmobile.shared.AuthApi
import com.um.eventosmobile.shared.MobileApi
import com.um.eventosmobile.shared.TokenStorageAndroid
import com.um.eventosmobile.shared.SaleResponseDto
import com.um.eventosmobile.ui.*
import com.um.eventosmobile.ui.theme.EventosMobileTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // URLs según el entorno
        // Para emulador Android: http://10.0.2.2:8080 (backend) y http://10.0.2.2:8081 (proxy)
        // Para dispositivo físico conectado por USB con adb reverse: 
        //   - http://localhost:8080 (backend para login y eventos/sesión)
        //   - http://localhost:8081 (proxy para mapa de asientos, bloquear y ventas)
        // IMPORTANTE: Ejecutar "adb reverse tcp:8080 tcp:8080" y "adb reverse tcp:8081 tcp:8081" antes de usar la app
        
        // Backend: usado para autenticación (login) y operaciones de eventos/sesión
        val backendUrl = "http://localhost:8080"
        
        // Proxy: usado para mapa de asientos, bloquear asientos y confirmar venta
        val proxyUrl = "http://localhost:8081"
        
        val authApi = AuthApi(backendUrl)
        val tokenStorage = TokenStorageAndroid(this)
        
        setContent {
            EventosMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var token by remember { mutableStateOf<String?>(null) }
                    var isLoadingToken by remember { mutableStateOf(true) }
                    val scope = rememberCoroutineScope()
                    
                    // Cargar token guardado al iniciar
                    LaunchedEffect(Unit) {
                        token = tokenStorage.getToken()
                        isLoadingToken = false
                    }
                    
                    // MobileApi que usa el token actual - usa backend para eventos/sesión y proxy para asientos/ventas
                    val mobileApi = remember(token) {
                        MobileApi(
                            backendUrl = backendUrl,
                            proxyUrl = proxyUrl,
                            tokenProvider = { token }
                        )
                    }
                    
                    if (isLoadingToken) {
                        // Mostrar indicador de carga mientras se verifica el token
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (token == null) {
                        // Mostrar pantalla de login si no hay token
                        LoginScreen(
                            authApi = authApi,
                            onLoginSuccess = { newToken ->
                                scope.launch {
                                    tokenStorage.saveToken(newToken)
                                    token = newToken
                                }
                            }
                        )
                    } else {
                        // Navegación entre pantallas
                        var currentScreen by remember { mutableStateOf<Screen>(Screen.EventList) }
                        var seatMapRefreshKey by remember { mutableStateOf(0) }
                        
                        when (val screen = currentScreen) {
                            is Screen.EventList -> {
                                EventListScreen(
                                    api = mobileApi,
                                    onEventClick = { eventId ->
                                        currentScreen = Screen.EventDetail(eventId)
                                    },
                                    onLogout = {
                                        scope.launch {
                                            tokenStorage.clearToken()
                                            token = null
                                        }
                                    }
                                )
                            }
                            is Screen.EventDetail -> {
                                EventDetailScreen(
                                    api = mobileApi,
                                    eventId = screen.eventId,
                                    onBack = { currentScreen = Screen.EventList },
                                    onViewSeats = { eventId ->
                                        currentScreen = Screen.SeatSelection(eventId, seatMapRefreshKey)
                                    },
                                    onResumeSelection = { eventId, seats, expiresAt ->
                                        currentScreen = Screen.Names(
                                            eventId = eventId,
                                            seats = seats,
                                            expiresAt = expiresAt
                                        )
                                    }
                                )
                            }
                            is Screen.SeatSelection -> {
                                SeatSelectionScreen(
                                    api = mobileApi,
                                    eventId = screen.eventId,
                                    refreshKey = screen.refreshKey,
                                    onBack = { currentScreen = Screen.EventDetail(screen.eventId) },
                                    onContinue = { eventId, seats, expiresAt ->
                                        currentScreen = Screen.Names(
                                            eventId = eventId,
                                            seats = seats,
                                            expiresAt = expiresAt
                                        )
                                    }
                                )
                            }
                            is Screen.Names -> {
                                NamesScreen(
                                    api = mobileApi,
                                    eventId = screen.eventId,
                                    seats = screen.seats,
                                    expiresAt = screen.expiresAt,
                                    onBack = { 
                                        // Incrementar refreshKey para forzar recreación del ViewModel y limpiar selección local
                                        seatMapRefreshKey++
                                        currentScreen = Screen.SeatSelection(screen.eventId, seatMapRefreshKey)
                                    },
                                    onExpired = {
                                        seatMapRefreshKey++
                                        currentScreen = Screen.EventDetail(screen.eventId)
                                    },
                                    onConfirm = { seatsWithPeople ->
                                        currentScreen = Screen.SaleConfirmation(
                                            eventId = screen.eventId,
                                            seatsWithPeople = seatsWithPeople
                                        )
                                    }
                                )
                            }
                            is Screen.SaleConfirmation -> {
                                SaleConfirmationScreen(
                                    api = mobileApi,
                                    eventId = screen.eventId,
                                    seatsWithPeople = screen.seatsWithPeople,
                                    onBack = { 
                                        currentScreen = Screen.Names(
                                            eventId = screen.eventId,
                                            seats = screen.seatsWithPeople.map { 
                                                it.first to it.second 
                                            },
                                            expiresAt = null
                                        )
                                    },
                                    onFinish = { response ->
                                        currentScreen = Screen.SaleResult(
                                            success = response.resultado == "EXITOSA",
                                            message = response.mensaje ?: (response.resultado ?: "Error desconocido")
                                        )
                                        seatMapRefreshKey++
                                    }
                                )
                            }
                            is Screen.SaleResult -> {
                                SaleResultScreen(
                                    success = screen.success,
                                    message = screen.message,
                                    onFinish = { 
                                        currentScreen = Screen.EventList
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sealed class para navegación
sealed class Screen {
    object EventList : Screen()
    data class EventDetail(val eventId: Long) : Screen()
    data class SeatSelection(val eventId: Long, val refreshKey: Int) : Screen()
    data class Names(
        val eventId: Long,
        val seats: List<Pair<String, Int>>,
        val expiresAt: String?
    ) : Screen()
    data class SaleConfirmation(
        val eventId: Long,
        val seatsWithPeople: List<Triple<String, Int, Pair<String, String>>>
    ) : Screen()
    data class SaleResult(
        val success: Boolean,
        val message: String
    ) : Screen()
}
