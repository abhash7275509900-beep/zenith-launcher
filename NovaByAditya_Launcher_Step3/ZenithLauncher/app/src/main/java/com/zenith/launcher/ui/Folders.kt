@file:OptIn(ExperimentalFoundationApi::class)

package com.zenith.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.launcher.LauncherViewModel
import com.zenith.launcher.data.Folder

@Composable
fun FolderTile(folder: Folder, vm: LauncherViewModel) {
    val preview = vm.folderApps(folder).take(4)
    Column(
        Modifier.padding(end = 12.dp).combinedClickable(onClick = { vm.openFolderId = folder.id }),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.18f)).padding(7.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                preview.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { Image(it.icon, null, Modifier.size(22.dp)) }
                    }
                }
            }
        }
        Text(
            folder.name, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = ShadowStyle, modifier = Modifier.padding(top = 4.dp).widthIn(max = 72.dp)
        )
    }
}

@Composable
fun FolderGlyph(folder: Folder, vm: LauncherViewModel, size: androidx.compose.ui.unit.Dp) {
    val preview = vm.folderApps(folder).take(4)
    val inner = size * 0.36f
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(Color.White.copy(0.20f)),
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(size * 0.06f)) {
            preview.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(size * 0.06f)) {
                    row.forEach { Image(it.icon, null, Modifier.size(inner)) }
                }
            }
        }
    }
}

@Composable
fun FolderPickerDialog(vm: LauncherViewModel) {
    val app = vm.folderPickerFor ?: return
    var newName by remember(app) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { vm.folderPickerFor = null },
        title = { Text("Add ${app.label} to a folder") },
        text = {
            Column {
                vm.prefs.folders.forEach { f ->
                    TextButton(onClick = { vm.prefs.addToFolder(f.id, app.pkg); vm.folderPickerFor = null }) { Text(f.name) }
                }
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it.take(24) },
                    label = { Text("New folder name") }, singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = newName.isNotBlank(),
                onClick = {
                    val id = vm.prefs.createFolder(newName.trim(), app.pkg)
                    vm.addToHome("folder", id)
                    vm.folderPickerFor = null
                }
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = { vm.folderPickerFor = null }) { Text("Cancel") } }
    )
}

@Composable
fun FolderDialog(vm: LauncherViewModel) {
    val id = vm.openFolderId ?: return
    val folder = vm.prefs.folders.firstOrNull { it.id == id } ?: return
    var name by remember(id) { mutableStateOf(folder.name) }
    val apps = vm.folderApps(folder)
    val close = { vm.prefs.renameFolder(id, name); vm.openFolderId = null }
    AlertDialog(
        onDismissRequest = { close() },
        title = {
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(24) },
                label = { Text("Folder name") }, singleLine = true
            )
        },
        text = {
            if (apps.isEmpty()) {
                Text("This folder is empty. Long-press an app and choose “Add to folder”.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tap to open · long-press to remove", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    apps.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { app ->
                                Column(
                                    Modifier.width(76.dp).combinedClickable(
                                        onClick = { vm.openFolderId = null; vm.launch(app) },
                                        onLongClick = { vm.prefs.removeFromFolder(id, app.pkg) }
                                    ),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(app.icon, app.label, Modifier.size(48.dp))
                                    Text(app.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { close() }) { Text("Done") } },
        dismissButton = {
            TextButton(onClick = { vm.prefs.deleteFolder(id); vm.openFolderId = null }) { Text("Delete folder") }
        }
    )
}
