package com.example.hexenapp.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hexenapp.data.repository.AuthRepository
import com.example.hexenapp.data.repository.ResultWrapper
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.ActionCodeSettings // Import ActionCodeSettings
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    application: Application // Untuk mendapatkan package name
) : AndroidViewModel(application) {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    private val _authResult = MutableStateFlow<ResultWrapper<AuthResult>?>(null)
    val authResult: StateFlow<ResultWrapper<AuthResult>?> = _authResult.asStateFlow()
    private val _resetPasswordResult = MutableStateFlow<ResultWrapper<Unit>?>(null)
    val resetPasswordResult: StateFlow<ResultWrapper<Unit>?> = _resetPasswordResult.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        _currentUser.value = authRepository.getCurrentUser()
        viewModelScope.launch {
            authRepository.getCurrentUserFlow()
                .catch { _currentUser.value = null }
                .collect { user -> _currentUser.value = user }
        }
    }

    fun registerUser(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authResult.value = ResultWrapper.Loading
            val result = authRepository.registerWithEmailPassword(email, password)
            _authResult.value = result
            _isLoading.value = false
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authResult.value = ResultWrapper.Loading
            val result = authRepository.loginWithEmailPassword(email, password)
            _authResult.value = result
            _isLoading.value = false
        }
    }

    fun signInWithGoogle(credential: AuthCredential) {
        viewModelScope.launch {
            _isLoading.value = true
            _authResult.value = ResultWrapper.Loading
            val result = authRepository.signInWithGoogleCredential(credential)
            _authResult.value = result
            _isLoading.value = false
        }
    }

    fun logoutUser(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Identity.getSignInClient(context).signOut().await()
            } catch (e: Exception) { /* logging */ }
            finally {
                authRepository.logout()
                _authResult.value = null // Reset auth result
                // _currentUser akan diupdate oleh listener di init
                _isLoading.value = false
            }
        }
    }


    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _resetPasswordResult.value = ResultWrapper.Loading

            val packageName = getApplication<Application>().packageName

            // Menggunakan domain Firebase Hosting Anda.
            // Pastikan path '/finishPasswordReset' (atau path pilihan Anda)
            // akan Anda tangani di AndroidManifest.xml.
            val continueUrl = "https://hexenapp-backend.web.app/finishPasswordReset"

            val actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl(continueUrl) // Menggunakan domain Firebase Hosting Anda
                .setHandleCodeInApp(true)
                .setAndroidPackageName(
                    packageName,
                    true, /* installIfNotAvailable */
                    "1" /* minimumVersion */
                )
                .build()

            val result = authRepository.sendPasswordResetEmail(email, actionCodeSettings)
            _resetPasswordResult.value = result
            _isLoading.value = false
        }
    }

    private val _verifyCodeResult = MutableStateFlow<ResultWrapper<String>?>(null)
    val verifyCodeResult: StateFlow<ResultWrapper<String>?> = _verifyCodeResult.asStateFlow()

    private val _confirmResetResult = MutableStateFlow<ResultWrapper<Unit>?>(null)
    val confirmResetResult: StateFlow<ResultWrapper<Unit>?> = _confirmResetResult.asStateFlow()

    fun verifyPasswordResetCode(actionCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _verifyCodeResult.value = ResultWrapper.Loading
            val result = authRepository.verifyPasswordResetCode(actionCode)
            _verifyCodeResult.value = result
            _isLoading.value = false
        }
    }

    fun confirmPasswordReset(actionCode: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _confirmResetResult.value = ResultWrapper.Loading
            val result = authRepository.confirmPasswordReset(actionCode, newPassword)
            _confirmResetResult.value = result
            _isLoading.value = false
        }
    }

    fun clearAuthResult() { _authResult.value = null }
    fun clearResetPasswordResult() { _resetPasswordResult.value = null }
    fun clearVerifyCodeResult() { _verifyCodeResult.value = null }
    fun clearConfirmResetResult() { _confirmResetResult.value = null }
}
