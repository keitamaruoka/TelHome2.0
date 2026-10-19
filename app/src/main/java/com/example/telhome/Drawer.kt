@file:OptIn(ExperimentalFoundationApi::class)

package com.example.telhome

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// =====================================================================
//  データ：並び順・非表示・フォルダを保存する
// =====================================================================

val AppInfo.key: String get() = "$pkg/$cls"

sealed class DrawerEntry {
    abstract val key: String

    data class App(val app: AppInfo) : DrawerEntry() {
        override val key: String get() = app.key
    }

    data class Folder(val id: String, val name: String, val apps: List<AppInfo>) : DrawerEntry() {
        override val key: String get() = "folder:$id"
    }
}

class DrawerStore(context: Context) {
    private class FolderData(val id: String, var name: String, val apps: MutableList<String>)

    private val prefs = context.getSharedPreferences("drawer", Context.MODE_PRIVATE)
    private val order = mutableListOf<String>()
    private val hidden = mutableSetOf<String>()
    private val folders = LinkedHashMap<String, FolderData>()

    /** 変更のたびに増える。画面の再描画に使う */
    var version by mutableIntStateOf(0)
        private set

    init {
        load()
    }

    /** 一覧に表示する並び（フォルダ内と非表示のアプリは除く） */
    fun entries(apps: List<AppInfo>): List<DrawerEntry> {
        val byKey = apps.associateBy { it.key }
        val inFolder = folders.values.flatMap { it.apps }.toSet()
        val used = mutableSetOf<String>()
        val result = mutableListOf<DrawerEntry>()

        fun folderEntry(f: FolderData) =
            DrawerEntry.Folder(f.id, f.name, f.apps.mapNotNull { byKey[it] }.filter { it.key !in hidden })

        for (k in order) {
            if (k in used) continue
            if (k.startsWith("folder:")) {
                val f = folders[k.removePrefix("folder:")] ?: continue
                result += folderEntry(f)
                used += k
            } else {
                val app = byKey[k] ?: continue
                if (k in hidden || k in inFolder) continue
                result += DrawerEntry.App(app)
                used += k
            }
        }
        for (f in folders.values) {
            if ("folder:${f.id}" !in used) result += folderEntry(f)
        }
        // 新しく入れたアプリは最後に（名前順）
        for (app in apps) {
            val k = app.key
            if (k !in used && k !in hidden && k !in inFolder) result += DrawerEntry.App(app)
        }
        return result
    }

    fun hiddenApps(apps: List<AppInfo>): List<AppInfo> = apps.filter { it.key in hidden }

    /** item を target の前へ（target が null なら最後へ）。current は今表示している並び */
    fun moveBefore(item: String, target: String?, current: List<String>) {
        val list = current.toMutableList()
        list.remove(item)
        val i = if (target == null) -1 else list.indexOf(target)
        list.add(if (i < 0) list.size else i, item)
        setOrder(list)
    }

    fun hide(appKey: String) {
        hidden += appKey
        folders.values.forEach { it.apps.remove(appKey) }
        save()
    }

    fun unhide(appKey: String) {
        hidden -= appKey
        order.remove(appKey) // 再表示したアプリは一覧の最後へ
        save()
    }

    fun createFolder(name: String, current: List<String>): String {
        val id = System.currentTimeMillis().toString()
        folders[id] = FolderData(id, name, mutableListOf())
        setOrder(current + "folder:$id")
        return id
    }

    fun renameFolder(id: String, name: String) {
        folders[id]?.name = name
        save()
    }

    /** フォルダを消して、中のアプリはフォルダがあった場所に戻す */
    fun deleteFolder(id: String, current: List<String>) {
        val f = folders.remove(id) ?: return
        val list = current.toMutableList()
        val i = list.indexOf("folder:$id")
        list.remove("folder:$id")
        list.addAll(if (i < 0) list.size else i, f.apps)
        setOrder(list)
    }

    fun addToFolder(appKey: String, id: String) {
        folders.values.forEach { it.apps.remove(appKey) }
        folders[id]?.apps?.add(appKey)
        save()
    }

