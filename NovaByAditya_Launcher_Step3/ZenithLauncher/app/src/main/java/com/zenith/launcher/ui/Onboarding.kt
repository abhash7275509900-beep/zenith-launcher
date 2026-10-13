package com.zenith.launcher.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.launcher.LauncherViewModel

@Composable
fun ThemeCard(t: NovaTheme, selected: Boolean, subtitle: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(t.surface)
            .then(if (selected) Modifier.border(2.dp, t.primary, shape) else Modifier)
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(t.primary))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(14.dp).clip(CircleShape).background(t.secondary))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, color = t.text)
            Text(subtitle, color = t.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun OnboardingScreen(vm: LauncherViewModel) {
    var step by remember { mutableIntStateOf(0) }
    val p = vm.prefs
    BackHandler { if (step > 0) step-- }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (step) {
                    0 -> {
                        Spacer(Modifier.height(40.dp))
                        Text("Nova by Aditya", style = MaterialTheme.typography.displaySmall)
                        Text(
                            "A fast, private launcher. Your layout and settings stay on this device, " +
                                "and Nova asks for no sensitive permissions.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Four quick steps: welcome, theme, starting layout, default Home app.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    1 -> {
                        Text("Choose a theme", style = MaterialTheme.typography.headlineSmall)
                        NovaThemes.forEach { t ->
                            ThemeCard(t, selected = p.themeId == t.id, subtitle = if (p.themeId == t.id) "Selected" else "Tap to select") {
                                p.setTheme(t.id)
                            }
                        }
                        Text("You can change this any time in Settings.", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    2 -> {
                        Text("Pick a starting layout", style = MaterialTheme.typography.headlineSmall)
                        Text("You can change it any time. Drag to rearrange after setup.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        listOf(
                            Triple("minimal", "Minimal", "Clean Home, apps in the drawer and dock"),
                            Triple("productivity", "Productivity", "Calendar, email, maps, browser and work apps"),
                            Triple("gaming", "Gaming", "Your installed games up front")
                        ).forEach { (k, n, d) ->
                            OutlinedButton(onClick = { vm.applyStarter(k) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth()) { Text(n); Text(d, fontSize = 12.sp) }
                            }
                        }
                        Text("Or skip this and add apps yourself from the drawer.", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> {
                        Text("Make Nova your Home app", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Android needs you to confirm this yourself. Tap the button, pick Nova by Aditya, " +
                                "then come back and press Finish.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { vm.start(Intent(Settings.ACTION_HOME_SETTINGS)) }) {
                            Text("Choose default Home app")
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(4) { i ->
                    Box(
                        Modifier.padding(end = 6.dp).size(if (i == step) 10.dp else 7.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.let { if (i == step) it else it.copy(0.35f) })
                    )
                }
                Spacer(Modifier.weight(1f))
                if (step == 0) TextButton(onClick = { p.setOnboarded() }) { Text("Skip") }
                if (step > 0) TextButton(onClick = { step-- }) { Text("Back") }
                Button(onClick = { if (step < 3) step++ else p.setOnboarded() }) {
                    Text(if (step < 3) "Next" else "Finish")
                }
            }
        }
    }
}
