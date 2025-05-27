package com.example.hexenapp.ui.screens.items

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hexenapp.data.local.model.ItemEntity
import com.example.hexenapp.data.repository.ResultWrapper // Pastikan ini diimport
import com.example.hexenapp.ui.theme.HexenAppTheme
import com.example.hexenapp.ui.viewmodel.ItemViewModel
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    navController: NavController,
    itemViewModel: ItemViewModel = hiltViewModel(),
    itemId: String?
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var itemName by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }
    var itemQuantity by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var currentItemState: ItemEntity? by remember { mutableStateOf(null) }

    val screenTitle = if (isEditing) "Edit Item" else "Tambah Item Baru"

    val isLoading by itemViewModel.isLoading.collectAsState() // Observasi isLoading
    val operationStatus by itemViewModel.operationStatus.collectAsState() // Observasi operationStatus
    val snackbarHostState = remember { SnackbarHostState() }

    // Efek untuk memuat item jika dalam mode edit
    LaunchedEffect(key1 = itemId) {
        if (itemId != null) {
            isEditing = true
            itemViewModel.loadItemById(itemId.toInt())
        } else {
            isEditing = false
            itemName = ""
            itemDescription = ""
            itemQuantity = ""
            currentItemState = null
            itemViewModel.clearSelectedItem()
        }
    }

    // Mengisi form jika item yang dipilih berubah (untuk mode edit)
    val selectedItemFromVM by itemViewModel.selectedItem.collectAsState()
    LaunchedEffect(selectedItemFromVM) {
        if (isEditing && selectedItemFromVM != null) {
            currentItemState = selectedItemFromVM
            itemName = selectedItemFromVM!!.name
            itemDescription = selectedItemFromVM!!.description ?: ""
            itemQuantity = selectedItemFromVM!!.quantity.toString()
        } else if (!isEditing && itemId == null) { // Reset jika kembali ke mode tambah dari edit
            itemName = ""
            itemDescription = ""
            itemQuantity = ""
            currentItemState = null
        }
    }

    // Menangani operationStatus untuk menampilkan Snackbar dan navigasi
    LaunchedEffect(operationStatus) {
        when (val status = operationStatus) {
            is ResultWrapper.Success -> {
                // Tipe Any tidak memberikan informasi spesifik, tapi kita tahu ini dari operasi item
                val successMessage = if (isEditing) "Item berhasil diperbarui!" else "Item berhasil ditambahkan!"
                Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show() // Atau Snackbar
                itemViewModel.clearOperationStatus()
                navController.popBackStack() // Kembali ke layar daftar setelah sukses
            }
            is ResultWrapper.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Error: ${status.exception.message ?: "Operasi gagal"}",
                    duration = SnackbarDuration.Long
                )
                itemViewModel.clearOperationStatus()
            }
            is ResultWrapper.Loading -> { /* Ditangani oleh state isLoading pada tombol */ }
            null -> { /* Tidak ada status operasi */ }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Tambahkan verticalScroll
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("Nama Item") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading // Disable saat loading
            )

            OutlinedTextField(
                value = itemDescription,
                onValueChange = { itemDescription = it },
                label = { Text("Deskripsi Item (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !isLoading // Disable saat loading
            )

            OutlinedTextField(
                value = itemQuantity,
                onValueChange = { itemQuantity = it.filter { char -> char.isDigit() } },
                label = { Text("Jumlah Item") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !isLoading // Disable saat loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    val quantityInt = itemQuantity.toIntOrNull() ?: 0
                    if (itemName.isNotBlank()) {
                        if (isEditing && currentItemState != null) {
                            // Untuk update, kita perlu ItemEntity yang sudah ada dengan ID dan userId yang benar
                            // Lalu buat ItemUpdateRequest dari field yang diubah.
                            val updatedEntity = currentItemState!!.copy(
                                name = itemName,
                                description = itemDescription.ifBlank { null },
                                quantity = quantityInt
                            )
                            itemViewModel.updateItem(updatedEntity)
                        } else {
                            itemViewModel.insertItem(
                                name = itemName,
                                description = itemDescription.ifBlank { null },
                                quantity = quantityInt
                            )
                        }
                        // Navigasi kembali akan ditangani oleh LaunchedEffect(operationStatus)
                    } else {
                        Toast.makeText(context, "Nama item tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading // Disable tombol saat loading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isEditing) "Simpan Perubahan" else "Tambah Item")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddItemScreenPreview() {
    HexenAppTheme {
        AddEditItemScreen(navController = NavController(LocalContext.current), itemId = null)
    }
}

@Preview(showBackground = true)
@Composable
fun EditItemScreenPreview() {
    HexenAppTheme {
        AddEditItemScreen(navController = NavController(LocalContext.current), itemId = "1")
    }
}
