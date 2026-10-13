@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.zenith.launcher.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.launcher.LauncherViewModel
import com.zenith.launcher.data.AppInfo

val ShadowStyle = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 2f), 6f))

fun expandNotifications(ctx: Context) {
    try {
        val svc = ctx.getSystemService("statusbar")
        Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(svc)
    } catch (_: Exception) { }
}

/** Monochrome icon treatment when the user turns it on. */
@Composable
fun iconFilter(vm: LauncherViewModel): ColorFilter? =
    if (vm.prefs.iconStyle == 1) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null

/** Short springy press feedback; disabled by Reduce motion. */
@Composable
fun Modifier.pressScale(src: MutableInteractionSource, reduced: Boolean): Modifier {
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pressed && !reduced) 0.92f else 1f,
        spring(stiffness = Spring.StiffnessMedium), label = "press"
    )
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

@Composable
fun AppIcon(
    app: AppInfo,
    vm: LauncherViewModel,
    labelColor: Color,
    showLabel: Boolean = vm.prefs.showLabels,
    shadow: Boolean = false
) {
    val size: Dp = 56.dp * vm.prefs.iconScale
    var menuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    val filter = iconFilter(vm)
    Box {
        Column(
            Modifier
                .pressScale(src, vm.prefs.reducedMotion)
                .combinedClickable(interactionSource = src, indication = null, onClick = { vm.launch(app) }, onLongClick = {
                    if (vm.prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuOpen = true
                })
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(size), colorFilter = filter)
            if (showLabel) {
                Text(
                    app.label,
                    color = labelColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = if (shadow) ShadowStyle else TextStyle.Default,
                    modifier = Modifier.padding(top = 4.dp).widthIn(max = size + 20.dp)
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            AppMenuItems(app, vm) { menuOpen = false }
        }
    }
}

@Composable
fun ColumnScope.AppMenuItems(app: AppInfo, vm: LauncherViewModel, onHome: Boolean = false, dismiss: () -> Unit) {
    val inDock = app.pkg in vm.dockPkgs()
    if (!onHome) {
        DropdownMenuItem(text = { Text("Add to Home") }, onClick = { dismiss(); vm.addToHome("app", app.pkg) })
    }
    DropdownMenuItem(text = { Text("App info") }, onClick = { dismiss(); vm.openAppInfo(app) })
    DropdownMenuItem(
        text = { Text(if (inDock) "Remove from dock" else "Add to dock") },
        onClick = { dismiss(); vm.toggleDock(app.pkg) }
    )
    DropdownMenuItem(text = { Text("Add to folder") }, onClick = { dismiss(); vm.folderPickerFor = app })
    DropdownMenuItem(text = { Text("Hide") }, onClick = { dismiss(); vm.prefs.hide(app.pkg) })
    DropdownMenuItem(text = { Text("Uninstall") }, onClick = { dismiss(); vm.uninstall(app) })
}

@Composable
fun AlphabetBar(letters: List<Char>, onSelect: (Char) -> Unit, modifier: Modifier = Modifier) {
    var heightPx by remember { mutableIntStateOf(1) }
    val pick = { y: Float ->
        if (letters.isNotEmpty()) {
            val i = ((y / heightPx) * letters.size).toInt().coerceIn(0, letters.size - 1)
            onSelect(letters[i])
        }
    }
    Column(
        modifier
            .fillMaxHeight()
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(letters) { detectTapGestures { pick(it.y) } }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { pick(it.y) },
                    onVerticalDrag = { change, _ -> pick(change.position.y) }
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach {
            Text(
                it.toString(),
                fontSize = 10.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}
