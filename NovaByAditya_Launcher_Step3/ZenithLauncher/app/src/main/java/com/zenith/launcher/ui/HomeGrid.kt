@file:OptIn(ExperimentalFoundationApi::class)

package com.zenith.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.zenith.launcher.LauncherViewModel
import com.zenith.launcher.data.HomeItem
import kotlin.math.floor

/** Same header height on every page so grid cells line up across pages. */
val HeaderHeight = 200.dp

@Composable
fun HomePageGrid(vm: LauncherViewModel, page: Int) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 12.dp).padding(bottom = 130.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(HeaderHeight)) { if (page == 0) HomeHeader(vm) }
        HomeGrid(vm, page, Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
fun HomeGrid(vm: LauncherViewModel, page: Int, modifier: Modifier) {
    val cols = vm.prefs.homeCols
    val rows = vm.prefs.homeRows
    val items = vm.homeItemsFor(page)
    BoxWithConstraints(
        modifier.then(
            if (vm.editMode) Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.07f)) else Modifier
        )
    ) {
        val cellW = maxWidth / cols
        val cellH = maxHeight / rows
        items.forEach { item -> key(item.id) { HomeCell(vm, item, cellW, cellH) } }
        if (items.isEmpty() && !vm.editMode && page == 0) {
            Text(
                "Your Home is empty.\nOpen the app drawer, long-press an app and choose “Add to Home”.",
                color = Color.White.copy(0.8f), fontSize = 14.sp, textAlign = TextAlign.Center, style = ShadowStyle,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
        }
    }
}

@Composable
fun HomeCell(vm: LauncherViewModel, item: HomeItem, cellW: Dp, cellH: Dp) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (pressed && !vm.prefs.reducedMotion && !vm.editMode) 0.92f else 1f,
        spring(stiffness = Spring.StiffnessMedium), label = "press"
    )
    val filter = iconFilter(vm)

    val app = if (item.type == "app") vm.visibleApps.firstOrNull { it.pkg == item.ref } else null
    val folder = if (item.type == "folder") vm.prefs.folders.firstOrNull { it.id == item.ref } else null
    if (app == null && folder == null) return

    val cwPx = with(density) { cellW.toPx() }
    val chPx = with(density) { cellH.toPx() }
    val cols = vm.prefs.homeCols
    val rows = vm.prefs.homeRows
    val iconSize = minOf(56.dp * vm.prefs.iconScale, cellH - 24.dp, cellW - 12.dp).coerceAtLeast(24.dp)
    val buzz = { if (vm.prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    Box(
        Modifier
            .offset(cellW * item.x, cellH * item.y)
            .size(cellW, cellH)
            .zIndex(if (dragging) 10f else 0f)
            .graphicsLayer {
                translationX = drag.x
                translationY = drag.y
                val s = (if (dragging) 1.12f else 1f) * pressScale
                scaleX = s
                scaleY = s
            }
            .combinedClickable(
                interactionSource = src,
                indication = null,
                onClick = {
                    if (vm.editMode) menu = true
                    else if (app != null) vm.launch(app)
                    else if (folder != null) vm.openFolderId = folder.id
                },
                onLongClick = { buzz(); menu = true }
            )
            .pointerInput(vm.editMode, item.page, item.x, item.y, cols, rows) {
                if (vm.editMode) {
                    detectDragGestures(
                        onDragStart = { dragging = true; buzz() },
                        onDragEnd = {
                            val tx = floor((item.x * cwPx + cwPx / 2 + drag.x) / cwPx).toInt().coerceIn(0, cols - 1)
                            val ty = floor((item.y * chPx + chPx / 2 + drag.y) / chPx).toInt().coerceIn(0, rows - 1)
                            drag = Offset.Zero
                            dragging = false
                            vm.moveItem(item.id, item.page, tx, ty)
                        },
                        onDragCancel = { drag = Offset.Zero; dragging = false },
                        onDrag = { change, amount -> change.consume(); drag += amount }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (app != null) Image(app.icon, app.label, Modifier.size(iconSize), colorFilter = filter)
            else if (folder != null) FolderGlyph(folder, vm, iconSize)
            if (vm.prefs.showLabels) {
                Text(
                    app?.label ?: folder?.name ?: "",
                    color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center, style = ShadowStyle,
                    modifier = Modifier.padding(top = 2.dp).widthIn(max = cellW - 4.dp)
                )
            }
        }
        if (vm.editMode) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape)
                    .background(Color.Black.copy(0.75f)).clickable { vm.removeFromHome(item.id) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, "Remove from Home", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            val dismiss = { menu = false }
            if (vm.editMode) {
                for (p in 0 until vm.prefs.homePages) {
                    if (p != item.page) {
                        DropdownMenuItem(
                            text = { Text("Move to page ${p + 1}") },
                            onClick = { dismiss(); vm.moveToPage(item.id, p) }
                        )
                    }
                }
                DropdownMenuItem(text = { Text("Remove from Home") }, onClick = { dismiss(); vm.removeFromHome(item.id) })
            } else {
                if (app != null) AppMenuItems(app, vm, true) { dismiss() }
                if (folder != null) {
                    DropdownMenuItem(text = { Text("Open folder") }, onClick = { dismiss(); vm.openFolderId = folder.id })
                }
                DropdownMenuItem(text = { Text("Edit Home") }, onClick = { dismiss(); vm.enterEdit() })
                DropdownMenuItem(text = { Text("Remove from Home") }, onClick = { dismiss(); vm.removeFromHome(item.id) })
            }
        }
    }
}