    /** フォルダから出して、フォルダのすぐ後ろに置く */
    fun removeFromFolder(appKey: String, id: String, current: List<String>) {
        folders[id]?.apps?.remove(appKey)
        val list = current.toMutableList()
        list.remove(appKey)
        val i = list.indexOf("folder:$id")
        list.add(if (i < 0) list.size else i + 1, appKey)
        setOrder(list)
    }

    fun moveInFolder(id: String, item: String, target: String?) {
        val apps = folders[id]?.apps ?: return
        apps.remove(item)
        val i = if (target == null) -1 else apps.indexOf(target)
        apps.add(if (i < 0) apps.size else i, item)
        save()
    }

    fun folderNames(): List<Pair<String, String>> = folders.values.map { it.id to it.name }

    private fun setOrder(list: List<String>) {
        order.clear()
        order.addAll(list)
        save()
    }

    private fun load() {
        val text = prefs.getString("data", null) ?: return
        try {
            val o = JSONObject(text)
            o.optJSONArray("order")?.let { a -> for (i in 0 until a.length()) order += a.getString(i) }
            o.optJSONArray("hidden")?.let { a -> for (i in 0 until a.length()) hidden += a.getString(i) }
            o.optJSONArray("folders")?.let { a ->
                for (i in 0 until a.length()) {
                    val f = a.getJSONObject(i)
                    val list = mutableListOf<String>()
                    val fa = f.getJSONArray("apps")
                    for (j in 0 until fa.length()) list += fa.getString(j)
                    val id = f.getString("id")
                    folders[id] = FolderData(id, f.getString("name"), list)
                }
            }
        } catch (e: JSONException) {
            order.clear(); hidden.clear(); folders.clear()
        }
    }

    private fun save() {
        val o = JSONObject()
        o.put("order", JSONArray(order))
        o.put("hidden", JSONArray(hidden.toList()))
        val fa = JSONArray()
        for (f in folders.values) {
            fa.put(JSONObject().put("id", f.id).put("name", f.name).put("apps", JSONArray(f.apps)))
        }
        o.put("folders", fa)
        prefs.edit().putString("data", o.toString()).apply()
        version++
    }
}

// =====================================================================
//  画面：アプリ一覧
// =====================================================================

private data class NameRequest(val folderId: String?, val initial: String, val addApp: String? = null)

private val CellLabel = 13.sp

