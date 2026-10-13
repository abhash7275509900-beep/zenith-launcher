@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.zenith.launcher.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.launcher.LauncherViewModel
import com.zenith.launcher.LoadState
import com.zenith.launcher.data.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun LauncherRoot(vm: LauncherViewModel) {
    val reduced = vm.prefs.reducedMotion
    LauncherTheme(vm.prefs, vm.previewThemeId) {
        Box(Modifier.fillMaxSize()) {
            HomeScreen(vm)
            AnimatedVisibility(
                visible = vm.drawerOpen,
                enter = if (reduced) EnterTransition.None else
                    slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it / 2 } + fadeIn(tween(180)) + scaleIn(initialScale = 0.96f),
                exit = if (reduced) ExitTransition.None else
                    slideOutVertically(tween(200)) { it / 3 } + fadeOut(tween(160))
            ) { AppDrawer(vm) }
            AnimatedVisibility(
                visible = vm.settingsOpen,
                enter = if (reduced) EnterTransition.None else fadeIn(),
                exit = if (reduced) ExitTransition.None else fadeOut()
            ) { SettingsScreen(vm) }
            FolderPickerDialog(vm)
            FolderDialog(vm)
            if (!vm.prefs.onboarded) OnboardingScreen(vm)
        }
    }
}

// ───────────────────────── Home ─────────────────────────

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val pager = rememberPagerState(initialPage = 1) { 1 + vm.prefs.homePages }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(vm.homeSignal) { if (vm.homeSignal > 0) pager.animateScrollToPage(1) }
    BackHandler { if (vm.editMode) vm.commitEdit() else scope.launch { pager.animateScrollToPage(1) } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(vm.prefs.wallpaperDim))
            .background(Brush.verticalGradient(listOf(Color.Black.copy(0.30f), Color.Transparent, Color.Black.copy(0.50f))))
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                    onDragEnd = {
                        if (!vm.editMode) {
                            if (total < -120f) vm.openDrawer()
                            else if (total > 120f) expandNotifications(ctx)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = {
                    if (!vm.editMode) {
                        if (vm.prefs.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.enterEdit()
                    }
                })
            }
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) HubPage(vm) else HomePageGrid(vm, page - 1)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                repeat(pager.pageCount) { i ->
                    Box(
                        Modifier.size(if (pager.currentPage == i) 8.dp else 6.dp).clip(CircleShape)
                            .background(Color.White.copy(if (pager.currentPage == i) 0.95f else 0.45f))
                    )
                }
            }
            if (vm.editMode) EditPanel(vm, pager.currentPage - 1) else Dock(vm)
        }
    }
}

@Composable
fun EditPanel(vm: LauncherViewModel, currentHomePage: Int) {
    val p = vm.prefs
    val canRemovePage = currentHomePage >= 0 && p.homePages > 1 && p.homeItems.none { it.page == currentHomePage }
    Column(
        Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(28.dp))
            .background(Color.Black.copy(glassAlpha(p, 0.62f))).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.cancelEdit() }) { Text("Cancel") }
            TextButton(enabled = vm.canUndo, onClick = { vm.undo() }) { Text("Undo") }
            Button(onClick = { vm.commitEdit() }) { Text("Done") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = p.homePages < Prefs.MAX_PAGES, onClick = { vm.addPage() }) { Text("Add page") }
            if (canRemovePage) TextButton(onClick = { vm.removePage(currentHomePage) }) { Text("Remove page") }
            TextButton(onClick = { vm.settingsOpen = true }) { Text("Settings") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Columns", color = Color.White, fontSize = 13.sp)
            (3..6).forEach { n ->
                FilterChip(selected = p.homeCols == n, onClick = { vm.setHomeGrid(n, p.homeRows) }, label = { Text("$n") })
            }
            Spacer(Modifier.width(8.dp))
            Text("Rows", color = Color.White, fontSize = 13.sp)
            (4..7).forEach { n ->
                FilterChip(selected = p.homeRows == n, onClick = { vm.setHomeGrid(p.homeCols, n) }, label = { Text("$n") })
            }
        }
        Text("Drag icons to move · tap an icon for page options", color = Color.White.copy(0.7f), fontSize = 12.sp)
    }
}

