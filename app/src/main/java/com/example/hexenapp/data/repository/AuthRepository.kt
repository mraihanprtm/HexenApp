package com.example.hexenapp.data.repository

import com.google.firebase.auth.ActionCodeSettings // Import
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject // Jika Anda menggunakan constructor injection Hilt untuk FirebaseAuth di sini

// Jika AuthRepository di-provide oleh Hilt AppModule, constructor @Inject tidak di sini,
// tapi di AppModule saat provideAuthRepository.
class AuthRepository @Inject constructor(private val firebaseAuth: FirebaseAuth) { // Atau FirebaseAuth di-pass dari AppModule

    // ... (getCurrentUserFlow, getCurrentUser, register, login, signInWithGoogleCredential, logout tetap sama) ...
    fun getCurrentUserFlow(): Flow<FirebaseUser?> = callbackFlow { /* ... sama ... */
        val authStateListener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser).isSuccess }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser
    suspend fun registerWithEmailPassword(email: String, password: String): ResultWrapper<AuthResult> { /* ... sama ... */
        return try { ResultWrapper.Success(firebaseAuth.createUserWithEmailAndPassword(email, password).await()) }
        catch (e: Exception) { ResultWrapper.Error(e) }
    }
    suspend fun loginWithEmailPassword(email: String, password: String): ResultWrapper<AuthResult> { /* ... sama ... */
        return try { ResultWrapper.Success(firebaseAuth.signInWithEmailAndPassword(email, password).await()) }
        catch (e: Exception) { ResultWrapper.Error(e) }
    }
    suspend fun signInWithGoogleCredential(credential: AuthCredential): ResultWrapper<AuthResult> { /* ... sama ... */
        return try { ResultWrapper.Success(firebaseAuth.signInWithCredential(credential).await()) }
        catch (e: Exception) { ResultWrapper.Error(e) }
    }
    fun logout() { firebaseAuth.signOut() }


    /**
     * Mengirim email reset password dengan ActionCodeSettings.
     *
     * @param email Alamat email untuk mengirim link reset.
     * @param actionCodeSettings Pengaturan untuk link aksi email.
     * @return ResultWrapper<Unit> yang menandakan sukses atau error.
     */
    suspend fun sendPasswordResetEmail(email: String, actionCodeSettings: ActionCodeSettings): ResultWrapper<Unit> { // Tambahkan parameter actionCodeSettings
        return try {
            firebaseAuth.sendPasswordResetEmail(email, actionCodeSettings).await() // Gunakan actionCodeSettings
            ResultWrapper.Success(Unit)
        } catch (e: Exception) {
            ResultWrapper.Error(e)
        }
    }

    /**
     * Memverifikasi kode reset password.
     *
     * @param actionCode Kode dari link email.
     * @return ResultWrapper<String> yang berisi email pengguna jika kode valid, atau error.
     */
    suspend fun verifyPasswordResetCode(actionCode: String): ResultWrapper<String> {
        return try {
            val email = firebaseAuth.verifyPasswordResetCode(actionCode).await()
            if (email != null) {
                ResultWrapper.Success(email)
            } else {
                ResultWrapper.Error(Exception("Gagal memverifikasi kode, email tidak ditemukan."))
            }
        } catch (e: Exception) {
            ResultWrapper.Error(e)
        }
    }

    /**
     * Mengonfirmasi reset password dengan password baru.
     *
     * @param actionCode Kode dari link email.
     * @param newPassword Password baru pengguna.
     * @return ResultWrapper<Unit> yang menandakan sukses atau error.
     */
    suspend fun confirmPasswordReset(actionCode: String, newPassword: String): ResultWrapper<Unit> {
        return try {
            firebaseAuth.confirmPasswordReset(actionCode, newPassword).await()
            ResultWrapper.Success(Unit)
        } catch (e: Exception) {
            ResultWrapper.Error(e)
        }
    }
}

// Sealed class ResultWrapper tetap sama
sealed class ResultWrapper<out T> { /* ... sama ... */
    data class Success<out T>(val value: T) : ResultWrapper<T>()
    data class Error(val exception: Exception) : ResultWrapper<Nothing>()
    object Loading : ResultWrapper<Nothing>()
}