/** 「アプリ」から開くアプリ一覧。長押しで移動・フォルダ・非表示 */
@Composable
fun AppDrawer(apps: List<AppInfo>, onLaunch: (AppInfo) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember { DrawerStore(context) }
    val version = store.version
    val entries = remember(apps, version) { store.entries(apps) }
    val keys = entries.map { it.key }

    var moving by remember { mutableStateOf<String?>(null) }          // 一覧で移動中
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var folderMoving by remember { mutableStateOf<String?>(null) }    // フォルダ内で移動中
    var menuFor by remember { mutableStateOf<DrawerEntry?>(null) }
    var folderAppMenu by remember { mutableStateOf<AppInfo?>(null) }
    var nameRequest by remember { mutableStateOf<NameRequest?>(null) }
    var pickFolderFor by remember { mutableStateOf<String?>(null) }
    var showHidden by remember { mutableStateOf(false) }

    val openFolder = openFolderId?.let { id -> entries.firstOrNull { it is DrawerEntry.Folder && it.id == id } as? DrawerEntry.Folder }

    BackHandler(enabled = moving != null || openFolderId != null) {
        when {
            folderMoving != null -> folderMoving = null
            openFolderId != null -> openFolderId = null
            else -> moving = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(TelColors.Background)
            .blockTouches(),
    ) {
        Header(
            title = "アプリ一覧",
            onClose = onClose,
            actions = {
                HeaderButton(Icons.Filled.CreateNewFolder, "フォルダ作成") {
                    nameRequest = NameRequest(folderId = null, initial = "")
                }
                HeaderButton(Icons.Filled.VisibilityOff, "非表示のアプリ") { showHidden = true }
            },
        )
        if (moving != null) {
            MoveBanner(
                onToEnd = { store.moveBefore(moving!!, null, keys); moving = null },
                onCancel = { moving = null },
            )
        }
        if (apps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("読み込み中…", color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(entries, key = { it.key }) { entry ->
                    val selected = entry.key == moving
                    Cell(
                        selected = selected,
                        onClick = {
                            val m = moving
                            when {
                                m != null -> {
                                    if (m != entry.key) store.moveBefore(m, entry.key, keys)
                                    moving = null
                                }
                                entry is DrawerEntry.App -> onLaunch(entry.app)
                                entry is DrawerEntry.Folder -> openFolderId = entry.id
                            }
                        },
                        onLongClick = { if (moving == null) menuFor = entry },
                    ) {
                        when (entry) {
                            is DrawerEntry.App -> AppCellContent(entry.app)
                            is DrawerEntry.Folder -> FolderCellContent(entry)
                        }
                    }
                }
            }
        }
    }

    // ---- フォルダの中身 ----
    if (openFolder != null) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xF01B1F24))
                .blockTouches(),
        ) {
            Header(
                title = openFolder.name,
                onClose = { folderMoving = null; openFolderId = null },
                actions = {
                    TextButton(onClick = { nameRequest = NameRequest(openFolder.id, openFolder.name) }) {
                        Text("名前変更", color = Color.White, fontSize = 16.sp)
                    }
                },
            )
            if (folderMoving != null) {
                MoveBanner(
                    onToEnd = { store.moveInFolder(openFolder.id, folderMoving!!, null); folderMoving = null },
                    onCancel = { folderMoving = null },
                )
            }
            if (openFolder.apps.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "フォルダは空です\n一覧でアプリを長押しして「フォルダに入れる」を選んでください",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(openFolder.apps, key = { it.key }) { app ->
                        Cell(
                            selected = app.key == folderMoving,
                            onClick = {
                                val m = folderMoving
                                if (m != null) {
                                    if (m != app.key) store.moveInFolder(openFolder.id, m, app.key)
                                    folderMoving = null
                                } else {
                                    onLaunch(app)
                                }
                            },
                            onLongClick = { if (folderMoving == null) folderAppMenu = app },
                        ) { AppCellContent(app) }
                    }
                }
            }
        }
        // フォルダ内のアプリを長押ししたとき
        folderAppMenu?.let { app ->
            ActionDialog(
                title = app.label,
                actions = listOf(
                    "移動する" to { folderMoving = app.key },
                    "フォルダから出す" to { store.removeFromFolder(app.key, openFolder.id, keys) },
                    "非表示にする" to { store.hide(app.key) },
                    "プロパティ（アプリ情報）" to { AppActions.openDetails(context, app.pkg) },
                    "アンインストール" to { AppActions.uninstall(context, app.pkg) },
                ),
                onDismiss = { folderAppMenu = null },
            )
        }
    }

    // ---- 一覧で長押ししたとき ----
    menuFor?.let { entry ->
        when (entry) {
            is DrawerEntry.App -> ActionDialog(
                title = entry.app.label,
                actions = listOf(
                    "移動する" to { moving = entry.key },
                    "フォルダに入れる" to { pickFolderFor = entry.key },
                    "非表示にする" to { store.hide(entry.key) },
                    "プロパティ（アプリ情報）" to { AppActions.openDetails(context, entry.app.pkg) },
                    "アンインストール" to { AppActions.uninstall(context, entry.app.pkg) },
                ),
                onDismiss = { menuFor = null },
            )
            is DrawerEntry.Folder -> ActionDialog(
                title = entry.name,
                actions = listOf(
                    "開く" to { openFolderId = entry.id },
                    "移動する" to { moving = entry.key },
                    "名前を変更" to { nameRequest = NameRequest(entry.id, entry.name) },
                    "フォルダを削除（中のアプリは一覧に戻ります）" to { store.deleteFolder(entry.id, keys) },
                ),
                onDismiss = { menuFor = null },
            )
        }
    }

    // ---- 入れるフォルダを選ぶ ----
    pickFolderFor?.let { appKey ->
        val folders = store.folderNames()
        ActionDialog(
            title = "どのフォルダに入れますか",
            actions = folders.map { (id, name) -> name to { store.addToFolder(appKey, id) } } +
                ("＋ 新しいフォルダを作る" to { nameRequest = NameRequest(null, "", addApp = appKey) }),
            onDismiss = { pickFolderFor = null },
        )
    }

    // ---- フォルダ名の入力 ----
    nameRequest?.let { req ->
        NameDialog(
            title = if (req.folderId == null) "新しいフォルダ" else "フォルダ名を変更",
            initial = req.initial,
            onDone = { name ->
                if (req.folderId == null) {
                    val id = store.createFolder(name, keys)
                    req.addApp?.let { store.addToFolder(it, id) }
                } else {
                    store.renameFolder(req.folderId, name)
                }
                nameRequest = null
            },
            onDismiss = { nameRequest = null },
        )
    }

    // ---- 非表示のアプリ ----
    if (showHidden) {
        val hiddenApps = store.hiddenApps(apps)
        AlertDialog(
            onDismissRequest = { showHidden = false },
            title = { Text("非表示のアプリ", fontSize = 20.sp) },
            confirmButton = { TextButton(onClick = { showHidden = false }) { Text("閉じる", fontSize = 16.sp) } },
            text = {
                if (hiddenApps.isEmpty()) {
                    Text("非表示のアプリはありません", fontSize = 16.sp)
                } else {
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(hiddenApps, key = { it.key }) { app ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BitmapIcon(app.icon, 36)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    app.label,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { store.unhide(app.key) }) { Text("表示する", fontSize = 15.sp) }
                            }
                        }
                    }
                }
            },
        )
    }
}