@Composable
fun Dock(vm: LauncherViewModel) {
    val dockShape = RoundedCornerShape((LocalNova.current.corner + 8).dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(dockShape)
            .background(Color.Black.copy(glassAlpha(vm.prefs, 0.38f)))
            .novaEdge(LocalNova.current, dockShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        vm.dockApps().forEach { AppIcon(it, vm, Color.White, showLabel = false) }
        IconButton(onClick = { vm.openDrawer() }) {
            Icon(Icons.Default.KeyboardArrowUp, "All apps", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun rememberNow(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000 - (System.currentTimeMillis() % 60_000) + 50)
        }
    }
    return now
}

@Composable
fun HomeHeader(vm: LauncherViewModel) {
    val ctx = LocalContext.current
    val now = rememberNow()
    val pattern = if (DateFormat.is24HourFormat(ctx)) "HH:mm" else "h:mm"
    val time = now.format(DateTimeFormatter.ofPattern(pattern))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))

    val pill = RoundedCornerShape((LocalNova.current.corner + 4).dp)
    val openAlarms = { vm.start(Intent(AlarmClock.ACTION_SHOW_ALARMS)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(24.dp))
        when (vm.prefs.clockStyle) {
            2 -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { openAlarms() }) {
                Text(time, fontSize = 44.sp, fontWeight = FontWeight.Medium, color = Color.White, style = ShadowStyle)
                Spacer(Modifier.width(14.dp))
                Text(date, fontSize = 16.sp, color = Color.White.copy(0.92f), style = ShadowStyle)
            }
            1 -> Column(Modifier.clickable { openAlarms() }) {
                Text(time, fontSize = 68.sp, fontWeight = FontWeight.Bold, color = Color.White, style = ShadowStyle)
                Text(date, fontSize = 18.sp, color = Color.White.copy(0.92f), style = ShadowStyle)
            }
            else -> Column(Modifier.clickable { openAlarms() }) {
                Text(time, fontSize = 72.sp, fontWeight = FontWeight.Light, color = Color.White, style = ShadowStyle)
                Text(date, fontSize = 18.sp, color = Color.White.copy(0.92f), style = ShadowStyle)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth().clip(pill).background(Color.Black.copy(glassAlpha(vm.prefs, 0.30f)))
                .novaEdge(LocalNova.current, pill)
                .clickable { vm.openDrawer(focus = true) }.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Text("Search apps & web", color = Color.White.copy(0.85f))
        }
    }
}

