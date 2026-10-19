package com.example.telhome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/*
 * F-04F のテレフォンホーム風の大タイル用アイコン（白い塗りのシンプルな形）。
 * 座標は 100×100 の枠で考えて、実際の大きさに合わせて拡大する。
 * 封筒のフタの線や電話帳の人型は「くり抜き」なので、タイルの質感が透けて見える。
 */

private const val White = 0xFFFFFFFF

/** 受話器＋ダイヤルキー（F-04F実機の表示に合わせた形） */
@Composable
fun PhoneGlyph(size: Dp, modifier: Modifier = Modifier) {
    GlyphCanvas(size, modifier) { u ->
        val white = Color(White)
        // 受話器のアーチ：細めの弧
        val handset = Path().apply {
            moveTo(u(19f), u(31f))
            cubicTo(u(21f), u(10f), u(79f), u(10f), u(81f), u(31f))
        }
        drawPath(handset, white, style = Stroke(width = u(8f), cap = StrokeCap.Butt))
        // 受話器の両端：外側・下向きに傾いた角ばった塊
        rotate(-38f, pivot = Offset(u(14.5f), u(32f))) {
            drawRoundRect(white, Offset(u(3f), u(25f)), Size(u(23f), u(14f)), CornerRadius(u(1.5f)))
        }
        rotate(38f, pivot = Offset(u(85.5f), u(32f))) {
            drawRoundRect(white, Offset(u(74f), u(25f)), Size(u(23f), u(14f)), CornerRadius(u(1.5f)))
        }
        // ダイヤルキー 3×3
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                drawRoundRect(
                    white,
                    Offset(u(26f + c * 18f), u(45f + r * 17.5f)),
                    Size(u(12f), u(11.5f)),
                    CornerRadius(u(1f)),
                )
            }
        }
    }
}

