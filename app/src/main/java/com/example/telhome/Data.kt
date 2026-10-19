package com.example.telhome

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat

// ---------- インストール済みアプリ ----------

data class AppInfo(
    val label: String,
    val pkg: String,
    val cls: String,
    val icon: ImageBitmap,
    /** アプリ自身が「ゲーム」として登録しているか */
    val isGame: Boolean = false,
)

object AppRepo {
    fun loadApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val list = pm.queryIntentActivities(intent, 0)
        return list
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                AppInfo(
                    label = ri.loadLabel(pm).toString(),
                    pkg = ri.activityInfo.packageName,
                    cls = ri.activityInfo.name,
                    icon = IconShape.roundedSquare(ri.loadIcon(pm), 192).asImageBitmap(),
                    isGame = ri.activityInfo.applicationInfo.category == android.content.pm.ApplicationInfo.CATEGORY_GAME,
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}

// ---------- ホームの小タイル（8枠） ----------

sealed class Slot {
    data class Builtin(val feature: Feature) : Slot()
    data class App(val pkg: String, val cls: String) : Slot()
    data object Empty : Slot()

    fun encode(): String = when (this) {
        is Builtin -> "builtin:${feature.name}"
        is App -> "app:$pkg/$cls"
        Empty -> "empty"
    }

    companion object {
        fun decode(s: String?): Slot? {
            if (s == null) return null
            return when {
                s == "empty" -> Empty
                s.startsWith("builtin:") ->
                    Feature.entries.firstOrNull { it.name == s.removePrefix("builtin:") }?.let { Builtin(it) }
                s.startsWith("app:") -> {
                    val body = s.removePrefix("app:")
                    val i = body.indexOf('/')
                    if (i <= 0) null else App(body.substring(0, i), body.substring(i + 1))
                }
                else -> null
            }
        }
    }
}

class SlotStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("slots", Context.MODE_PRIVATE)

    /** 標準機能の枠は、その機能を担当する実際のアプリ（本物のアイコン）に置き換えて返す */
    fun load(): List<Slot> = DEFAULTS.indices.map { i ->
        when (val slot = Slot.decode(prefs.getString("slot_$i", null)) ?: DEFAULTS[i]) {
            is Slot.Builtin -> {
                val app = resolveApp(slot.feature)
                val replaced = app ?: Slot.Empty
                prefs.edit().putString("slot_$i", replaced.encode()).apply()
                replaced
            }
            else -> slot
        }
    }

    /** 機能（メッセージ・カメラなど）を受け持つアプリを探す */
    private fun resolveApp(feature: Feature): Slot.App? {
        val pm = context.packageManager
        for (intent in feature.resolveIntents()) {
            @Suppress("DEPRECATION")
            val pkgs = pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }
            for (pkg in pkgs) {
                if (pkg == context.packageName || pkg == "android") continue
                val cmp = pm.getLaunchIntentForPackage(pkg)?.component ?: continue
                return Slot.App(cmp.packageName, cmp.className)
            }
        }
        return null
    }

    fun save(index: Int, slot: Slot) {
        prefs.edit().putString("slot_$index", slot.encode()).apply()
    }

    fun reset(index: Int) {
        prefs.edit().remove("slot_$index").apply()
    }

    companion object {
        const val ROWS = 3
        const val COLUMNS = 4
        const val COUNT = ROWS * COLUMNS
        val DEFAULTS: List<Slot> = listOf(
            Slot.Builtin(Feature.MESSAGES),
            Slot.Builtin(Feature.CALENDAR),
            Slot.Builtin(Feature.CAMERA),
            Slot.Builtin(Feature.GALLERY),
            Slot.Builtin(Feature.MAPS),
            Slot.Builtin(Feature.BROWSER),
            Slot.Builtin(Feature.MUSIC),
            Slot.Builtin(Feature.CALCULATOR),
            Slot.Builtin(Feature.ALARM),
            Slot.Empty,
            Slot.Empty,
            Slot.Empty,
        )
    }
}

// ---------- 着信・発信履歴 ----------

data class CallGroup(
    val number: String,
    val name: String?,
    val latestDate: Long,
    val latestType: Int,
    val count: Int,
)

object CallLogRepo {
    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    fun canCallDirectly(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    /** 番号ごとにまとめた履歴（新しい順） */
    fun load(context: Context, outgoing: Boolean): List<CallGroup> {
        if (!hasPermission(context)) return emptyList()
        val types = if (outgoing) {
            listOf(CallLog.Calls.OUTGOING_TYPE)
        } else {
            listOf(CallLog.Calls.INCOMING_TYPE, CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE)
        }
        val selection = "${CallLog.Calls.TYPE} IN (${types.joinToString(",")})"
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
        )
        val groups = LinkedHashMap<String, CallGroup>()
        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI, projection, selection, null, "${CallLog.Calls.DATE} DESC",
            )?.use { c ->
                var rows = 0
                while (c.moveToNext() && rows < 1000) {
                    rows++
                    val number = c.getString(0) ?: ""
                    val name = c.getString(1)?.takeIf { it.isNotBlank() }
                    val type = c.getInt(2)
                    val date = c.getLong(3)
                    val key = number.filter { it.isDigit() || it == '+' }.ifEmpty { "unknown" }
                    val existing = groups[key]
                    groups[key] = existing?.copy(count = existing.count + 1, name = existing.name ?: name)
                        ?: CallGroup(number, name, date, type, 1)
                }
            }
        } catch (e: SecurityException) {
            return emptyList()
        }
        return groups.values.toList()
    }
}
