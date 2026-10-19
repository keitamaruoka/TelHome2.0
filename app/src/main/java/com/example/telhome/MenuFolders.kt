@file:OptIn(ExperimentalFoundationApi::class)

package com.example.telhome

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MENU画面の「ポイントカード」「クーポン」から開く、アプリをまとめたフォルダ。
 * 最初に開いたとき、端末に入っているアプリから名前で探して自動で入れる。
 * あとから「＋追加」で好きなアプリを足したり、長押しで外したりできる。
 */
enum class MenuFolder(val title: String, val keywords: List<String>) {
    POINT(
        "ポイントカード",
        listOf("Vポイント", "Tポイント", "dポイント", "マツキヨ", "ココカラ", "白光舎"),
    ),
    COUPON(
        "クーポン",
        listOf("マクドナルド", "マック", "すかいらーく", "ガスト", "バーミヤン", "ジョナサン", "クーポン"),
    ),
    SHOPPING(
        "ショッピング",
        listOf("Amazon", "楽天", "Yahoo!ショッピング", "メルカリ", "ZOZO", "ユニクロ", "PayPayフリマ", "ショッピング", "通販"),
    ),
    TRANSPORT(
        "交通",
        listOf("乗換", "Suica", "ICOCA", "PASMO", "JR", "NAVITIME", "えきねっと", "新幹線", "タクシー", "S.RIDE", "バス", "電車", "運行"),
    ),
    GAME(
        "ゲーム",
        listOf("ゲーム", "Game", "パズル", "将棋", "囲碁", "麻雀", "ソリティア", "数独"),
    ),
    TOOLS(
        "便利ツール",
        listOf("電卓", "計算機", "Calculator", "時計", "Clock", "アラーム", "メモ", "Keep", "ボイスレコーダー", "レコーダー"),
    ),
}

class MenuFolderStore(context: Context) {
    private val prefs = context.getSharedPreferences("menu_folders", Context.MODE_PRIVATE)

    var version by mutableIntStateOf(0)
        private set

    fun isInitialized(folder: MenuFolder) = prefs.getBoolean("init_${folder.name}", false)

    /** 保存されている順番のまま、入っているアプリを返す（アンインストール済みは除く） */
    fun apps(folder: MenuFolder, all: List<AppInfo>): List<AppInfo> {
        val byKey = all.associateBy { it.key }
        return keys(folder).mapNotNull { byKey[it] }
    }

    /** 初回だけ、名前にキーワードを含むアプリを自動で入れる */
    fun initializeIfNeeded(folder: MenuFolder, all: List<AppInfo>) {
        if (isInitialized(folder) || all.isEmpty()) return
        val found = all.filter { app ->
            (folder == MenuFolder.GAME && app.isGame) ||
                folder.keywords.any { app.label.contains(it, ignoreCase = true) }
        }
        save(folder, found.map { it.key }, initialized = true)
    }

    fun add(folder: MenuFolder, app: AppInfo) {
        val list = keys(folder)
        if (app.key !in list) save(folder, list + app.key)
    }

    fun remove(folder: MenuFolder, app: AppInfo) = save(folder, keys(folder) - app.key)

    private fun keys(folder: MenuFolder): List<String> =
        prefs.getString("apps_${folder.name}", null)?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

    private fun save(folder: MenuFolder, keys: List<String>, initialized: Boolean = true) {
        prefs.edit()
            .putString("apps_${folder.name}", keys.joinToString("\n"))
            .putBoolean("init_${folder.name}", initialized)
            .apply()
        version++
    }
}

/** フォルダを開いた画面（MENU画面と同じ見た目で、中のアプリを並べる） */
@Composable
fun MenuFolderScreen(
    folder: MenuFolder,
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    torchOn: Boolean = false,
    onTorch: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val store = remember { MenuFolderStore(context) }
    val version = store.version
    LaunchedEffect(apps) { store.initializeIfNeeded(folder, apps) }
    val inFolder = remember(apps, version) { store.apps(folder, apps) }

    var showPicker by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<AppInfo?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(TelColors.MenuStrip)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(14.dp),
    ) {
        // 見出し（MENUのマスと同じ色の帯）
        Box(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(TelColors.MenuTileSolid),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(folder.title, color = Color(0xFFF0EFEA), fontSize = 22.sp, modifier = Modifier.padding(start = 16.dp))
        }
        Spacer(Modifier.height(2.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.weight(1f),
        ) {
            // 便利ツールには、アプリではない「ライト」を最初に置く
            if (onTorch != null) {
                item(key = "__torch__") {
                    FolderCell(label = if (torchOn) "ライト（点灯中）" else "ライト", onClick = onTorch, onLongClick = {}) {
                        Icon(
                            if (torchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                            contentDescription = null,
                            tint = if (torchOn) Color(0xFFFFD54F) else Color(0xFFF7F6F2),
                            modifier = Modifier.size(58.dp),
                        )
                    }
                }
            }
            items(inFolder, key = { it.key }) { app ->
                FolderCell(
                    label = app.label,
                    onClick = { onLaunch(app) },
                    onLongClick = { removeTarget = app },
                ) { BitmapIcon(app.icon, 62) }
            }
            item(key = "__add__") {
                FolderCell(label = "追加", onClick = { showPicker = true }, onLongClick = {}) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFFF7F6F2), modifier = Modifier.size(56.dp))
                }
            }
        }
        if (inFolder.isEmpty()) {
            Text(
                "「追加」から、このフォルダに入れるアプリを選んでください",
                color = Color(0xFF45423C),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }

    if (showPicker) {
        val candidates = apps.filter { a -> inFolder.none { it.key == a.key } }
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("${folder.title}に追加", fontSize = 20.sp) },
            confirmButton = { TextButton(onClick = { showPicker = false }) { Text("閉じる", fontSize = 16.sp) } },
            text = {
                LazyColumn(Modifier.heightIn(max = 440.dp)) {
                    items(candidates, key = { it.key }) { app ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    store.add(folder, app)
                                    showPicker = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BitmapIcon(app.icon, 40)
                            Spacer(Modifier.width(12.dp))
                            Text(app.label, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
        )
    }

    removeTarget?.let { app ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text(app.label, fontSize = 20.sp) },
            text = { Text("${folder.title}から外しますか？（アプリ自体は消えません）", fontSize = 16.sp) },
            confirmButton = {
                TextButton(onClick = {
                    store.remove(folder, app)
                    removeTarget = null
                }) { Text("外す", fontSize = 16.sp) }
            },
            dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("キャンセル", fontSize = 16.sp) } },
        )
    }
}

@Composable
private fun FolderCell(
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(TelColors.MenuTileSolid)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.height(12.dp))
        Text(
            label,
            color = Color(0xFFF0EFEA),
            fontSize = 14.sp,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}
