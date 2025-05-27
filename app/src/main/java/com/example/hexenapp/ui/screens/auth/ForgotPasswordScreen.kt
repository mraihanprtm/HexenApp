package com.example.hexenapp.ui.screens.auth

import android.widget.Toast // Pastikan import ini ada dan benar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hexenapp.data.repository.ResultWrapper
import com.example.hexenapp.ui.theme.HexenAppTheme
import com.example.hexenapp.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }

    val resetPasswordResult by authViewModel.resetPasswordResult.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()

    LaunchedEffect(resetPasswordResult) {
        when (val result = resetPasswordResult) {
            is ResultWrapper.Success -> {
                // Menggunakan Toast.LENGTH_LONG dengan benar
                Toast.makeText(context, "Email reset password telah dikirim ke $email. Silakan periksa kotak masuk Anda.", Toast.LENGTH_LONG).show()
                authViewModel.clearResetPasswordResult()
                navController.popBackStack()
            }
            is ResultWrapper.Error -> {
                // Menggunakan Toast.LENGTH_LONG dengan benar
                Toast.makeText(context, "Gagal mengirim email: ${result.exception.message}", Toast.LENGTH_LONG).show()
                authViewModel.clearResetPasswordResult()
            }
            is ResultWrapper.Loading -> { /* Ditangani oleh isLoading state */ }
            null -> { /* Initial state */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lupa Password") },
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
                text = "Reset Password Akun Anda",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Masukkan alamat email yang terdaftar untuk menerima link reset password.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Terdaftar") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email Icon") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (email.isNotBlank()) {
                        authViewModel.sendPasswordResetEmail(email)
                    } else {
                        Toast.makeText(context, "Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    }
                }),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (email.isNotBlank()) {
                            authViewModel.sendPasswordResetEmail(email)
                        } else {
                            Toast.makeText(context, "Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Kirim Email Reset")
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun ForgotPasswordScreenPreview() {
    HexenAppTheme {
        ForgotPasswordScreen(navController = NavController(LocalContext.current))
    }
}
