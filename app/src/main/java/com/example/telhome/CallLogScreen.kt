package com.example.telhome

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/** 左右にスワイプして出てくる履歴画面。outgoing=false で着信、true で発信 */
@Composable
fun CallLogScreen(outgoing: Boolean, refreshKey: Int) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(CallLogRepo.hasPermission(context)) }
    var groups by remember { mutableStateOf<List<CallGroup>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        granted = result[Manifest.permission.READ_CALL_LOG] == true || CallLogRepo.hasPermission(context)
    }

    LaunchedEffect(refreshKey, granted) {
        granted = CallLogRepo.hasPermission(context)
        if (granted) {
            groups = withContext(Dispatchers.IO) { CallLogRepo.load(context, outgoing) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(TelColors.LogPageSolid),
    ) {
        // ヘッダー
        Row(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF2B7391), TelColors.LogHeader)))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.History, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(if (outgoing) "発信履歴" else "着信履歴", color = Color.White, fontSize = 22.sp)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val list = groups
            when {
                !granted -> PermissionRequest {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE),
                    )
                }
                list == null -> Unit
                list.isEmpty() -> Text(
                    "履歴はありません",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(list, key = { it.number + it.latestDate }) { g ->
                        CallRow(g, outgoing)
                    }
                }
            }
        }

        // 下部バー（電話帳ボタン）
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(TelColors.BottomBar)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .clickable { Feature.CONTACTS.launch(context) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Contacts, contentDescription = null, tint = Color(0xFF333333), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("電話帳", color = Color(0xFF333333), fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun CallRow(g: CallGroup, outgoing: Boolean) {
    val context = LocalContext.current
    val missed = !outgoing && g.latestType != CallLog.Calls.INCOMING_TYPE
    val displayName = when {
        g.name != null -> "${g.name}さん"
        g.number.isBlank() || g.number == "-1" || g.number == "-2" -> "非通知"
        else -> g.number
    }
    val dialable = g.number.isNotBlank() && !g.number.startsWith("-")

    Row(
        Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(TelColors.LogItem)
            .clickable(enabled = dialable) {
                // 行タップは発信前に番号を確認できるダイヤル画面へ
                Launch.start(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${g.number}")))
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(displayName, color = Color.White, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, tint) = when {
                    outgoing -> Icons.AutoMirrored.Filled.CallMade to Color.White
                    missed -> Icons.AutoMirrored.Filled.CallMissed to TelColors.Missed
                    else -> Icons.AutoMirrored.Filled.CallReceived to TelColors.CallAccent
                }
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = (if (g.count > 1) "${g.count}件  " else "") + formatDate(g.latestDate),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                )
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 12.dp).background(TelColors.LogDivider))
        Box(
            Modifier
                .width(76.dp)
                .fillMaxHeight()
                .clickable(enabled = dialable) { call(context, g.number) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Call, contentDescription = "発信", tint = TelColors.CallAccent, modifier = Modifier.size(38.dp))
        }
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "履歴を表示するには\n通話履歴へのアクセスを許可してください",
            color = Color.White,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = TelColors.CallAccent),
        ) { Text("許可する", fontSize = 18.sp) }
    }
}

private fun call(context: android.content.Context, number: String) {
    val action = if (CallLogRepo.canCallDirectly(context)) Intent.ACTION_CALL else Intent.ACTION_DIAL
    Launch.start(context, Intent(action, Uri.parse("tel:$number")))
}

/** 例：1/17（金）AM 11:12 */
private fun formatDate(millis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    val week = arrayOf("日", "月", "火", "水", "木", "金", "土")[c.get(Calendar.DAY_OF_WEEK) - 1]
    val hour = c.get(Calendar.HOUR_OF_DAY)
    val ampm = if (hour < 12) "AM" else "PM"
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "%d/%d（%s）%s %02d:%02d".format(
        c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), week, ampm, h12, c.get(Calendar.MINUTE),
    )
}
