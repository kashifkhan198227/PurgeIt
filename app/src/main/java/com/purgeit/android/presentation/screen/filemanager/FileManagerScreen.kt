package com.purgeit.android.presentation.screen.filemanager

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purgeit.android.domain.model.JunkFile
import com.purgeit.android.presentation.util.formatBytes

private fun hasStoragePermission(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    onBack: () -> Unit,
    viewModel: FileManagerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // Permission gate for MANAGE_EXTERNAL_STORAGE
    var hasPermission by remember { mutableStateOf(hasStoragePermission()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = hasStoragePermission()
                if (granted && !hasPermission) {
                    hasPermission = true
                    viewModel.scanJunk()
                } else {
                    hasPermission = granted
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-scan on first open if permission is already granted
    LaunchedEffect(hasPermission) {
        if (hasPermission && uiState.junkFiles.isEmpty() && !uiState.isScanning) {
            viewModel.scanJunk()
        }
    }

    if (!hasPermission) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("File Manager") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Storage Access Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("PurgeIt needs access to all files to scan for junk and duplicates. Tap below and enable 'Allow access to manage all files'.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Grant Storage Access") }
                }
            }
        }
        return
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirmation,
            title = { Text("Confirm Delete") },
            text = {
                Text(
                    "Delete ${uiState.selectedFiles.size} files? " +
                        "This will free ${formatBytes(
                            uiState.junkFiles.filter { it.file.path in uiState.selectedFiles }
                                .sumOf { it.file.sizeBytes }
                        )}. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(onClick = viewModel::deleteSelected) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirmation) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedFiles.isNotEmpty()) {
                FloatingActionButton(onClick = viewModel::showDeleteConfirmation) {
                    Icon(Icons.Default.Delete, "Delete selected")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("Junk Files") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Duplicates") })
            }

            if (uiState.isScanning) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Scanning…")
                    LinearProgressIndicator(
                        progress = { uiState.scanProgress },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }

            when (selectedTab) {
                0 -> JunkTab(uiState, viewModel)
                1 -> DuplicatesTab(uiState, viewModel)
            }
        }
    }
}

@Composable
private fun JunkTab(uiState: FileManagerUiState, viewModel: FileManagerViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.junkFiles.isEmpty() && !uiState.isScanning) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No junk files scanned yet")
                    Button(onClick = viewModel::scanJunk, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Scan Now")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${formatBytes(uiState.totalJunkBytes)} junk found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedButton(onClick = viewModel::selectAllJunk) { Text("Select All") }
            }

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(uiState.junkFiles, key = { it.file.path }) { junk ->
                    JunkFileCard(
                        junk = junk,
                        isSelected = junk.file.path in uiState.selectedFiles,
                        onToggle = { viewModel.toggleFileSelection(junk.file.path) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DuplicatesTab(uiState: FileManagerUiState, viewModel: FileManagerViewModel) {
    if (uiState.duplicateGroups.isEmpty() && !uiState.isScanning) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No duplicates scanned yet")
                Button(onClick = viewModel::scanDuplicates, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Find Duplicates")
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.duplicateGroups, key = { it.hash }) { group ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "${group.files.size} duplicates · ${formatBytes(group.totalWastedBytes)} wasted",
                            fontWeight = FontWeight.Medium,
                        )
                        group.files.forEachIndexed { index, file ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = file.path in uiState.selectedFiles,
                                    onCheckedChange = { viewModel.toggleFileSelection(file.path) },
                                    enabled = index != 0, // keep first copy by default
                                )
                                Column {
                                    Text(file.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        file.path,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JunkFileCard(junk: JunkFile, isSelected: Boolean, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(junk.file.name, fontWeight = FontWeight.Medium)
                Text(
                    "${junk.category.name.replace('_', ' ')} · ${formatBytes(junk.file.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
