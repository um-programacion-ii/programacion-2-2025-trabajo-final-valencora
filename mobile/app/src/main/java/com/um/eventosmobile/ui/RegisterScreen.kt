package com.um.eventosmobile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.um.eventosmobile.shared.AuthApi
import com.um.eventosmobile.model.RegisterEffect
import com.um.eventosmobile.viewmodel.RegisterViewModel
import com.um.eventosmobile.viewmodel.RegisterViewModelFactory
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authApi: AuthApi,
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = RegisterViewModelFactory(authApi))
) {
    val uiState by viewModel.uiState.collectAsState()

    // Resetear el estado cuando se navega a esta pantalla
    LaunchedEffect(Unit) {
        viewModel.resetSuccess()
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is RegisterEffect.RegisterSuccess -> {
                    onRegisterSuccess()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrarse") },
                navigationIcon = {
                    TextButton(onClick = {
                        viewModel.resetSuccess()
                        onBackToLogin()
                    }) {
                        Text("← Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Eventos Mobile",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (uiState.success) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "¡Registro exitoso!",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Tu cuenta ha sido creada. Ya puedes iniciar sesión.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = {
                                viewModel.resetSuccess()
                                onBackToLogin()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Volver al login")
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = uiState.login,
                    onValueChange = { viewModel.updateLogin(it) },
                    label = { Text("Usuario") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    enabled = !uiState.isLoading,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.updateEmail(it) },
                    label = { Text("Correo electrónico") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    enabled = !uiState.isLoading,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.updatePassword(it) },
                    label = { Text("Contraseña") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !uiState.isLoading,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                uiState.error?.let {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Button(
                    onClick = { viewModel.register() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !uiState.isLoading && uiState.login.isNotBlank() && uiState.email.isNotBlank() && 
                             uiState.password.isNotBlank(),
                    shape = MaterialTheme.shapes.large,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            "Registrarse",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        viewModel.resetSuccess()
                        onBackToLogin()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("¿Ya tienes una cuenta? Iniciar sesión")
                }
            }
        }
    }
}

