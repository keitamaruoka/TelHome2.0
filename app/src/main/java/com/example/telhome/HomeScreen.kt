@file:OptIn(ExperimentalFoundationApi::class)

package com.example.telhome

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Gap = 2.dp

/** 中央のホーム画面 */
@Composable
fun HomeScreen(
    slots: List<Slot>,
    apps: List<AppInfo>,
    onSlotLongPress: (Int) -> Unit,
    onMenu: () -> Unit,
) {
    val context = LocalContext.current
    // 通知ドット：電話は標準の電話アプリ（不在着信）、メールは受信トレイを開くメールアプリの通知で判断
    val badged by BadgeService.badged.collectAsState()
    val dialerPkg = remember { BadgeService.dialerPackage(context) }
    val mailPkg = remember(apps) { MailApp.findMailPackage(context) }
    val phoneBadge = (dialerPkg?.let { badged[it] } ?: 0) + (badged["com.android.server.telecom"] ?: 0)
    val mailBadge = mailPkg?.let { badged[it] } ?: 0
    Column(
        Modifier
            .fillMaxSize()
            .background(TelColors.MenuStrip), // タイルのすき間とMENU周りを同じ白で一続きに
    ) {
        // 検索バーの後ろだけグレーの帯にする（F-04F風）
        Box(
            Modifier
                .fillMaxWidth()
                .background(TelColors.SearchBand)
                .padding(horizontal = 4.dp, vertical = 7.dp),
        ) {
            SearchBar(apps)
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
        ) {

            // 大きいタイル：電話（左）、メール・電話帳（右）
            Row(Modifier.weight(1f).fillMaxWidth()) {
                BigTile(
                    feature = Feature.DIALER,
                    badge = phoneBadge,
                    top = TelColors.PhoneTop,
                    bottom = TelColors.PhoneBottom,
                    iconSize = 90,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Spacer(Modifier.width(Gap))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    BigTile(
                        feature = Feature.MAIL,
                        badge = mailBadge,
                        top = TelColors.MailTop,
                        bottom = TelColors.MailBottom,
                        iconSize = 70,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Gap))
                    BigTile(
                        feature = Feature.CONTACTS,
                        top = TelColors.ContactsTop,
                        bottom = TelColors.ContactsBottom,
                        iconSize = 70,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(Gap))

            // 小さいタイル 4列×3段（長押しで割り当て変更）
            Column(Modifier.weight(1.2f).fillMaxWidth()) {
                for (row in 0 until SlotStore.ROWS) {
                    if (row > 0) Spacer(Modifier.height(Gap))
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until SlotStore.COLUMNS) {
                            if (col > 0) Spacer(Modifier.width(Gap))
                            val index = row * SlotStore.COLUMNS + col
                            SmallSlotTile(
                                slot = slots.getOrElse(index) { Slot.Empty },
                                apps = apps,
                                onClick = { slot ->
                                    when (slot) {
                                        is Slot.Builtin -> slot.feature.launch(context)
                                        is Slot.App -> Launch.app(context, slot.pkg, slot.cls)
                                        Slot.Empty -> onSlotLongPress(index)
                                    }
                                },
                                onLongClick = { onSlotLongPress(index) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                badged = badged,
                            )
                        }
                    }
                }
            }
        }
        MenuStrip(onMenu)
    }
}

/** 検索バーの白文字（明るい背景でも読めるよう薄い影付き） */
/** 検索バーの「Google」表示（F-04F風：明るめの白・細めのセリフ体・薄い影） */
private val SearchLogoStyle = TextStyle(
    color = Color(0xFFF4F5F7),
    fontSize = 21.sp,
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Normal,
    shadow = Shadow(color = Color(0x80000000), offset = Offset(0f, 1.5f), blurRadius = 2.5f),
)

private val SearchTextStyle = TextStyle(
    color = Color.White,
    fontSize = 21.sp,
    fontWeight = FontWeight.Bold,
    shadow = Shadow(color = Color(0x99000000), offset = Offset(0f, 1.5f), blurRadius = 3f),
)

