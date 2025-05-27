package com.example.hexenapp.ui.screens.auth

import android.app.Application // Sebenarnya tidak diperlukan lagi jika factory dihapus
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// import androidx.lifecycle.viewmodel.compose.viewModel // Tidak digunakan lagi dengan factory
import androidx.hilt.navigation.compose.hiltViewModel // Import hiltViewModel
import androidx.navigation.NavController
import com.example.hexenapp.data.repository.ResultWrapper
import com.example.hexenapp.ui.navigation.AppRoutes
import com.example.hexenapp.ui.theme.HexenAppTheme
import com.example.hexenapp.ui.viewmodel.AuthViewModel
// AuthViewModelFactory tidak lagi diperlukan dan seharusnya dihapus
// import com.example.hexenapp.ui.viewmodel.AuthViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    // Gunakan hiltViewModel() untuk mendapatkan instance AuthViewModel
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val authResult by authViewModel.authResult.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is ResultWrapper.Success -> {
                Toast.makeText(context, "Registrasi berhasil!", Toast.LENGTH_SHORT).show()
                navController.navigate(AppRoutes.ITEM_LIST) {
                    popUpTo(AppRoutes.REGISTER) { inclusive = true }
                    launchSingleTop = true
                }
                authViewModel.clearAuthResult()
            }
            is ResultWrapper.Error -> {
                Toast.makeText(context, "Registrasi gagal: ${result.exception.message}", Toast.LENGTH_LONG).show()
                authViewModel.clearAuthResult()
            }
            is ResultWrapper.Loading -> { /* Ditangani oleh isLoading */ }
            null -> { /* Initial state */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buat Akun Baru") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Selamat Datang di HexenApp!",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Isi detail berikut untuk membuat akun.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email Icon") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password Icon") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(FocusDirection.Down)
                }),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, if (passwordVisible) "Sembunyikan" else "Tampilkan")
                    }
                },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Konfirmasi Password") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password Icon") },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()) {
                        if (password == confirmPassword) {
                            authViewModel.registerUser(email, password)
                        } else {
                            Toast.makeText(context, "Password tidak cocok!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                    }
                }),
                trailingIcon = {
                    val image = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = image, if (confirmPasswordVisible) "Sembunyikan" else "Tampilkan")
                    }
                },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()) {
                            if (password == confirmPassword) {
                                authViewModel.registerUser(email, password)
                            } else {
                                Toast.makeText(context, "Password tidak cocok!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Daftar")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sudah punya akun? ")
                TextButton(onClick = { navController.navigate(AppRoutes.LOGIN) {
                    popUpTo(AppRoutes.REGISTER) { inclusive = true }
                } }) {
                    Text("Login di sini")
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun RegisterScreenPreview() {
    HexenAppTheme {
        RegisterScreen(navController = NavController(LocalContext.current))
    }
}