@Composable
fun HubPage(vm: LauncherViewModel) {
    val ctx = LocalContext.current
    var battery by remember { mutableIntStateOf(0) }
    var charging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val lvl = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            battery = if (lvl >= 0 && scale > 0) lvl * 100 / scale else 0
            val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
            delay(30_000)
        }
    }
    val top = vm.mostUsed(6)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(20.dp).padding(bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Today", fontSize = 32.sp, fontWeight = FontWeight.Light, color = Color.White, style = ShadowStyle)
        HubCard(vm) {
            Text("Battery", color = Color.White.copy(0.8f), fontSize = 13.sp)
            Text(
                "$battery%" + if (charging) "  ·  charging" else "",
                color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { battery / 100f },
                modifier = Modifier.fillMaxWidth().clip(CircleShape),
                trackColor = Color.White.copy(0.2f)
            )
        }
        HubCard(vm) {
            Text("Most used", color = Color.White.copy(0.8f), fontSize = 13.sp)
            if (top.isEmpty()) {
                Text("Apps you open often will appear here.", color = Color.White)
            }
            top.forEach { app ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.launch(app) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(app.icon, null, Modifier.size(36.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(app.label, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun HubCard(vm: LauncherViewModel, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(LocalNova.current.corner.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Color.Black.copy(glassAlpha(vm.prefs, 0.42f)))
            .novaEdge(LocalNova.current, shape).padding(16.dp),
        content = content
    )
}

// ───────────────────────── Drawer ─────────────────────────

private fun matches(label: String, q: String): Boolean {
    val l = label.lowercase()
    val s = q.trim().lowercase()
    if (s.isEmpty() || l.contains(s)) return true
    val initials = l.split(" ", "-", "_").filter { it.isNotEmpty() }.joinToString("") { it.take(1) }
    return initials.startsWith(s)
}

private fun letterOf(label: String): Char {
    val c = label.firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}

@Composable
fun AppDrawer(vm: LauncherViewModel) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val visible = vm.visibleApps
    val filtered = remember(query, visible) {
        if (query.isBlank()) visible
        else visible.filter { matches(it.label, query) }
            .sortedBy { if (it.label.lowercase().startsWith(query.trim().lowercase())) 0 else 1 }
    }
    val letters = remember(filtered) { filtered.map { letterOf(it.label) }.distinct().sorted() }
    val onBg = MaterialTheme.colorScheme.onBackground

    BackHandler { if (query.isNotEmpty()) query = "" else vm.drawerOpen = false }
    LaunchedEffect(Unit) { if (vm.focusSearch) focus.requestFocus() }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = vm.prefs.drawerOpacity))
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding()) {
            Column(
                Modifier.pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onVerticalDrag = { _, dy -> total += dy },
                        onDragEnd = { if (total > 120f) vm.drawerOpen = false }
                    )
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.padding(top = 10.dp).size(width = 40.dp, height = 4.dp).clip(CircleShape)
                        .background(onBg.copy(0.3f))
                )
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, "Clear") }
                        } else {
                            IconButton(onClick = { vm.settingsOpen = true }) { Icon(Icons.Default.Settings, "Settings") }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (filtered.isNotEmpty()) vm.launch(filtered.first())
                        else if (query.isNotBlank()) vm.searchWeb(query)
                    }),
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).focusRequester(focus)
                )
            }
            Box(Modifier.weight(1f)) {
                DrawerStatus(vm, visible)
                if (visible.isNotEmpty()) LazyVerticalGrid(
                    columns = GridCells.Fixed(vm.prefs.columns),
                    state = state,
                    contentPadding = PaddingValues(start = 8.dp, end = 28.dp, bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.pkg + "/" + it.activity }) { app ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            AppIcon(app, vm, onBg)
                        }
                    }
                    if (query.isNotBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Search the web for “${query.trim()}”",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().clickable { vm.searchWeb(query) }.padding(16.dp)
                            )
                        }
                    }
                }
                if (query.isBlank() && letters.size > 1) {
                    AlphabetBar(
                        letters = letters,
                        onSelect = { ch ->
                            val idx = filtered.indexOfFirst { letterOf(it.label) == ch }
                            if (idx >= 0) scope.launch { state.scrollToItem(idx) }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerStatus(vm: LauncherViewModel, visible: List<com.zenith.launcher.data.AppInfo>) {
    if (visible.isNotEmpty()) return
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        when {
            vm.loadState == LoadState.Loading -> CircularProgressIndicator()
            vm.loadState == LoadState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Couldn't load your apps.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.refresh() }) { Text("Retry") }
            }
            else -> Text("No apps to show. Check Hidden apps in Settings.")
        }
    }
}

// ───────────────────────── Settings ─────────────────────────

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
fun SettingsScreen(vm: LauncherViewModel) {
    val p = vm.prefs
    BackHandler { vm.closeSettings() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.closeSettings() }) { Icon(Icons.Default.Close, "Close") }
            }

            vm.previewThemeId?.let { id ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Previewing ${themeById(id).name}", Modifier.weight(1f))
                    TextButton(onClick = { vm.previewThemeId = null }) { Text("Cancel") }
                    Button(onClick = { p.setTheme(id); vm.previewThemeId = null }) { Text("Apply") }
                }
            }

            Section("Theme") {
                NovaThemes.forEach { t ->
                    val current = t.id == p.themeId
                    ThemeCard(
                        t,
                        selected = (vm.previewThemeId ?: p.themeId) == t.id,
                        subtitle = if (current) "Current" else "Tap to preview"
                    ) { vm.previewThemeId = if (current) null else t.id }
                }
                Text("Accent colour")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = p.accentMode == 0, onClick = { p.setAccentMode(0) }, label = { Text("Theme") })
                    if (android.os.Build.VERSION.SDK_INT >= 31) {
                        FilterChip(selected = p.accentMode == 1, onClick = { p.setAccentMode(1) }, label = { Text("Wallpaper") })
                    }
                    FilterChip(selected = p.accentMode == 2, onClick = { p.setAccentMode(2) }, label = { Text("Custom") })
                }
                if (p.accentMode == 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Accents.forEachIndexed { i, c ->
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(c)
                                    .then(if (p.accentIndex == i) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier)
                                    .clickable { p.setAccent(i) }
                            )
                        }
                    }
                }
            }

            Section("Appearance") {
                Text("Icon style")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Original", "Monochrome").forEachIndexed { i, t ->
                        FilterChip(selected = p.iconStyle == i, onClick = { p.setIconStyle(i) }, label = { Text(t) })
                    }
                }
                Text("Clock style")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Light", "Bold", "Compact").forEachIndexed { i, t ->
                        FilterChip(selected = p.clockStyle == i, onClick = { p.setClockStyle(i) }, label = { Text(t) })
                    }
                }
                Text("Wallpaper dimming")
                Slider(value = p.wallpaperDim, onValueChange = { p.setWallpaperDim(it) }, valueRange = 0f..0.6f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("High contrast panels", Modifier.weight(1f))
                    Switch(checked = p.highContrast, onCheckedChange = { p.setHighContrast(it) })
                }
            }

            var starter by remember { mutableStateOf<String?>(null) }
            Section("Starter layouts") {
                Text("Replaces your current Home layout.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("minimal" to "Minimal", "productivity" to "Productivity", "gaming" to "Gaming").forEach { (k, n) ->
                        OutlinedButton(onClick = { starter = k }) { Text(n) }
                    }
                }
            }
            starter?.let { k ->
                AlertDialog(
                    onDismissRequest = { starter = null },
                    title = { Text("Apply $k layout?") },
                    text = { Text("Your current Home pages will be replaced. Folders and dock are kept.") },
                    confirmButton = { TextButton(onClick = { vm.applyStarter(k); starter = null }) { Text("Apply") } },
                    dismissButton = { TextButton(onClick = { starter = null }) { Text("Cancel") } }
                )
            }

            Section("Grid") {
                Text("Columns")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (3..6).forEach { n ->
                        FilterChip(selected = p.columns == n, onClick = { p.setColumns(n) }, label = { Text("$n") })
                    }
                }
                Text("Icon size")
                Slider(value = p.iconScale, onValueChange = { p.setIconScale(it) }, valueRange = 0.7f..1.4f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show app names", Modifier.weight(1f))
                    Switch(checked = p.showLabels, onCheckedChange = { p.setLabels(it) })
                }
            }

            Section("App drawer") {
                Text("Background opacity")
                Slider(value = p.drawerOpacity, onValueChange = { p.setDrawerOpacity(it) }, valueRange = 0.3f..1f)
            }

            val hiddenApps = vm.apps.filter { it.pkg in p.hidden }
            Section("Hidden apps") {
                if (hiddenApps.isEmpty()) Text("None. Long-press an app and choose Hide.")
                hiddenApps.forEach { app ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(app.icon, null, Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(app.label, Modifier.weight(1f))
                        TextButton(onClick = { p.unhide(app.pkg) }) { Text("Unhide") }
                    }
                }
            }

            Section("System") {
                OutlinedButton(onClick = { vm.start(Intent(Settings.ACTION_HOME_SETTINGS)) }) {
                    Text("Set as default launcher")
                }
                OutlinedButton(onClick = {
                    vm.start(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"))
                }) { Text("Change wallpaper") }
            }

            Section("Motion & feel") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reduce motion", Modifier.weight(1f))
                    Switch(checked = p.reducedMotion, onCheckedChange = { p.setReducedMotion(it) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Haptic feedback", Modifier.weight(1f))
                    Switch(checked = p.haptics, onCheckedChange = { p.setHaptics(it) })
                }
            }

            val ctx = LocalContext.current
            val ver = remember {
                try {
                    val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                    "v${pi.versionName} (build ${pi.longVersionCode})"
                } catch (_: Exception) { "" }
            }
            var confirmReset by remember { mutableStateOf(false) }
            Section("About") {
                Text("Nova by Aditya $ver · Android API ${android.os.Build.VERSION.SDK_INT}")
                OutlinedButton(onClick = {
                    vm.start(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, "Nova by Aditya feedback")
                            .putExtra(Intent.EXTRA_TEXT, "Nova $ver\nAndroid API ${android.os.Build.VERSION.SDK_INT}\n\nMy feedback:\n"),
                        "Send feedback"))
                }) { Text("Send feedback") }
                OutlinedButton(onClick = { p.setOnboarded(false) }) { Text("Show setup again") }
                OutlinedButton(onClick = { confirmReset = true }) { Text("Reset Nova") }
            }
            if (confirmReset) {
                AlertDialog(
                    onDismissRequest = { confirmReset = false },
                    title = { Text("Reset Nova?") },
                    text = { Text("This resets theme, folders, dock, hidden apps and settings. Backup/export is planned for a later step, so this can't be undone yet.") },
                    confirmButton = { TextButton(onClick = { p.resetAll(); vm.previewThemeId = null; confirmReset = false }) { Text("Reset") } },
                    dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
                )
            }

            Text(
                "Gestures: swipe up = all apps · swipe down = notifications · long-press empty space = edit Home · swipe left = Today page",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(0.65f)
            )
        }
    }
}
