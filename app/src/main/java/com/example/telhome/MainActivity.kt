package com.example.telhome

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    /** 画面に戻るたびに増やして、履歴やアプリ一覧を読み直す */
    private val resumeTick = mutableIntStateOf(0)

    /** ホームボタンが押されたら増やして、ホーム（中央）に戻す */
    private val homeSignal = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                TelHomeApp(resumeTick.intValue, homeSignal.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeSignal.intValue++
    }
}

private enum class Overlay { NONE, MENU, DRAWER, FOLDER }

@Composable
private fun TelHomeApp(resumeTick: Int, homeSignal: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = 1) { 3 }

    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var showTools by remember { mutableStateOf(false) }
    var openFolder by remember { mutableStateOf(MenuFolder.POINT) }
    var pickerSlot by remember { mutableStateOf<Int?>(null) }

    val store = remember { SlotStore(context) }
    var slots by remember { mutableStateOf(store.load()) }

    val apps by produceState(initialValue = emptyList<AppInfo>(), resumeTick) {
        value = withContext(Dispatchers.IO) { AppRepo.loadApps(context) }
    }

    val torch = remember { Torch(context) }
    DisposableEffect(Unit) { onDispose { torch.release() } }

    // ドキュメントビューワー：ファイルを選んで対応アプリで開く
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val view = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Launch.startAny(context, listOf(view))
        }
    }

    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            overlay = Overlay.NONE
            showTools = false
            pickerSlot = null
            pager.animateScrollToPage(1)
        }
    }

    BackHandler {
        when {
            showTools -> showTools = false
            overlay == Overlay.DRAWER || overlay == Overlay.FOLDER -> overlay = Overlay.MENU
            overlay == Overlay.MENU -> overlay = Overlay.NONE
            pager.currentPage != 1 -> scope.launch { pager.animateScrollToPage(1) }
            else -> Unit // ホームでは何もしない
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(TelColors.Background)
            .systemBarsPadding(),
    ) {
        // 左：着信履歴 / 中央：ホーム / 右：発信履歴
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> CallLogScreen(outgoing = false, refreshKey = resumeTick)
                1 -> HomeScreen(
                    slots = slots,
                    apps = apps,
                    onSlotLongPress = { pickerSlot = it },
                    onMenu = { overlay = Overlay.MENU },
                )
                else -> CallLogScreen(outgoing = true, refreshKey = resumeTick)
            }
        }

        when (overlay) {
            Overlay.MENU -> MenuScreen(
                onSelect = { item ->
                    when (item) {
                        MenuItem.APPS -> overlay = Overlay.DRAWER
                        MenuItem.TOOLS -> { openFolder = MenuFolder.TOOLS; overlay = Overlay.FOLDER }
                        MenuItem.POINT_CARD -> { openFolder = MenuFolder.POINT; overlay = Overlay.FOLDER }
                        MenuItem.COUPON -> { openFolder = MenuFolder.COUPON; overlay = Overlay.FOLDER }
                        MenuItem.SHOPPING -> { openFolder = MenuFolder.SHOPPING; overlay = Overlay.FOLDER }
                        MenuItem.TRANSPORT -> { openFolder = MenuFolder.TRANSPORT; overlay = Overlay.FOLDER }
                        MenuItem.GAME -> { openFolder = MenuFolder.GAME; overlay = Overlay.FOLDER }
                        else -> if (item.appPackages.isNotEmpty()) {
                            AppOpener.open(context, item, apps)
                        } else {
                            item.feature?.launch(context)
                        }
                    }
                },
                onClose = { overlay = Overlay.NONE },
            )
            Overlay.DRAWER -> AppDrawer(
                apps = apps,
                onLaunch = { Launch.app(context, it.pkg, it.cls) },
                onClose = { overlay = Overlay.MENU },
            )
            Overlay.FOLDER -> MenuFolderScreen(
                folder = openFolder,
                apps = apps,
                onLaunch = { Launch.app(context, it.pkg, it.cls) },
            )
            Overlay.NONE -> Unit
        }
    }

    if (showTools) {
        ToolsDialog(
            torchOn = torch.isOn,
            onFeature = { it.launch(context) },
            onTorch = { torch.toggle() },
            onDismiss = { showTools = false },
        )
    }

    // 通知ドットを使うための「通知へのアクセス」許可のお願い（許可されるまで、起動ごとに1回）
    val badgePrefs = remember { context.getSharedPreferences("badge", Context.MODE_PRIVATE) }
    var askBadge by remember {
        mutableStateOf(!BadgeService.isEnabled(context) && !badgePrefs.getBoolean("never_ask", false))
    }
    LaunchedEffect(resumeTick) {
        if (BadgeService.isEnabled(context)) askBadge = false
    }
    if (askBadge) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { askBadge = false },
            title = { androidx.compose.material3.Text("通知ドットの表示") },
            text = {
                androidx.compose.material3.Text(
                    "ホームの電話・メール・アプリに通知ドットを出すには、" +
                        "次の画面で「テレフォンホーム」の通知へのアクセスをオンにしてください。\n" +
                        "（通知の内容は読まず、どのアプリに通知があるかだけを使います）",
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    askBadge = false
                    BadgeService.openSettings(context)
                }) { androidx.compose.material3.Text("設定を開く") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    askBadge = false
                    badgePrefs.edit().putBoolean("never_ask", true).apply()
                }) { androidx.compose.material3.Text("使わない") }
            },
        )
    }

    pickerSlot?.let { index ->
        SlotPickerDialog(
            apps = apps,
            onPick = { slot ->
                store.save(index, slot)
                slots = store.load()
                pickerSlot = null
            },
            onResetDefault = {
                store.reset(index)
                slots = store.load()
                pickerSlot = null
            },
            onDismiss = { pickerSlot = null },
        )
    }
}

/** カメラのライト（懐中電灯）。権限は不要。 */
private class Torch(private val context: Context) {
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String? = try {
        manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: Exception) {
        null
    }

    var isOn by mutableStateOf(false)
        private set

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) isOn = enabled
        }
    }

    init {
        if (cameraId != null) manager.registerTorchCallback(callback, null)
    }

    fun toggle() {
        val id = cameraId
        if (id == null) {
            Toast.makeText(context, "この端末にはライトがありません", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            manager.setTorchMode(id, !isOn)
        } catch (e: Exception) {
            Toast.makeText(context, "ライトを使用できません", Toast.LENGTH_SHORT).show()
        }
    }

    fun release() {
        if (cameraId != null) manager.unregisterTorchCallback(callback)
        if (isOn && cameraId != null) {
            try { manager.setTorchMode(cameraId, false) } catch (_: Exception) {}
        }
    }
}
