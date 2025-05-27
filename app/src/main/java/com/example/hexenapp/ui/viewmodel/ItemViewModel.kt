package com.example.hexenapp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hexenapp.data.local.model.ItemEntity
import com.example.hexenapp.data.remote.dto.ItemCreateRequest
import com.example.hexenapp.data.remote.dto.ItemUpdateRequest
import com.example.hexenapp.data.repository.ItemRepository
import com.example.hexenapp.data.repository.ResultWrapper // Pastikan ResultWrapper diimport
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val firebaseAuth: FirebaseAuth, // Di-inject untuk mendapatkan UID pengguna saat ini
    application: Application
) : AndroidViewModel(application) {

    private val _currentUserId = MutableStateFlow<String?>(null)

    // State untuk hasil operasi (misalnya, pesan error atau sukses dari API)
    private val _operationStatus = MutableStateFlow<ResultWrapper<Any>?>(null) // Gunakan Any atau tipe spesifik jika perlu
    val operationStatus: StateFlow<ResultWrapper<Any>?> = _operationStatus.asStateFlow()

    // State untuk loading global (misalnya saat refresh atau operasi CRUD)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        _currentUserId.value = firebaseAuth.currentUser?.uid
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val newUid = auth.currentUser?.uid
            if (_currentUserId.value != newUid) {
                _currentUserId.value = newUid
                if (newUid != null) {
                    // Jika pengguna berubah atau login, refresh data dari server
                    refreshItems()
                }
            }
        }
        firebaseAuth.addAuthStateListener(authStateListener)

        // Panggil refreshItems saat ViewModel pertama kali dibuat jika ada pengguna
        if (_currentUserId.value != null) {
            refreshItems()
        }
    }

    val allItems: StateFlow<List<ItemEntity>> = _currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            itemRepository.getAllItems(userId) // Sumber data tetap dari Room
        } else {
            MutableStateFlow(emptyList())
        }
    }.catch { exception ->
        Log.e("ItemViewModel", "Error collecting allItems from Room", exception)
        emit(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedItem = MutableStateFlow<ItemEntity?>(null)
    val selectedItem: StateFlow<ItemEntity?> = _selectedItem.asStateFlow()

    /**
     * Meminta refresh data item dari server.
     */
    fun refreshItems() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                itemRepository.refreshItemsFromServer()
                // Tidak perlu emit status sukses khusus di sini karena allItems akan otomatis update
            } catch (e: Exception) {
                Log.e("ItemViewModel", "Error refreshing items", e)
                _operationStatus.value = ResultWrapper.Error(e) // Tampilkan error jika refresh gagal
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun insertItem(name: String, description: String?, quantity: Int) {
        val userId = _currentUserId.value
        if (userId == null) {
            _operationStatus.value = ResultWrapper.Error(Exception("Pengguna tidak login."))
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val request = ItemCreateRequest(name = name, description = description, quantity = quantity)
            val result = itemRepository.insertItem(request)
            _operationStatus.value = result // UI akan mengobservasi ini
            _isLoading.value = false
        }
    }

    fun updateItem(itemEntity: ItemEntity) { // Menerima ItemEntity untuk mendapatkan ID dan data lama
        val userId = _currentUserId.value
        if (userId == null || itemEntity.userId != userId) {
            _operationStatus.value = ResultWrapper.Error(Exception("Operasi tidak diizinkan."))
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            // Buat ItemUpdateRequest dari ItemEntity
            val request = ItemUpdateRequest(
                name = itemEntity.name, // Asumsi semua field dikirim untuk update
                description = itemEntity.description,
                quantity = itemEntity.quantity
            )
            val result = itemRepository.updateItem(itemEntity.id, request)
            _operationStatus.value = result
            _isLoading.value = false
        }
    }

    // Menggunakan deleteItem dari repository yang menerima itemId
    fun deleteItem(itemEntity: ItemEntity) {
        val userId = _currentUserId.value
        if (userId == null || itemEntity.userId != userId) {
            _operationStatus.value = ResultWrapper.Error(Exception("Operasi tidak diizinkan."))
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = itemRepository.deleteItem(itemEntity.id)
            _operationStatus.value = result
            _isLoading.value = false
        }
    }


    fun deleteAllItemsForCurrentUser() {
        val userId = _currentUserId.value
        if (userId == null) {
            _operationStatus.value = ResultWrapper.Error(Exception("Pengguna tidak login."))
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            // TODO: Panggil repository untuk hapus di server juga jika ada endpointnya
            itemRepository.deleteAllItemsForUser(userId) // Ini hanya menghapus lokal saat ini
            _operationStatus.value = ResultWrapper.Success(Unit) // Asumsi sukses jika hanya lokal
            _isLoading.value = false
        }
    }

    fun loadItemById(itemId: Int) {
        val userId = _currentUserId.value
        if (userId == null) {
            _selectedItem.value = null
            return
        }
        viewModelScope.launch {
            // Untuk load detail, kita mungkin ingin mengambil dari server untuk data terbaru
            // atau cukup dari Room jika sudah disinkronkan.
            // Untuk saat ini, kita ambil dari Room.
            itemRepository.getItemById(itemId, userId)
                .catch { exception ->
                    Log.e("ItemViewModel", "Error loading item by id from Room", exception)
                    _selectedItem.value = null
                }
                .collect { item ->
                    _selectedItem.value = item
                }
        }
    }

    fun clearSelectedItem() {
        _selectedItem.value = null
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }

    fun searchItems(query: String): Flow<List<ItemEntity>> {
        return _currentUserId.flatMapLatest { userId ->
            if (userId != null) {
                if (query.isBlank()) {
                    itemRepository.getAllItems(userId)
                } else {
                    itemRepository.searchItemsByName(query, userId)
                }
            } else {
                MutableStateFlow(emptyList())
            }
        }.catch {
            Log.e("ItemViewModel", "Error searching items from Room", it)
            emit(emptyList())
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Hapus AuthStateListener jika perlu, tapi FirebaseAuth biasanya menangani lifecycle-nya sendiri
        // firebaseAuth.removeAuthStateListener(authStateListener) // Perlu referensi ke listener
    }
}
