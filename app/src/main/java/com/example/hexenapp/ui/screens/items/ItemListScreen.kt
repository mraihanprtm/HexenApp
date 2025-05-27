package com.example.hexenapp.ui.screens.items

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh // Ikon untuk refresh manual
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.hexenapp.data.local.model.ItemEntity
import com.example.hexenapp.data.repository.ResultWrapper // Pastikan ini diimport
import com.example.hexenapp.ui.navigation.AppRoutes
import com.example.hexenapp.ui.theme.HexenAppTheme
import com.example.hexenapp.ui.viewmodel.AuthViewModel
import com.example.hexenapp.ui.viewmodel.ItemViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    navController: NavController,
    itemViewModel: ItemViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val items by itemViewModel.allItems.collectAsState()
    val isLoading by itemViewModel.isLoading.collectAsState() // Observasi isLoading
    val operationStatus by itemViewModel.operationStatus.collectAsState() // Observasi operationStatus

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() } // Untuk menampilkan Snackbar
    val coroutineScope = rememberCoroutineScope()

    var showTokenDialog by remember { mutableStateOf(false) }
    var currentIdToken by remember { mutableStateOf<String?>(null) }
    var isLoadingToken by remember { mutableStateOf(false) }

    // Menangani operationStatus untuk menampilkan Snackbar
    LaunchedEffect(operationStatus) {
        when (val status = operationStatus) {
            is ResultWrapper.Success -> {
                // Pesan sukses bisa ditampilkan jika perlu, atau biarkan UI update secara otomatis
                // Contoh: snackbarHostState.showSnackbar("Operasi berhasil!")
                itemViewModel.clearOperationStatus() // Bersihkan status setelah ditangani
            }
            is ResultWrapper.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Error: ${status.exception.message ?: "Terjadi kesalahan"}",
                    duration = SnackbarDuration.Long
                )
                itemViewModel.clearOperationStatus() // Bersihkan status setelah ditangani
            }
            is ResultWrapper.Loading -> { /* Ditangani oleh state isLoading global */ }
            null -> { /* Tidak ada status operasi */ }
        }
    }


    fun fetchAndShowIdToken() {
        isLoadingToken = true
        currentIdToken = null
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUser.getIdToken(true)
                .addOnSuccessListener { result ->
                    isLoadingToken = false
                    currentIdToken = result.token
                    if (currentIdToken != null) {
                        showTokenDialog = true
                    } else {
                        Toast.makeText(context, "Gagal mendapatkan ID Token (null).", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    isLoadingToken = false
                    Toast.makeText(context, "Error mendapatkan ID Token: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            isLoadingToken = false
            Toast.makeText(context, "Tidak ada pengguna yang login.", Toast.LENGTH_SHORT).show()
        }
    }

    if (showTokenDialog && currentIdToken != null) {
        TokenDisplayDialog(
            idToken = currentIdToken!!,
            onDismissRequest = { showTokenDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Tambahkan SnackbarHost
        topBar = {
            TopAppBar(
                title = { Text("Daftar Item HexenApp") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Tombol Refresh Manual
                    IconButton(onClick = { itemViewModel.refreshItems() }, enabled = !isLoading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Item")
                    }
                    IconButton(onClick = { fetchAndShowIdToken() }, enabled = !isLoadingToken) {
                        if (isLoadingToken) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Key, contentDescription = "Tampilkan ID Token")
                        }
                    }
                    IconButton(onClick = { authViewModel.logoutUser(context) }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(AppRoutes.addEditItemRoute(null))
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Item")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) { // Gunakan Box untuk menempatkan loading di tengah
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (items.isEmpty() && !isLoading) { // Hanya tampilkan pesan jika tidak loading dan items kosong
                    Text(
                        text = "Belum ada item. Tekan tombol '+' untuk menambah atau refresh.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                } else if (!items.isEmpty()) { // Hanya tampilkan list jika ada item
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(items, key = { item -> item.id }) { item ->
                            ItemRow(
                                item = item,
                                onItemClick = {
                                    navController.navigate(AppRoutes.addEditItemRoute(item.id.toString()))
                                },
                                onEditClick = {
                                    navController.navigate(AppRoutes.addEditItemRoute(item.id.toString()))
                                },
                                onDeleteClick = {
                                    // Bisa tambahkan dialog konfirmasi di sini sebelum menghapus
                                    itemViewModel.deleteItem(item)
                                }
                            )
                        }
                    }
                }
            }

            // Tampilkan CircularProgressIndicator di tengah jika isLoading
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// TokenDisplayDialog, SelectableText, ItemRow, formatTimestamp, dan Preview tetap sama
// ... (Salin sisa kode dari versi sebelumnya) ...
@Composable
fun TokenDisplayDialog(idToken: String, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Firebase ID Token Anda:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                SelectableText(text = idToken, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Firebase ID Token", idToken)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Token disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                        onDismissRequest()
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Salin Token", modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Salin & Tutup")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismissRequest) { Text("Tutup") }
                }
            }
        }
    }
}

@Composable
fun SelectableText(text: String, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current) {
    SelectionContainer(modifier = modifier) { Text(text, style = style) }
}

@Composable
fun ItemRow(item: ItemEntity, onItemClick: (ItemEntity) -> Unit, onEditClick: (ItemEntity) -> Unit, onDeleteClick: (ItemEntity) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onItemClick(item) }, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                item.description?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)) }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Jumlah: ${item.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                Text(text = "Dibuat: ${formatTimestamp(item.createdAt)}", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                Text(text = "Diperbarui: ${formatTimestamp(item.updatedAt)}", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            Row {
                IconButton(onClick = { onEditClick(item) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit Item", tint = MaterialTheme.colorScheme.primary) }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { onDeleteClick(item) }) { Icon(Icons.Filled.Delete, contentDescription = "Hapus Item", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
fun ItemListScreenPreview() {
    HexenAppTheme {
        val dummyItem = ItemEntity(id=1, userId="previewUser", name="Contoh Item Preview", description="Ini deskripsi preview", quantity=10)
        Column(modifier = Modifier.padding(16.dp)) { ItemRow(item = dummyItem, onItemClick = {}, onEditClick = {}, onDeleteClick = {}) }
    }
}

@Preview(showBackground = true)
@Composable
fun TokenDisplayDialogPreview(){
    HexenAppTheme { TokenDisplayDialog(idToken = "iniadalahcontohtokenyangsangatpanjangsekalidantidakakancukupdalamsatulayaruntukdicopysecaramanualmakadariitukitaperlutombolcopy...", onDismissRequest = {}) }
}
