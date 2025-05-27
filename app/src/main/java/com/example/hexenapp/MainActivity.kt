package com.example.hexenapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.hexenapp.ui.navigation.AppNavigation
import com.example.hexenapp.ui.navigation.AppRoutes
import com.example.hexenapp.ui.theme.HexenAppTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController
    private val TAG_TOKEN_LOG = "FIREBASE_ID_TOKEN_DEBUG" // Tag log yang lebih spesifik

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            navController = rememberNavController()
            HexenAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(navController = navController)
                }
            }
        }
        intent?.let { handleIntent(it) }

        Log.d(TAG_TOKEN_LOG, "onCreate: Memanggil logCurrentUserFirebaseIdToken()")
        logCurrentUserFirebaseIdToken()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action: String? = intent.action
        val data: Uri? = intent.data
        Log.d("MainActivity", "Handling intent: Action=$action, Data=$data")

        if (Intent.ACTION_VIEW == action && data != null) {
            if (FirebaseAuth.getInstance().isSignInWithEmailLink(data.toString())) {
                Log.d("MainActivity", "Diterima sign-in with email link: $data")
            } else {
                val mode = data.getQueryParameter("mode")
                val actionCode = data.getQueryParameter("oobCode")
                Log.d("MainActivity", "Deep Link Diterima: mode=$mode, oobCode=$actionCode")

                if (mode == "resetPassword" && actionCode != null) {
                    if (::navController.isInitialized) {
                        val route = "${AppRoutes.RESET_PASSWORD_CONFIRM}?oobCode=$actionCode"
                        Log.d("MainActivity", "Navigating to: $route")
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    } else {
                        Log.e("MainActivity", "NavController belum diinisialisasi saat menangani deep link reset password.")
                    }
                }
            }
        }
    }

    private fun logCurrentUserFirebaseIdToken() {
        Log.d(TAG_TOKEN_LOG, "Memulai logCurrentUserFirebaseIdToken...")
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            Log.d(TAG_TOKEN_LOG, "CurrentUser ditemukan: UID = ${currentUser.uid}, Email = ${currentUser.email}")
            Log.d(TAG_TOKEN_LOG, "Memanggil getIdToken(true)...")
            currentUser.getIdToken(true) // Parameter true untuk memaksa refresh token
                .addOnSuccessListener { result ->
                    val idToken = result.token
                    if (idToken != null) {
                        Log.d(TAG_TOKEN_LOG, "SUKSES! Token: $idToken")
                        // Salin token ini dari Logcat untuk pengujian Postman
                    } else {
                        Log.w(TAG_TOKEN_LOG, "GAGAL! ID Token adalah null meskipun user ada dan getIdToken sukses.")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG_TOKEN_LOG, "GAGAL! Gagal mendapatkan ID Token:", exception)
                }
                .addOnCompleteListener { task ->
                    Log.d(TAG_TOKEN_LOG, "getIdToken complete. Sukses: ${task.isSuccessful}")
                    if (!task.isSuccessful) {
                        Log.e(TAG_TOKEN_LOG, "getIdToken complete dengan error:", task.exception)
                    }
                }
        } else {
            Log.d(TAG_TOKEN_LOG, "Tidak ada pengguna yang login saat ini (currentUser adalah null).")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    HexenAppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // Preview
        }
    }
}