// =====================================================================
//  部品
// =====================================================================

@Composable
private fun Header(title: String, onClose: () -> Unit, actions: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(TelColors.LogHeader)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 22.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
        Box(
            Modifier.size(52.dp).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "閉じる", tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun MoveBanner(onToEnd: () -> Unit, onCancel: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFE0A800))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "移動先のアイコンをタップ\n（その前に入ります）",
            color = Color(0xFF1B1F24),
            fontSize = 15.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onToEnd) { Text("最後へ", color = Color(0xFF1B1F24), fontSize = 16.sp) }
        TextButton(onClick = onCancel) { Text("やめる", color = Color(0xFF1B1F24), fontSize = 16.sp) }
    }
}

@Composable
private fun Cell(
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (selected) Modifier.border(3.dp, Color(0xFFE0A800), RoundedCornerShape(8.dp)) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun AppCellContent(app: AppInfo) {
    BitmapIcon(app.icon, size = 56)
    Spacer(Modifier.height(4.dp))
    CellText(app.label)
}

@Composable
private fun FolderCellContent(folder: DrawerEntry.Folder) {
    Box(
        Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF5A606B)),
        contentAlignment = Alignment.Center,
    ) {
        // 中のアプリを最大4つ、小さく並べる
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            for (r in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (c in 0 until 2) {
                        val app = folder.apps.getOrNull(r * 2 + c)
                        if (app != null) {
                            BitmapIcon(app.icon, 22)
                        } else {
                            Spacer(Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    CellText(folder.name)
}

@Composable
private fun CellText(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = CellLabel,
        lineHeight = 16.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}

/** 長押しメニュー。項目を選ぶと実行してから閉じる */
@Composable
private fun ActionDialog(title: String, actions: List<Pair<String, () -> Unit>>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("キャンセル", fontSize = 16.sp) } },
        text = {
            Column {
                for ((label, action) in actions) {
                    Text(
                        label,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                action()
                            }
                            .padding(vertical = 14.dp),
                    )
                }
            }
        },
    )
}

@Composable
private fun NameDialog(title: String, initial: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 20.sp) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(20) },
                singleLine = true,
                label = { Text("フォルダ名") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onDone(text.trim()) }, enabled = text.isNotBlank()) {
                Text("決定", fontSize = 16.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル", fontSize = 16.sp) } },
    )
}

/** 重ねた画面の下にタッチが伝わらないようにする */
@Composable
private fun Modifier.blockTouches(): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = {},
)

/** アプリのアンインストールと、アプリ情報（プロパティ）画面 */
object AppActions {
    fun uninstall(context: Context, pkg: String) {
        Launch.startAny(
            context,
            listOf(
                android.content.Intent(android.content.Intent.ACTION_DELETE, android.net.Uri.parse("package:$pkg")),
                android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:$pkg"),
                ),
            ),
        )
    }

    fun openDetails(context: Context, pkg: String) {
        Launch.startAny(
            context,
            listOf(
                android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:$pkg"),
                ),
            ),
        )
    }
}
