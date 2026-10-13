package com.zenith.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppInfo(
    val label: String,
    val pkg: String,
    val activity: String,
    val icon: ImageBitmap,
    val category: Int = -1
)

class AppRepository(private val ctx: Context) {

    fun load(): List<AppInfo> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { ri ->
                AppInfo(
                    label = ri.loadLabel(pm).toString(),
                    pkg = ri.activityInfo.packageName,
                    activity = ri.activityInfo.name,
                    icon = ri.loadIcon(pm).toBitmap(144, 144).asImageBitmap(),
                    category = ri.activityInfo.applicationInfo.category
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    /** Default apps for well-known categories (calendar, email, maps...) as chosen on this device. */
    fun packagesForCategories(cats: List<String>, apps: List<AppInfo>): List<String> {
        val pm = ctx.packageManager
        val known = apps.map { it.pkg }.toSet()
        return cats.mapNotNull {
            pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(it), PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        }.filter { it in known }.distinct()
    }

    /** Phone, messages, browser, camera — whatever the device has set as default. */
    fun defaultDockPackages(apps: List<AppInfo>): List<String> {
        val pm = ctx.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        )
        val known = apps.map { it.pkg }.toSet()
        return intents
            .mapNotNull { pm.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName }
            .filter { it in known }
            .distinct()
    }
}
