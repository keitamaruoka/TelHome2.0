package com.example.telhome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MENU画面の項目。
 * appPackages があるものは、そのアプリを起動する（入っていなければ名前で探し、なければPlayストアを開く）。
 */
enum class MenuItem(
    val label: String,
    val icon: ImageVector,
    val feature: Feature? = null,
    val appPackages: List<String> = emptyList(),
    val labelHints: List<String> = emptyList(),
) {
    WALLET(
        "ウォレット", Icons.Filled.Apps,
        appPackages = listOf("com.google.android.apps.walletnfcrel"),
        labelHints = listOf("ウォレット", "Wallet"),
    ),
    BANK(
        "銀行", Icons.Filled.Apps,
        appPackages = listOf("jp.co.smbc.direct"),
        labelHints = listOf("三井住友銀行"),
    ),
    CREDIT(
        "クレジット", Icons.Filled.Apps,
        appPackages = listOf("com.smbc_card.vpass"),
        labelHints = listOf("Vpass", "三井住友カード"),
    ),
    POINT_CARD("ポイント\nカード", Icons.Filled.Apps),
    COUPON("クーポン", Icons.Filled.Apps),
    SHOPPING("ショッピング", Icons.Filled.Apps),
    TRANSPORT("交通", Icons.Filled.Apps),
    GAME("ゲーム", Icons.Filled.Apps),
    MAIL("メール", Feature.MAIL.icon, Feature.MAIL),
    BROWSER("インターネット", Feature.BROWSER.icon, Feature.BROWSER),
    APPS("アプリ", Icons.Filled.Apps),
    CAMERA("カメラ", Feature.CAMERA.icon, Feature.CAMERA),
    GALLERY("ギャラリー", Feature.GALLERY.icon, Feature.GALLERY),
    TOOLS("便利ツール", Icons.Filled.Handyman),
    CALL_SETTINGS("電話機能", Feature.CALL_SETTINGS.icon, Feature.CALL_SETTINGS),
    SETTINGS("本体設定", Feature.SETTINGS.icon, Feature.SETTINGS),
    MAPS(
        "地図", Feature.MAPS.icon, Feature.MAPS,
        appPackages = listOf("com.google.android.apps.maps"),
        labelHints = listOf("マップ", "Maps"),
    ),
    CALENDAR("スケジュール", Feature.CALENDAR.icon, Feature.CALENDAR),
    PROFILE("プロフィール", Feature.PROFILE.icon, Feature.PROFILE),
    DOCUMENTS(
        "ドキュメント\nビューワー", Icons.Filled.Description,
        // Officeファイルを見るアプリ（入っているものを上から順に探す）
        appPackages = listOf(
            "com.microsoft.office.officehubrow",   // Microsoft 365 (Office)
            "com.microsoft.office.word",
            "cn.wps.moffice_eng",                  // WPS Office
            "com.infraware.office.link",           // Polaris Office
        ),
        labelHints = listOf("Microsoft 365", "Office", "オフィス", "WPS", "Polaris"),
    ),
    ;

    companion object {
        /** MENU画面に置ける項目（長押しで選べるもの） */
        val catalog = listOf(
            MAIL, BROWSER, APPS, CAMERA, GALLERY, TOOLS,
            CALL_SETTINGS, SETTINGS, MAPS, CALENDAR, DOCUMENTS,
            WALLET, BANK, CREDIT, POINT_CARD, COUPON, SHOPPING, TRANSPORT, GAME,
        )

        /** 初期の並び（3列×4段） */
        val layout = listOf(
            CAMERA, GALLERY, APPS,
            WALLET, BANK, CREDIT,
            POINT_CARD, COUPON, SHOPPING,
            TRANSPORT, MAPS, TOOLS,
        )
    }
}

/** 決まったアプリを起動する。入っていなければアプリ名で探し、それでもなければPlayストアを開く */
object AppOpener {
    fun open(context: android.content.Context, item: MenuItem, apps: List<AppInfo>) {
        val pm = context.packageManager
        for (pkg in item.appPackages) {
            pm.getLaunchIntentForPackage(pkg)?.let { if (Launch.start(context, it)) return }
        }
        val byName = apps.firstOrNull { app -> item.labelHints.any { app.label.contains(it, ignoreCase = true) } }
        if (byName != null) {
            Launch.app(context, byName.pkg, byName.cls)
            return
        }
        val pkg = item.appPackages.firstOrNull() ?: return
        Launch.startAny(
            context,
            listOf(
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$pkg")),
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://play.google.com/store/apps/details?id=$pkg"),
                ),
            ),
        )
    }
}

/** MENU画面の並び（12マス）を保存する。null は空きマス */
class MenuLayoutStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("menu_layout", android.content.Context.MODE_PRIVATE)

    fun load(): List<MenuItem?> {
        val saved = prefs.getString("layout", null) ?: return MenuItem.layout
        val names = saved.split(",")
        if (names.size != 12) return MenuItem.layout
        return names.map { n -> MenuItem.entries.firstOrNull { it.name == n } }
    }

    fun save(list: List<MenuItem?>) {
        prefs.edit().putString("layout", list.joinToString(",") { it?.name ?: "" }).apply()
    }

    fun reset() = prefs.edit().remove("layout").apply()
}

