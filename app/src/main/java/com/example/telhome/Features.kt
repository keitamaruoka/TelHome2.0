package com.example.telhome

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** 配色（F-04F 実機画面の色を拾って合わせたもの） */
object TelColors {
    val Background = Color(0xFF1B1F24)
    val PhoneTop = Color(0xFF31687F)
    val PhoneBottom = Color(0xFF1D4152)
    val MailTop = Color(0xFF6A3A48)
    val MailBottom = Color(0xFF4E1220)
    val ContactsTop = Color(0xFF62766D)
    val ContactsBottom = Color(0xFF445840)
    val SmallTileTop = Color(0xFF5A616B)
    val SmallTile = Color(0xFF444851)
    val SmallTileEmptyTop = Color(0xFF50565F)
    val SmallTileEmpty = Color(0xFF3D4049)
    val HomeBgTop = Color(0xFF262B31)
    val SearchBarTop = Color(0xFFB0B4BD)
    val SearchBar = Color(0xFFA7ABB4)
    val MenuStrip = Color(0xFFCBCED3)
    val SearchBand = Color(0xFF656D78)   // 検索バーの後ろの帯
    val MenuButton = Color(0xFF48433D)
    val LogHeader = Color(0xFF215D75)
    val LogPageTop = Color(0xFF34444B)
    val LogPage = Color(0xFF8E949B)
    val LogPageSolid = Color(0xFF4E575F) // 履歴画面の背景（単色・これまでのグラデーションの中間色）
    val LogItem = Color(0x52000000)   // 半透明の黒（約32%）
    val LogDivider = Color(0x33FFFFFF)
    val CallAccent = Color(0xFF35A7CC)
    val Missed = Color(0xFFE0455B)
    val BottomBar = Color(0xFFE6E7E9)
    val MenuGap = Color(0xFF6E6F6A)
    val MenuTileTop = Color(0xFF524E47)
    val MenuTile = Color(0xFF45423C)
    val MenuTileSolid = Color(0xFF4B4842)   // MENU画面のマス（単色）
}

