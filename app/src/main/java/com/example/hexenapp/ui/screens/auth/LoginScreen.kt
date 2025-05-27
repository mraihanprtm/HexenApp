package com.example.hexenapp.ui.screens.auth

import android.app.Activity
// import android.app.Application // Tidak lagi diperlukan untuk factory di sini
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel // Pastikan import ini ada
import androidx.navigation.NavController
import com.example.hexenapp.R
import com.example.hexenapp.data.repository.ResultWrapper
import com.example.hexenapp.ui.navigation.AppRoutes // Pastikan AppRoutes diimport
import com.example.hexenapp.ui.theme.HexenAppTheme
import com.example.hexenapp.ui.viewmodel.AuthViewModel
// import com.example.hexenapp.ui.viewmodel.AuthViewModelFactory // Tidak lagi diperlukan
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authResult by authViewModel.authResult.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val webClientId = "631170916854-2odhfng0p2t9gfiu2s79hojrd8hm7hrc.apps.googleusercontent.com" // Pastikan ini benar

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val credential = Identity.getSignInClient(context)
                    .getSignInCredentialFromIntent(result.data)
                val googleIdToken = credential.googleIdToken
                if (googleIdToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    authViewModel.signInWithGoogle(firebaseCredential)
                } else {
                    Toast.makeText(context, "Google Sign-In gagal: Token tidak ditemukan", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Google Sign-In gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Google Sign-In dibatalkan atau gagal. Kode: ${result.resultCode}", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is ResultWrapper.Success -> {
                navController.navigate(AppRoutes.ITEM_LIST) {
                    popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
                authViewModel.clearAuthResult()
            }
            is ResultWrapper.Error -> {
                Toast.makeText(context, "Login gagal: ${result.exception.message}", Toast.LENGTH_LONG).show()
                authViewModel.clearAuthResult()
            }
            else -> Unit // Loading atau null
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null && navController.currentDestination?.route != AppRoutes.ITEM_LIST) {
            navController.navigate(AppRoutes.ITEM_LIST) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ... (Image, Text, Email Field, Password Field tetap sama) ...
            Image(painter = painterResource(id = R.drawable.ic_launcher_foreground), contentDescription = "Logo Aplikasi", modifier = Modifier.size(120.dp).padding(bottom = 24.dp), contentScale = ContentScale.Fit)
            Text(text = "Selamat Datang Kembali!", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
            Text(text = "Silakan masuk ke akun Anda", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 24.dp), textAlign = TextAlign.Center)
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email Icon") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password Icon") }, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (email.isNotBlank() && password.isNotBlank()) authViewModel.loginUser(email, password)
                else Toast.makeText(context, "Email dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }), trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(imageVector = image, if (passwordVisible) "Sembunyikan" else "Tampilkan") }
            }, singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    // Navigasi ke layar lupa password
                    navController.navigate(AppRoutes.FORGOT_PASSWORD) // DIUBAH DI SINI
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Lupa Password?")
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ... (Tombol Login, Tombol Google Sign-In, Teks "Belum punya akun?" tetap sama) ...
            if (isLoading) { CircularProgressIndicator() } else { Button(onClick = {
                focusManager.clearFocus()
                if (email.isNotBlank() && password.isNotBlank()) authViewModel.loginUser(email, password)
                else Toast.makeText(context, "Email dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Login") } }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = {
                coroutineScope.launch {
                    try {
                        val beginSignInRequest = com.google.android.gms.auth.api.identity.BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions.builder().setSupported(true).setServerClientId(webClientId).setFilterByAuthorizedAccounts(false).build()).setAutoSelectEnabled(false).build()
                        val signInIntentSenderResult = Identity.getSignInClient(context).beginSignIn(beginSignInRequest).await()
                        googleSignInLauncher.launch(IntentSenderRequest.Builder(signInIntentSenderResult.pendingIntent.intentSender).build())
                    } catch (e: Exception) { Toast.makeText(context, "Gagal memulai Google Sign-In: ${e.localizedMessage}", Toast.LENGTH_LONG).show() }
                }
            }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Masuk dengan Google", modifier = Modifier.padding(start = 8.dp)) }
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Belum punya akun? ")
                TextButton(onClick = { navController.navigate(AppRoutes.REGISTER) }) { Text("Daftar di sini") }
            }
        }
    }
}

// ... (Preview tetap sama) ...
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun LoginScreenPreview() {
    HexenAppTheme {
        LoginScreen(navController = NavController(LocalContext.current))
    }
}