/** MENU項目のアイコン（自作の白アイコン、なければ標準アイコン） */
@Composable
fun MenuItemIcon(item: MenuItem, size: androidx.compose.ui.unit.Dp) {
    when (item) {
        MenuItem.WALLET -> WalletGlyph(size)
        MenuItem.BANK -> BankGlyph(size)
        MenuItem.CREDIT -> CreditCardGlyph(size)
        MenuItem.POINT_CARD -> PointCardGlyph(size)
        MenuItem.COUPON -> CouponGlyph(size)
        MenuItem.SHOPPING -> ShoppingGlyph(size)
        MenuItem.TRANSPORT -> TransportGlyph(size)
        MenuItem.GAME -> GameGlyph(size)
        else -> Icon(item.icon, contentDescription = null, tint = Color(0xFFF7F6F2), modifier = Modifier.size(size))
    }
}

/**
 * MENU画面。各マスを長押しすると、そこに置く項目を自由に選べる。
 * 閉じるときは端末の「戻る」で閉じる（onClose は互換のため残している）
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun MenuScreen(onSelect: (MenuItem) -> Unit, onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { MenuLayoutStore(context) }
    var slots by remember { mutableStateOf(store.load()) }
    var editIndex by remember { mutableStateOf<Int?>(null) }

    // F-04F風：ホームと同じ白の台紙の上に、ブラウングレーのマスを細い白線で区切って並べる
    Column(
        Modifier
            .fillMaxSize()
            .background(TelColors.MenuStrip)
            .blockTouches() // 下のホームにタッチが抜けないように
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxWidth()) {
            for (row in 0 until 4) {
                if (row > 0) Spacer(Modifier.height(MenuGapLine))
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0 until 3) {
                        if (col > 0) Spacer(Modifier.width(MenuGapLine))
                        val index = row * 3 + col
                        val item = slots.getOrNull(index)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(TelColors.MenuTileSolid)
                                .combinedClickable(
                                    onClick = { if (item != null) onSelect(item) else editIndex = index },
                                    onLongClick = { editIndex = index },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (item == null) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "追加",
                                    tint = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(44.dp),
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    MenuItemIcon(item, 66.dp)
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        item.label,
                                        color = Color(0xFFF0EFEA),
                                        fontSize = 15.sp,
                                        lineHeight = 18.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- マスに置く項目を選ぶ ----
    editIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { editIndex = null },
            title = { Text("ここに置く項目", fontSize = 20.sp) },
            text = {
                LazyColumn(Modifier.heightIn(max = 460.dp)) {
                    items(MenuItem.catalog) { candidate ->
                        val placed = candidate in slots
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // すでに別のマスにある項目なら、場所を入れ替える
                                    val list = slots.toMutableList()
                                    val from = list.indexOf(candidate)
                                    if (from >= 0) list[from] = list[index]
                                    list[index] = candidate
                                    slots = list
                                    store.save(list)
                                    editIndex = null
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(44.dp).background(TelColors.MenuTileSolid),
                                contentAlignment = Alignment.Center,
                            ) { MenuItemIcon(candidate, 30.dp) }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                candidate.label.replace("\n", "") + if (placed) "（配置済み・入れ替え）" else "",
                                fontSize = 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editIndex = null }) { Text("キャンセル", fontSize = 16.sp) } },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        store.reset()
                        slots = store.load()
                        editIndex = null
                    }) { Text("初期の並び", fontSize = 15.sp) }
                    TextButton(onClick = {
                        val list = slots.toMutableList()
                        list[index] = null
                        slots = list
                        store.save(list)
                        editIndex = null
                    }) { Text("空にする", fontSize = 15.sp) }
                }
            },
        )
    }
}

/** MENU画面のマスとマスの間（細い白線） */
private val MenuGapLine = 2.dp

/** 便利ツール（電卓・アラーム・ライト） */
@Composable
fun ToolsDialog(
    torchOn: Boolean,
    onFeature: (Feature) -> Unit,
    onTorch: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
        title = { Text("便利ツール") },
        text = {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                ToolButton("電卓", { VectorBadge(Feature.CALCULATOR.icon, Feature.CALCULATOR.color, 52) }) {
                    onFeature(Feature.CALCULATOR)
                }
                ToolButton("アラーム", { VectorBadge(Feature.ALARM.icon, Feature.ALARM.color, 52) }) {
                    onFeature(Feature.ALARM)
                }
                ToolButton(if (torchOn) "ライト消灯" else "ライト", {
                    VectorBadge(
                        if (torchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                        if (torchOn) Color(0xFFE0A800) else Color(0xFF6A6F7A),
                        52,
                    )
                }, onTorch)
            }
        },
    )
}

@Composable
private fun ToolButton(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp)
    }
}

/** ホームの小タイルに割り当てるものを選ぶ */
@Composable
fun SlotPickerDialog(
    apps: List<AppInfo>,
    onPick: (Slot) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("タイルに割り当てる") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
        dismissButton = {
            Row {
                TextButton(onClick = onResetDefault) { Text("標準に戻す") }
                TextButton(onClick = { onPick(Slot.Empty) }) { Text("空にする") }
            }
        },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(apps, key = { it.pkg + "/" + it.cls }) { app ->
                    PickerRow(app.label, { BitmapIcon(app.icon, 40) }) { onPick(Slot.App(app.pkg, app.cls)) }
                }
            }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun PickerRow(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 重ねた画面の下にタッチが伝わらないようにする */
private fun Modifier.blockTouches(): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {},
    )
}