/** 端末の標準アプリを呼び出す機能。インテントは上から順に試す。 */
enum class Feature(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val intents: () -> List<Intent>,
) {
    DIALER("電話", Icons.Filled.Dialpad, Color(0xFF2F7E99), {
        listOf(Intent(Intent.ACTION_DIAL))
    }),
    MAIL("メール", Icons.Filled.Email, Color(0xFF8A2A3A), {
        listOf(category(Intent.CATEGORY_APP_EMAIL), Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")))
    }),
    CONTACTS("電話帳", Icons.Filled.Contacts, Color(0xFF5C6D65), {
        listOf(category(Intent.CATEGORY_APP_CONTACTS), Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
    }),
    MESSAGES("メッセージ", Icons.Filled.Sms, Color(0xFF6DA83B), {
        listOf(category(Intent.CATEGORY_APP_MESSAGING), Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))
    }),
    BROWSER("インターネット", Icons.Filled.Language, Color(0xFF3B78C2), {
        listOf(category(Intent.CATEGORY_APP_BROWSER), Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/")))
    }),
    CAMERA("カメラ", Icons.Filled.PhotoCamera, Color(0xFF555B66), {
        listOf(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
    }),
    GALLERY("ギャラリー", Icons.Filled.PhotoLibrary, Color(0xFF3E8E9C), {
        listOf(category(Intent.CATEGORY_APP_GALLERY), Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
    }),
    CALENDAR("スケジュール", Icons.Filled.CalendarMonth, Color(0xFFC9483E), {
        listOf(
            category(Intent.CATEGORY_APP_CALENDAR),
            Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()),
        )
    }),
    MAPS("地図", Icons.Filled.Map, Color(0xFF4F9A5A), {
        listOf(category(Intent.CATEGORY_APP_MAPS), Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")))
    }),
    MUSIC("ミュージック", Icons.Filled.MusicNote, Color(0xFFB0558F), {
        listOf(category(Intent.CATEGORY_APP_MUSIC))
    }),
    CALCULATOR("電卓", Icons.Filled.Calculate, Color(0xFF6A6F7A), {
        listOf(category(Intent.CATEGORY_APP_CALCULATOR))
    }),
    ALARM("アラーム", Icons.Filled.Alarm, Color(0xFFCB7A2E), {
        listOf(Intent(AlarmClock.ACTION_SHOW_ALARMS))
    }),
    SETTINGS("本体設定", Icons.Filled.Settings, Color(0xFF6A6F7A), {
        listOf(Intent(Settings.ACTION_SETTINGS))
    }),
    CALL_SETTINGS("電話機能", Icons.Filled.PhoneInTalk, Color(0xFF2F7E99), {
        listOf(Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS), Intent(Intent.ACTION_DIAL))
    }),
    PROFILE("プロフィール", Icons.Filled.AccountBox, Color(0xFF5C6D65), {
        listOf(
            Intent(Intent.ACTION_VIEW, ContactsContract.Profile.CONTENT_URI),
            category(Intent.CATEGORY_APP_CONTACTS),
        )
    });

    fun launch(context: Context): Boolean {
        // メールは作成画面ではなく、メールアプリの受信トレイ（トップ画面）を開く
        if (this == MAIL && MailApp.openInbox(context)) return true
        return Launch.startAny(context, intents())
    }

    /** 担当アプリを探すためのインテント（選択画面用インテントは使わず、カテゴリを直接指定） */
    fun resolveIntents(): List<Intent> {
        val cat = when (this) {
            MAIL -> Intent.CATEGORY_APP_EMAIL
            CONTACTS -> Intent.CATEGORY_APP_CONTACTS
            MESSAGES -> Intent.CATEGORY_APP_MESSAGING
            BROWSER -> Intent.CATEGORY_APP_BROWSER
            GALLERY -> Intent.CATEGORY_APP_GALLERY
            CALENDAR -> Intent.CATEGORY_APP_CALENDAR
            MAPS -> Intent.CATEGORY_APP_MAPS
            MUSIC -> Intent.CATEGORY_APP_MUSIC
            CALCULATOR -> Intent.CATEGORY_APP_CALCULATOR
            else -> null
        }
        val byCategory = cat?.let { listOf(Intent(Intent.ACTION_MAIN).addCategory(it)) } ?: emptyList()
        val plain = intents().filter { it.selector == null }
        return byCategory + plain
    }
}

private fun category(cat: String): Intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, cat)

object Launch {
    fun start(context: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    fun startAny(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            if (start(context, intent)) return true
        }
        Toast.makeText(context, "対応するアプリが見つかりません", Toast.LENGTH_SHORT).show()
        return false
    }

    fun app(context: Context, pkg: String, cls: String) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(pkg, cls)
            .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        startAny(context, listOf(intent))
    }
}

/** 検索バーから、端末のメインブラウザ（既定のブラウザ）で Google 検索する */
object WebSearch {
    fun search(context: Context, query: String) {
        if (query.isEmpty()) {
            // 何も入力していなければ、メインブラウザをそのまま開く
            Launch.startAny(
                context,
                listOf(
                    category(Intent.CATEGORY_APP_BROWSER),
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/")),
                ),
            )
            return
        }
        // アプリを指定せずにURLを開くと、端末で「既定」にしているブラウザが使われる
        val url = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
        Launch.startAny(context, listOf(Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE)))
    }
}

/** 端末で使っているメールアプリを探して、その最初の画面（受信トレイ）を開く */
object MailApp {
    fun openInbox(context: Context): Boolean {
        val pkg = findMailPackage(context) ?: return false
        val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        return Launch.start(context, launch)
    }

    /** 受信トレイを開くメールアプリのパッケージ名（通知ドットにも使う） */
    fun findMailPackage(context: Context): String? {
        val pm = context.packageManager
        val mailto = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        // 既定のメールアプリが決まっていればそれを使う
        @Suppress("DEPRECATION")
        val def = pm.resolveActivity(mailto, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        if (def != null && def != "android" && pm.getLaunchIntentForPackage(def) != null) return def
        // 決まっていなければ、メールを扱えるアプリの最初の1つ
        @Suppress("DEPRECATION")
        return pm.queryIntentActivities(mailto, 0)
            .map { it.activityInfo.packageName }
            .firstOrNull { it != context.packageName && pm.getLaunchIntentForPackage(it) != null }
    }
}
