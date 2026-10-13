package com.zenith.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.launcher.data.AppInfo
import com.zenith.launcher.data.AppRepository
import com.zenith.launcher.data.HomeItem
import com.zenith.launcher.data.Prefs
import java.util.UUID
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LoadState { Loading, Ready, Error }

private data class HomeSnapshot(val items: List<HomeItem>, val pages: Int, val cols: Int, val rows: Int)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    private val repo = AppRepository(app)

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set
    private var defaultDock by mutableStateOf<List<String>>(emptyList())

    var drawerOpen by mutableStateOf(false)
    var focusSearch by mutableStateOf(false)
    var settingsOpen by mutableStateOf(false)
    var loadState by mutableStateOf(LoadState.Loading)
        private set
    var previewThemeId by mutableStateOf<String?>(null)
    var folderPickerFor by mutableStateOf<AppInfo?>(null)
    var openFolderId by mutableStateOf<String?>(null)
    var editMode by mutableStateOf(false)
        private set
    private var editBase: HomeSnapshot? = null
    private val history = androidx.compose.runtime.mutableStateListOf<HomeSnapshot>()
    val canUndo: Boolean get() = history.isNotEmpty()
    private var pruned = false
    var homeSignal by mutableIntStateOf(0)
        private set

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refresh()
    }

    init {
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(app, receiver, f, ContextCompat.RECEIVER_EXPORTED)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (apps.isEmpty()) loadState = LoadState.Loading
            try {
                val loaded = withContext(Dispatchers.Default) { repo.load() }
                apps = loaded
                defaultDock = repo.defaultDockPackages(loaded)
                loadState = LoadState.Ready
                pruneMissing(loaded)
            } catch (e: Exception) {
                loadState = LoadState.Error
            }
        }
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(receiver)
    }

    val visibleApps: List<AppInfo>
        get() = apps.filter { it.pkg !in prefs.hidden }

    fun mostUsed(n: Int): List<AppInfo> =
        visibleApps
            .filter { (prefs.usage[it.pkg] ?: 0) > 0 }
            .sortedByDescending { prefs.usage[it.pkg] ?: 0 }
            .take(n)

    fun dockPkgs(): List<String> = prefs.dock.ifEmpty { defaultDock }

    fun dockApps(): List<AppInfo> =
        dockPkgs().mapNotNull { p -> visibleApps.firstOrNull { it.pkg == p } }

    fun toggleDock(pkg: String) {
        val cur = dockPkgs()
        prefs.setDock(if (pkg in cur) cur - pkg else (cur + pkg).takeLast(5))
    }

    fun openDrawer(focus: Boolean = false) { focusSearch = focus; drawerOpen = true }

    // ───────── Home layout ─────────
    private fun snapshot() = HomeSnapshot(prefs.homeItems, prefs.homePages, prefs.homeCols, prefs.homeRows)
    private fun restore(s: HomeSnapshot) { prefs.setHomeGrid(s.cols, s.rows); prefs.setHome(s.items, s.pages) }
    private fun push() { if (editMode) history.add(snapshot()) }
    fun toast(msg: String) { Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show() }

    /** Once per launch, drop home entries whose app is really gone. Hidden apps are kept. */
    private fun pruneMissing(installed: List<AppInfo>) {
        if (pruned || installed.isEmpty()) return
        pruned = true
        val pk = installed.map { it.pkg }.toSet()
        val kept = prefs.homeItems.filter { it.type != "app" || it.ref in pk }
        if (kept.size != prefs.homeItems.size) prefs.setHome(kept, prefs.homePages)
    }

    private fun renderable(it: HomeItem) = when (it.type) {
        "app" -> visibleApps.any { a -> a.pkg == it.ref }
        "folder" -> prefs.folders.any { f -> f.id == it.ref }
        else -> false
    }

    fun homeItemsFor(page: Int): List<HomeItem> = prefs.homeItems.filter { it.page == page && renderable(it) }

    fun enterEdit() { editBase = snapshot(); history.clear(); editMode = true }
    fun commitEdit() { editMode = false; editBase = null; history.clear() }
    fun cancelEdit() { editBase?.let { restore(it) }; commitEdit() }
    fun undo() { history.removeLastOrNull()?.let { restore(it) } }

    private fun freeCell(items: List<HomeItem>, page: Int): Pair<Int, Int>? {
        val taken = items.filter { it.page == page }.map { it.x to it.y }.toSet()
        for (y in 0 until prefs.homeRows) for (x in 0 until prefs.homeCols) if ((x to y) !in taken) return x to y
        return null
    }

    fun addToHome(type: String, ref: String): Boolean {
        val items = prefs.homeItems
        if (items.any { it.type == type && it.ref == ref }) { toast("Already on Home"); return true }
        val pages = prefs.homePages
        for (p in 0 until pages) {
            val c = freeCell(items, p) ?: continue
            push()
            prefs.setHome(items + HomeItem(UUID.randomUUID().toString(), type, ref, p, c.first, c.second), pages)
            toast("Added to Home")
            return true
        }
        if (pages < Prefs.MAX_PAGES) {
            push()
            prefs.setHome(items + HomeItem(UUID.randomUUID().toString(), type, ref, pages, 0, 0), pages + 1)
            toast("Added to a new Home page")
            return true
        }
        toast("Home is full")
        return false
    }

    fun removeFromHome(id: String) {
        push()
        prefs.setHome(prefs.homeItems.filter { it.id != id }, prefs.homePages)
    }

    /** Move to a cell; if another item is there the two swap. */
    fun moveItem(id: String, page: Int, x: Int, y: Int) {
        val items = prefs.homeItems
        val me = items.firstOrNull { it.id == id } ?: return
        if (me.page == page && me.x == x && me.y == y) return
        val other = items.firstOrNull { it.id != id && it.page == page && it.x == x && it.y == y }
        push()
        prefs.setHome(items.map {
            when {
                it.id == id -> it.copy(page = page, x = x, y = y)
                other != null && it.id == other.id -> it.copy(page = me.page, x = me.x, y = me.y)
                else -> it
            }
        }, prefs.homePages)
    }

    fun moveToPage(id: String, page: Int) {
        val c = freeCell(prefs.homeItems, page)
        if (c == null) { toast("That page is full"); return }
        moveItem(id, page, c.first, c.second)
    }

    fun addPage() {
        if (prefs.homePages >= Prefs.MAX_PAGES) { toast("Maximum ${Prefs.MAX_PAGES} pages"); return }
        push()
        prefs.setHome(prefs.homeItems, prefs.homePages + 1)
    }

    fun removePage(i: Int) {
        if (prefs.homePages <= 1 || prefs.homeItems.any { it.page == i }) return
        push()
        prefs.setHome(prefs.homeItems.map { if (it.page > i) it.copy(page = it.page - 1) else it }, prefs.homePages - 1)
    }

    fun setHomeGrid(cols: Int, rows: Int) {
        if (cols == prefs.homeCols && rows == prefs.homeRows) return
        push()
        prefs.setHomeGrid(cols, rows)
        val cur = prefs.homeItems.toMutableList()
        var pages = prefs.homePages
        val outside = cur.filter { it.x >= cols || it.y >= rows }
        cur.removeAll(outside)
        for (m in outside) {
            var placed = false
            for (p in 0 until pages) {
                val c = freeCell(cur, p) ?: continue
                cur.add(m.copy(page = p, x = c.first, y = c.second)); placed = true; break
            }
            if (!placed && pages < Prefs.MAX_PAGES) { cur.add(m.copy(page = pages, x = 0, y = 0)); pages++ }
        }
        prefs.setHome(cur, pages)
    }

    /** Replaces the Home layout with a starter: "minimal", "productivity" or "gaming". */
    fun applyStarter(kind: String) {
        val visible = visibleApps
        val vis = visible.map { it.pkg }.toSet()
        val picks: List<String> = when (kind) {
            "productivity" -> {
                val wellKnown = repo.packagesForCategories(listOf(
                    Intent.CATEGORY_APP_CALENDAR, Intent.CATEGORY_APP_EMAIL, Intent.CATEGORY_APP_MAPS,
                    Intent.CATEGORY_APP_BROWSER, Intent.CATEGORY_APP_MESSAGING), visible)
                val byCat = visible.filter {
                    it.category == android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY ||
                        it.category == android.content.pm.ApplicationInfo.CATEGORY_NEWS
                }.map { it.pkg }
                (wellKnown + byCat).distinct().take(8)
            }
            "gaming" -> visible.filter { it.category == android.content.pm.ApplicationInfo.CATEGORY_GAME }
                .map { it.pkg }.take(12)
            else -> emptyList()
        }.filter { it in vis }
        val cols = 4
        prefs.setHomeGrid(cols, if (kind == "minimal") 4 else 5)
        val items = picks.mapIndexed { i, p -> HomeItem(UUID.randomUUID().toString(), "app", p, 0, i % cols, i / cols) }
        prefs.setHome(items, 1)
        history.clear()
        toast(
            when {
                kind == "minimal" -> "Minimal layout applied"
                items.isEmpty() -> "No matching apps found. Add apps from the drawer."
                else -> "Layout applied with ${items.size} apps"
            }
        )
    }

    fun closeSettings() { previewThemeId = null; settingsOpen = false }

    /** Folder members that are still installed and not hidden. Missing apps are skipped, not deleted. */
    fun folderApps(f: com.zenith.launcher.data.Folder): List<AppInfo> =
        f.pkgs.mapNotNull { p -> visibleApps.firstOrNull { it.pkg == p } }

    fun goHome() {
        drawerOpen = false
        closeSettings()
        homeSignal++
    }

    fun launch(app: AppInfo) {
        val i = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.pkg, app.activity))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            getApplication<Application>().startActivity(i)
            prefs.recordLaunch(app.pkg)
            drawerOpen = false
        } catch (_: Exception) { }
    }

    fun openAppInfo(app: AppInfo) = start(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}"))
    )

    fun uninstall(app: AppInfo) = start(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}")))

    fun searchWeb(q: String) {
        start(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))))
        drawerOpen = false
    }

    fun start(i: Intent) {
        try {
            getApplication<Application>().startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { }
    }
}