/** 封筒（フタの V 字はくり抜き） */
@Composable
fun MailGlyph(size: Dp, modifier: Modifier = Modifier) {
    GlyphCanvas(size, modifier) { u ->
        drawRoundRect(Color(White), Offset(u(6f), u(20f)), Size(u(88f), u(62f)), CornerRadius(u(7f)))
        val flap = Path().apply {
            moveTo(u(13f), u(27f))
            lineTo(u(50f), u(57f))
            lineTo(u(87f), u(27f))
        }
        drawPath(
            flap,
            Color.Black,
            style = Stroke(width = u(6.5f), cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = BlendMode.Clear,
        )
    }
}

/** 電話帳（人型はくり抜き、右側にインデックスのツメ） */
@Composable
fun ContactsGlyph(size: Dp, modifier: Modifier = Modifier) {
    GlyphCanvas(size, modifier) { u ->
        val white = Color(White)
        // 表紙
        drawRoundRect(white, Offset(u(14f), u(11f)), Size(u(58f), u(78f)), CornerRadius(u(6f)))
        // 人型（頭と肩）をくり抜く
        drawCircle(Color.Black, radius = u(10f), center = Offset(u(43f), u(39f)), blendMode = BlendMode.Clear)
        drawRoundRect(
            Color.Black,
            Offset(u(27f), u(53f)),
            Size(u(32f), u(16f)),
            CornerRadius(u(12f)),
            blendMode = BlendMode.Clear,
        )
        // インデックスのツメ 5つ
        for (i in 0 until 5) {
            drawRoundRect(white, Offset(u(77f), u(14f + i * 15f)), Size(u(13f), u(9.5f)), CornerRadius(u(2.5f)))
        }
    }
}

/** 財布（ウォレット）：後ろにカードが1枚、右側に留め具のくり抜き */
@Composable
fun WalletGlyph(size: Dp, modifier: Modifier = Modifier) {
    GlyphCanvas(size, modifier) { u ->
        val white = Color(White)
        // 後ろからのぞくカード
        drawRoundRect(white, Offset(u(16f), u(12f)), Size(u(62f), u(18f)), CornerRadius(u(5f)))
        drawRect(Color.Black, Offset(u(4f), u(20f)), Size(u(92f), u(5f)), blendMode = BlendMode.Clear)
        // 財布の本体
        drawRoundRect(white, Offset(u(8f), u(25f)), Size(u(84f), u(60f)), CornerRadius(u(9f)))
        // 留め具（右側をくり抜いて、中に丸）
        drawRoundRect(
            Color.Black,
            Offset(u(60f), u(43f)),
            Size(u(36f), u(24f)),
            CornerRadius(u(7f)),
            blendMode = BlendMode.Clear,
        )
        drawCircle(white, radius = u(5f), center = Offset(u(72f), u(55f)))
    }
}

/** 銀行：三角屋根・4本の柱・土台 */
@Composable
fun BankGlyph(size: Dp, modifier: Modifier = Modifier) {
    GlyphCanvas(size, modifier) { u ->
        val white = Color(White)
        val roof = Path().apply {
            moveTo(u(50f), u(6f))
            lineTo(u(95f), u(31f))
            lineTo(u(5f), u(31f))
            close()
        }
        drawPath(roof, white)
        // 屋根の中の丸い飾り（くり抜き）
        drawCircle(Color.Black, radius = u(5f), center = Offset(u(50f), u(22f)), blendMode = BlendMode.Clear)
        // 屋根の下の帯
        drawRect(white, Offset(u(8f), u(34f)), Size(u(84f), u(6f)))
        // 柱 4本
        for (i in 0 until 4) {
            drawRect(white, Offset(u(13f + i * 21f), u(44f)), Size(u(11f), u(32f)))
        }
        // 土台（2段）
        drawRect(white, Offset(u(8f), u(80f)), Size(u(84f), u(6f)))
        drawRect(white, Offset(u(4f), u(89f)), Size(u(92f), u(6f)))
    }
}

/** クレジットカード：磁気ストライプとICチップをくり抜き */
@Composable
fun CreditCardGlyph(size: Dp, modifier: Modifier = Modifier) {
    GlyphCanvas(size, modifier) { u ->
        drawRoundRect(Color(White), Offset(u(4f), u(18f)), Size(u(92f), u(64f)), CornerRadius(u(8f)))
        // 磁気ストライプ
        drawRect(Color.Black, Offset(u(4f), u(30f)), Size(u(92f), u(11f)), blendMode = BlendMode.Clear)
        // ICチップ
        drawRoundRect(
            Color.Black,
            Offset(u(14f), u(51f)),
            Size(u(18f), u(14f)),
            CornerRadius(u(3f)),
            blendMode = BlendMode.Clear,
        )
        // カード番号の線
        drawRoundRect(
            Color.Black,
            Offset(u(42f), u(64f)),
            Size(u(42f), u(5f)),
            CornerRadius(u(2.5f)),
            blendMode = BlendMode.Clear,
        )
    }
}

/** ポイントカード：カードの左に丸、その中に「P」 */
@Composable
fun PointCardGlyph(size: Dp, modifier: Modifier = Modifier) {
    GlyphCanvas(size, modifier) { u ->
        val white = Color(White)
        drawRoundRect(white, Offset(u(4f), u(18f)), Size(u(92f), u(64f)), CornerRadius(u(8f)))
        // 丸をくり抜いて、中に白い「P」
        drawCircle(Color.Black, radius = u(19f), center = Offset(u(32f), u(50f)), blendMode = BlendMode.Clear)
        drawRect(white, Offset(u(25f), u(38f)), Size(u(6f), u(25f)))
        val bowl = Path().apply {
            moveTo(u(28f), u(41f))
            lineTo(u(34f), u(41f))
            cubicTo(u(42f), u(41f), u(42f), u(52f), u(34f), u(52f))
            lineTo(u(28f), u(52f))
        }
        drawPath(bowl, white, style = Stroke(width = u(6f)))
        // 右側の文字の線（くり抜き）
        drawRoundRect(Color.Black, Offset(u(60f), u(38f)), Size(u(26f), u(6f)), CornerRadius(u(3f)), blendMode = BlendMode.Clear)
        drawRoundRect(Color.Black, Offset(u(60f), u(52f)), Size(u(18f), u(6f)), CornerRadius(u(3f)), blendMode = BlendMode.Clear)
    }
}

/** クーポン：両脇に切り欠きのあるチケットと、切り取り線 */
@Composable
fun CouponGlyph(size: Dp, modifier: Modifier = Modifier) {
    GlyphCanvas(size, modifier) { u ->
        drawRoundRect(Color(White), Offset(u(4f), u(22f)), Size(u(92f), u(56f)), CornerRadius(u(6f)))
        // 左右の半円の切り欠き
        drawCircle(Color.Black, radius = u(8f), center = Offset(u(4f), u(50f)), blendMode = BlendMode.Clear)
        drawCircle(Color.Black, radius = u(8f), center = Offset(u(96f), u(50f)), blendMode = BlendMode.Clear)
        // 切り取り線（点線）
        for (i in 0 until 5) {
            drawRect(Color.Black, Offset(u(66f), u(27f + i * 10f)), Size(u(4f), u(5f)), blendMode = BlendMode.Clear)
        }
        // 「%」の形（くり抜き）
        drawCircle(Color.Black, radius = u(5f), center = Offset(u(26f), u(39f)), blendMode = BlendMode.Clear)
        drawCircle(Color.Black, radius = u(5f), center = Offset(u(46f), u(61f)), blendMode = BlendMode.Clear)
        val slash = Path().apply {
            moveTo(u(47f), u(35f))
            lineTo(u(25f), u(65f))
        }
        drawPath(slash, Color.Black, style = Stroke(width = u(5f), cap = StrokeCap.Round), blendMode = BlendMode.Clear)
    }
}

/** ショッピング：持ち手の付いた買い物袋 */
@Composable
fun ShoppingGlyph(size: Dp, modifier: Modifier = Modifier) {
    GlyphCanvas(size, modifier) { u ->
        val white = Color(White)
        // 持ち手
        val handle = Path().apply {
            moveTo(u(34f), u(36f))
            lineTo(u(34f), u(26f))
            cubicTo(u(34f), u(8f), u(66f), u(8f), u(66f), u(26f))
            lineTo(u(66f), u(36f))
        }
        drawPath(handle, white, style = Stroke(width = u(7f), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // 袋（下がやや広い台形）
        val bag = Path().apply {
            moveTo(u(20f), u(32f))
            lineTo(u(80f), u(32f))
            lineTo(u(88f), u(92f))
            lineTo(u(12f), u(92f))
            close()
        }
        drawPath(bag, white)
        // 持ち手の付け根の穴（くり抜き）
        drawCircle(Color.Black, radius = u(4f), center = Offset(u(34f), u(44f)), blendMode = BlendMode.Clear)
        drawCircle(Color.Black, radius = u(4f), center = Offset(u(66f), u(44f)), blendMode = BlendMode.Clear)
    }
}

/** 交通：電車の正面（窓・ライト・足元） */
@Composable
fun TransportGlyph(size: Dp, modifier: Modifier = Modifier) {
    GlyphCanvas(size, modifier) { u ->
        val white = Color(White)
        // 車体
        drawRoundRect(white, Offset(u(18f), u(6f)), Size(u(64f), u(72f)), CornerRadius(u(14f)))
        // 行き先表示と窓（くり抜き）
        drawRoundRect(Color.Black, Offset(u(34f), u(13f)), Size(u(32f), u(6f)), CornerRadius(u(3f)), blendMode = BlendMode.Clear)
        drawRoundRect(Color.Black, Offset(u(26f), u(25f)), Size(u(48f), u(24f)), CornerRadius(u(4f)), blendMode = BlendMode.Clear)
        // ライト（くり抜き）
        drawCircle(Color.Black, radius = u(5f), center = Offset(u(33f), u(64f)), blendMode = BlendMode.Clear)
        drawCircle(Color.Black, radius = u(5f), center = Offset(u(67f), u(64f)), blendMode = BlendMode.Clear)
        // 足元（ハの字）
        val legs = Path().apply {
            moveTo(u(32f), u(80f)); lineTo(u(20f), u(96f))
            moveTo(u(68f), u(80f)); lineTo(u(80f), u(96f))
        }
        drawPath(legs, white, style = Stroke(width = u(7f), cap = StrokeCap.Round))
    }
}

/** ゲーム：ゲームコントローラー（十字キーとボタン） */
@Composable
fun GameGlyph(size: Dp, modifier: Modifier = Modifier) {
    GlyphCanvas(size, modifier) { u ->
        val white = Color(White)
        // 本体（左右のグリップが少し下がった形）
        val body = Path().apply {
            moveTo(u(26f), u(28f))
            lineTo(u(74f), u(28f))
            cubicTo(u(90f), u(28f), u(98f), u(44f), u(97f), u(62f))
            cubicTo(u(96f), u(78f), u(84f), u(82f), u(76f), u(72f))
            lineTo(u(68f), u(62f))
            lineTo(u(32f), u(62f))
            lineTo(u(24f), u(72f))
            cubicTo(u(16f), u(82f), u(4f), u(78f), u(3f), u(62f))
            cubicTo(u(2f), u(44f), u(10f), u(28f), u(26f), u(28f))
            close()
        }
        drawPath(body, white)
        // 十字キー（くり抜き）
        drawRect(Color.Black, Offset(u(19f), u(42f)), Size(u(18f), u(6f)), blendMode = BlendMode.Clear)
        drawRect(Color.Black, Offset(u(25f), u(36f)), Size(u(6f), u(18f)), blendMode = BlendMode.Clear)
        // ボタン（くり抜き）
        drawCircle(Color.Black, radius = u(4.5f), center = Offset(u(72f), u(39f)), blendMode = BlendMode.Clear)
        drawCircle(Color.Black, radius = u(4.5f), center = Offset(u(81f), u(48f)), blendMode = BlendMode.Clear)
    }
}

/** 100×100 の座標で描くためのキャンバス。くり抜き（Clear）が効くよう別レイヤーで描く */
@Composable
private fun GlyphCanvas(size: Dp, modifier: Modifier, draw: DrawScope.(u: (Float) -> Float) -> Unit) {
    Canvas(
        modifier
            .size(size)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        val scale = this.size.minDimension / 100f
        draw { it * scale }
    }
}
