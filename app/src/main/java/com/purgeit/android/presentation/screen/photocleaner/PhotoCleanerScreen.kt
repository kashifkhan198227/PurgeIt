package com.purgeit.android.presentation.screen.photocleaner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.purgeit.android.domain.model.Photo
import com.purgeit.android.domain.model.PhotoGroup
import com.purgeit.android.domain.model.PhotoQuality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCleanerScreen(
    onBack: () -> Unit,
    viewModel: PhotoCleanerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, mediaPermission) == PackageManager.PERMISSION_GRANTED)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(context, mediaPermission) == PackageManager.PERMISSION_GRANTED
                if (granted && !hasPermission) {
                    hasPermission = true
                    if (uiState.groups.isEmpty() && !uiState.isScanning) viewModel.scan()
                } else {
                    hasPermission = granted
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted && uiState.groups.isEmpty() && !uiState.isScanning) viewModel.scan()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && uiState.groups.isEmpty() && !uiState.isScanning) viewModel.scan()
    }

    if (!hasPermission) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Photo Cleaner") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                )
            }
        ) { padding ->
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Text("Photo Access Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("PurgeIt needs access to your photos to find duplicates and low-quality images.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { permissionLauncher.launch(mediaPermission) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant Photo Access")
                    }
                }
            }
        }
        return
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirmation,
            title = { Text("Move to Trash") },
            text = {
                Text(
                    "Move ${uiState.selectedPhotoIds.size} photos to Trash? " +
                        "You can restore them within 30 days."
                )
            },
            confirmButton = {
                Button(onClick = viewModel::deleteSelected) { Text("Move to Trash") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirmation) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Cleaner") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedPhotoIds.isNotEmpty()) {
                FloatingActionButton(onClick = viewModel::showDeleteConfirmation) {
                    Icon(Icons.Default.Delete, "Delete selected")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            if (uiState.isScanning) {
                LinearProgressIndicator(
                    progress = { uiState.scanProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (uiState.groups.isEmpty() && !uiState.isScanning) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Scan your gallery for junk photos")
                        Button(onClick = viewModel::scan, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Scan Gallery")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(uiState.groups, key = { it.groupType.name + it.photos.first().id }) { group ->
                        PhotoGroupCard(
                            group = group,
                            selectedIds = uiState.selectedPhotoIds,
                            onToggle = viewModel::togglePhotoSelection,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoGroupCard(
    group: PhotoGroup,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                groupTitle(group.groupType),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "${group.photos.size} photos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                userScrollEnabled = false,
            ) {
                items(group.photos, key = { it.id }) { photo ->
                    PhotoThumbnail(
                        photo = photo,
                        isSelected = photo.id in selectedIds,
                        isKeep = group.photos.indexOf(photo) == group.suggestedKeepIndex,
                        onToggle = { onToggle(photo.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(
    photo: Photo,
    isSelected: Boolean,
    isKeep: Boolean,
    onToggle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onToggle)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary)
                else Modifier
            ),
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isKeep) {
            Badge(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                containerColor = MaterialTheme.colorScheme.primary,
            ) { Text("Keep") }
        }
    }
}

private fun groupTitle(quality: PhotoQuality) = when (quality) {
    PhotoQuality.BLURRY -> "Blurry Photos"
    PhotoQuality.DARK -> "Dark / Underexposed"
    PhotoQuality.DUPLICATE -> "Exact Duplicates"
    PhotoQuality.BURST_REDUNDANT -> "Burst / Near-Duplicates"
    PhotoQuality.SCREENSHOT -> "Old Screenshots"
    else -> "Low Quality Photos"
}