/** 上部の検索バー：入力して確定すると、端末のメインブラウザで Google 検索する */
@Composable
private fun SearchBar(apps: List<AppInfo>) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }

    fun submit() {
        WebSearch.search(context, query.trim())
        query = ""
        keyboard?.hide()
        focus.clearFocus()
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            // F-04F風：バーの下と周りにやわらかい影、下辺に濃い線
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(4.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(TelColors.SearchBarTop, TelColors.SearchBar)))
            .drawWithContent {
                drawContent()
                val px = 1.dp.toPx()
                // 上辺のハイライトと下辺の影の線
                drawRect(Color.White.copy(alpha = 0.35f), size = androidx.compose.ui.geometry.Size(size.width, px))
                drawRect(
                    Color(0xFF5D6773),
                    topLeft = Offset(0f, size.height - px * 2),
                    size = androidx.compose.ui.geometry.Size(size.width, px * 2),
                )
            }
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text("Google", style = SearchLogoStyle)
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = SearchTextStyle,
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable { submit() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Search, contentDescription = "検索", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun BigTile(
    feature: Feature,
    top: Color,
    bottom: Color,
    iconSize: Int,
    modifier: Modifier,
    badge: Int = 0,
) {
    val context = LocalContext.current
    Box(
        modifier
            .tileSurface(top, bottom, texture = 0.16f, sheen = 0.08f)
            .clickable { feature.launch(context) },
        contentAlignment = Alignment.Center,
    ) {
        // メール・電話帳は、文字の位置はそのままで、アイコンだけ少し下げる
        val iconShift = Modifier.offset(y = 10.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(if (feature == Feature.DIALER) Modifier else iconShift) {
                WithBadge(count = badge, dotSize = 26.dp) {
                    when (feature) {
                        Feature.DIALER -> PhoneGlyph(iconSize.dp)
                        Feature.MAIL -> MailGlyph(iconSize.dp)
                        Feature.CONTACTS -> ContactsGlyph(iconSize.dp)
                        else -> Icon(feature.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(feature.label, color = Color.White, fontSize = 22.sp)
        }
    }
}

@Composable
private fun SmallSlotTile(
    slot: Slot,
    apps: List<AppInfo>,
    onClick: (Slot) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
    badged: Map<String, Int> = emptyMap(),
) {
    // アンインストール済みのアプリは空き枠として表示
    val app = (slot as? Slot.App)?.let { s -> apps.firstOrNull { it.pkg == s.pkg && it.cls == s.cls } }
    val effective = if (slot is Slot.App && app == null && apps.isNotEmpty()) Slot.Empty else slot
    val empty = effective == Slot.Empty

    Box(
        modifier
            .then(
                if (empty) {
                    Modifier.tileSurface(TelColors.SmallTileEmptyTop, TelColors.SmallTileEmpty, texture = 0.06f, sheen = 0.05f)
                } else {
                    Modifier.tileSurface(TelColors.SmallTileTop, TelColors.SmallTile, texture = 0.06f, sheen = 0.07f)
                },
            )
            .combinedClickable(onClick = { onClick(effective) }, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            app != null ->
                TileContent(label = app.label) {
                    WithBadge(count = badged[app.pkg] ?: 0) { BitmapIcon(app.icon, 54) }
                }
            effective is Slot.App -> Unit // アプリ一覧の読み込み待ち
            else ->
                Icon(Icons.Filled.Add, contentDescription = "追加", tint = Color.White.copy(alpha = 0.18f), modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun TileContent(label: String, icon: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
    ) {
        icon()
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun VectorBadge(icon: ImageVector, color: Color, size: Int = 34) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 6).dp))
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.85f), color))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size((size * 0.62f).dp))
    }
}

@Composable
fun BitmapIcon(bitmap: ImageBitmap, size: Int = 36) {
    Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(size.dp))
}

@Composable
private fun MenuStrip(onMenu: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(top = 2.dp), // ボタンをタイルの近く（上寄り）に置く
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .width(150.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TelColors.MenuButton)
                .clickable(onClick = onMenu),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "MENU",
                // 横幅はそのままで、文字の高さだけ少し低くする
                modifier = Modifier.graphicsLayer(scaleY = 0.85f),
                // 文字の上下の余白を取り除いて、ボタンのちょうど真ん中に置く
                style = TextStyle(
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 27.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both,
                    ),
                ),
            )
        }
    }
}
