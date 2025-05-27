package com.example.hexenapp.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hexenapp.data.repository.ResultWrapper
import com.example.hexenapp.ui.navigation.AppRoutes
import com.example.hexenapp.ui.theme.HexenAppTheme
import com.example.hexenapp.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordConfirmScreen(
    navController: NavController,
    oobCode: String?, // oobCode (action code) dari deep link
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmNewPasswordVisible by remember { mutableStateOf(false) }
    var userEmailForDisplay by remember { mutableStateOf<String?>(null) }
    var isCodeVerified by remember { mutableStateOf(false) }
    var verificationError by remember { mutableStateOf<String?>(null) }


    val verifyCodeResult by authViewModel.verifyCodeResult.collectAsState()
    val confirmResetResult by authViewModel.confirmResetResult.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()

    // Verifikasi kode saat layar pertama kali dimuat jika oobCode ada
    LaunchedEffect(key1 = oobCode) {
        if (oobCode != null) {
            authViewModel.verifyPasswordResetCode(oobCode)
        } else {
            verificationError = "Kode reset password tidak valid atau hilang."
            // Mungkin navigasi kembali atau tampilkan error permanen
        }
    }

    // Tangani hasil verifikasi kode
    LaunchedEffect(verifyCodeResult) {
        when (val result = verifyCodeResult) {
            is ResultWrapper.Success -> {
                userEmailForDisplay = result.value // Simpan email untuk ditampilkan
                isCodeVerified = true
                verificationError = null
                Toast.makeText(context, "Kode terverifikasi untuk: ${result.value}", Toast.LENGTH_SHORT).show()
                authViewModel.clearVerifyCodeResult()
            }
            is ResultWrapper.Error -> {
                verificationError = "Gagal memverifikasi kode: ${result.exception.message}"
                isCodeVerified = false
                // Toast.makeText(context, "Gagal memverifikasi kode: ${result.exception.message}", Toast.LENGTH_LONG).show()
                authViewModel.clearVerifyCodeResult()
            }
            is ResultWrapper.Loading -> { /* Ditangani oleh isLoading */ }
            null -> { /* Initial state */ }
        }
    }

    // Tangani hasil konfirmasi reset password
    LaunchedEffect(confirmResetResult) {
        when (val result = confirmResetResult) {
            is ResultWrapper.Success -> {
                Toast.makeText(context, "Password berhasil direset! Silakan login dengan password baru Anda.", Toast.LENGTH_LONG).show()
                authViewModel.clearConfirmResetResult()
                // Navigasi ke layar login dan bersihkan backstack hingga ke sana
                navController.navigate(AppRoutes.LOGIN) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true } // Bersihkan semua backstack
                    launchSingleTop = true
                }
            }
            is ResultWrapper.Error -> {
                Toast.makeText(context, "Gagal mereset password: ${result.exception.message}", Toast.LENGTH_LONG).show()
                authViewModel.clearConfirmResetResult()
            }
            is ResultWrapper.Loading -> { /* Ditangani oleh isLoading */ }
            null -> { /* Initial state */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atur Password Baru") },
                navigationIcon = {
                    // Tombol kembali bisa mengarah ke Login jika pengguna ingin batal
                    IconButton(onClick = {
                        navController.navigate(AppRoutes.LOGIN) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke Login")
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
            if (oobCode == null || verificationError != null && !isCodeVerified) {
                Text(
                    text = verificationError ?: "Link reset password tidak valid atau sudah kedaluwarsa.",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }) {
                    Text("Kembali ke Login")
                }
            } else if (!isCodeVerified && isLoading) {
                CircularProgressIndicator()
                Text("Memverifikasi link...", modifier = Modifier.padding(top = 16.dp))
            } else if (isCodeVerified) {
                Text(
                    text = "Atur Password Baru Anda",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                userEmailForDisplay?.let {
                    Text(
                        text = "Untuk akun: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Password Baru") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password Baru Icon") },
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }),
                    trailingIcon = {
                        val image = if (newPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(imageVector = image, if (newPasswordVisible) "Sembunyikan" else "Tampilkan")
                        }
                    },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it },
                    label = { Text("Konfirmasi Password Baru") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Konfirmasi Password Icon") },
                    visualTransformation = if (confirmNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        if (newPassword.isNotBlank() && confirmNewPassword.isNotBlank()) {
                            if (newPassword == confirmNewPassword) {
                                authViewModel.confirmPasswordReset(oobCode, newPassword)
                            } else {
                                Toast.makeText(context, "Password baru tidak cocok!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        }
                    }),
                    trailingIcon = {
                        val image = if (confirmNewPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        IconButton(onClick = { confirmNewPasswordVisible = !confirmNewPasswordVisible }) {
                            Icon(imageVector = image, if (confirmNewPasswordVisible) "Sembunyikan" else "Tampilkan")
                        }
                    },
                    singleLine = true,
                    isError = newPassword.isNotEmpty() && confirmNewPassword.isNotEmpty() && newPassword != confirmNewPassword
                )
                if (newPassword.isNotEmpty() && confirmNewPassword.isNotEmpty() && newPassword != confirmNewPassword) {
                    Text("Password tidak cocok", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }


                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (newPassword.isNotBlank() && confirmNewPassword.isNotBlank()) {
                                if (newPassword == confirmNewPassword) {
                                    authViewModel.confirmPasswordReset(oobCode, newPassword)
                                } else {
                                    Toast.makeText(context, "Password baru tidak cocok!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = oobCode != null && isCodeVerified // Tombol hanya aktif jika kode valid dan terverifikasi
                    ) {
                        Text("Reset Password")
                    }
                }
            } else {
                // State default jika oobCode ada tapi belum diverifikasi dan tidak loading
                Text("Memproses permintaan...", modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun ResetPasswordConfirmScreenPreview() {
    HexenAppTheme {
        ResetPasswordConfirmScreen(navController = NavController(LocalContext.current), oobCode = "sampleoobcode")
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun ResetPasswordConfirmScreenInvalidCodePreview() {
    HexenAppTheme {
        ResetPasswordConfirmScreen(navController = NavController(LocalContext.current), oobCode = null)
    }
}
