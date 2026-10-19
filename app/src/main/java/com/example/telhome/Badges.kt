package com.example.telhome

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telecom.TelecomManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 通知ドット（通知バッジ）用。
 * 端末の「通知へのアクセス」をこのアプリに許可すると、どのアプリに未読の通知があるかが分かる。
 * 通知の中身は読まず、通知を出しているアプリの名前（パッケージ名）だけを使う。
 */
class BadgeService : NotificationListenerService() {

    override fun onListenerConnected() = refresh()
    override fun onListenerDisconnected() { counts.value = emptyMap() }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val active = try { activeNotifications } catch (e: Exception) { null } ?: return
        counts.value = active
            .filter { it.isClearable && !it.isOngoing }   // 音楽再生中などの常駐通知は数えない
            .groupBy { it.packageName }
            .mapValues { (_, list) -> countOf(list) }
            .filterValues { it > 0 }
    }

    /**
     * 1つのアプリの通知の件数。
     * まとめ表示用の通知（グループの見出し）は数えず、アプリが「件数」を付けていればそれを使う。
     */
    private fun countOf(list: List<StatusBarNotification>): Int {
        val isSummary = { n: StatusBarNotification ->
            (n.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0
        }
        val items = list.filterNot(isSummary)
        val target = items.ifEmpty { list }
        return target.sumOf { n -> n.notification.number.takeIf { it > 0 } ?: 1 }
    }

    companion object {
        private val counts = MutableStateFlow<Map<String, Int>>(emptyMap())

        /** 通知が出ているアプリのパッケージ名と、その件数 */
        val badged: StateFlow<Map<String, Int>> = counts.asStateFlow()

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            val me = ComponentName(context, BadgeService::class.java).flattenToString()
            return flat.split(":").any { it.equals(me, ignoreCase = true) }
        }

        /** 「通知へのアクセス」の設定画面を開く */
        fun openSettings(context: Context) {
            Launch.startAny(
                context,
                listOf(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                    Intent(Settings.ACTION_SETTINGS),
                ),
            )
        }

        /** 電話タイルのドット用：標準の電話アプリ（不在着信の通知を出すアプリ） */
        fun dialerPackage(context: Context): String? = try {
            (context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager).defaultDialerPackage
        } catch (e: Exception) {
            null
        }
    }
}

/** 中身（アイコン）の右上に、件数入りの通知バッジを重ねる（0件なら何も出さない） */
@Composable
fun WithBadge(count: Int, dotSize: Dp = 22.dp, content: @Composable () -> Unit) {
    Box {
        content()
        if (count > 0) {
            val text = if (count > 99) "99+" else count.toString()
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = dotSize / 2.5f, y = -dotSize / 2.5f)
                    .defaultMinSize(minWidth = dotSize, minHeight = dotSize)
                    .background(Color(0xFFE53935), RoundedCornerShape(50))
                    .border(2.dp, Color.White, RoundedCornerShape(50))
                    .padding(horizontal = if (text.length > 1) 5.dp else 0.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (dotSize.value * 0.58f).sp,
                    style = TextStyle(
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
}
