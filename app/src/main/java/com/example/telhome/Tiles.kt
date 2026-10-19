package com.example.telhome

import android.graphics.Bitmap
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.random.Random

/**
 * F-04F のテレフォンホーム風タイルの質感。
 *  - 上が明るく下が暗いグラデーション
 *  - 紙や革のような細かいムラ（テクスチャ）
 *  - 右上から斜めに入る光の反射
 *  - 上辺のハイライトと下辺の影で、少し浮き出たような縁
 */
fun Modifier.tileSurface(
    top: Color,
    bottom: Color,
    texture: Float = 0.10f,
    sheen: Float = 0.07f,
): Modifier = composed {
    val noise = remember { TileTexture.bitmap }
    val noiseBrush = remember(noise) { ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated)) }
    drawBehind {
        val w = size.width
        val h = size.height

        // 1) 下地のグラデーション（少し斜め）
        drawRect(Brush.linearGradient(listOf(top, bottom), start = Offset(w, 0f), end = Offset(w * 0.65f, h)))

        // 2) ムラのテクスチャ（明るい所と暗い所を重ねる）
        if (texture > 0f) {
            drawRect(noiseBrush, alpha = texture, blendMode = BlendMode.Overlay)
        }

        // 3) 斜めの光（左上側を少し明るく。境目の線は右上から左下へ急な角度で走る）
        //    線の位置と角度はタイルの「幅」だけで決めるので、電話・メール・電話帳・小タイルで
        //    同じ角度になる（F-04F実機と同じく、上辺の右寄りから左下へ）。
        val edge = w * 0.80f        // 上辺のどこから線が始まるか
        val slope = 1.9f            // 線の傾き（横1に対して縦の下がり量）
        val wedge = Path().apply {
            moveTo(0f, 0f)
            lineTo(edge, 0f)
            val leftY = edge * slope
            if (leftY <= h) {
                lineTo(0f, leftY)
            } else {
                lineTo(edge - h / slope, h)
                lineTo(0f, h)
            }
            close()
        }
        drawPath(
            wedge,
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = sheen * 1.4f), Color.White.copy(alpha = sheen * 0.3f)),
                start = Offset(0f, 0f),
                end = Offset(w * 0.6f, w * 0.7f),
            ),
        )

        // 4) 上部の柔らかい光沢
        drawRect(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                startY = 0f,
                endY = h * 0.45f,
            ),
            size = Size(w, h * 0.45f),
        )

        // 5) 縁：上と左に細いハイライト、下と右に影
        val px = 1.dp.toPx()
        drawRect(Color.White.copy(alpha = 0.16f), size = Size(w, px))
        drawRect(Color.White.copy(alpha = 0.08f), size = Size(px, h))
        drawRect(Color.Black.copy(alpha = 0.30f), topLeft = Offset(0f, h - px), size = Size(w, px))
        drawRect(Color.Black.copy(alpha = 0.18f), topLeft = Offset(w - px, 0f), size = Size(px, h))
    }
}

/** タイル・背景用の細かいムラ模様（起動時に一度だけ作る） */
object TileTexture {
    val bitmap: ImageBitmap by lazy { make(160, 0x5EED) }

    /** 値ノイズを数段重ねた、ゆるやかなムラ */
    private fun make(size: Int, seed: Int): ImageBitmap {
        val rnd = Random(seed)
        val grids = listOf(8 to 0.5f, 16 to 0.3f, 40 to 0.2f)
        val layers = grids.map { (g, _) -> FloatArray(g * g) { rnd.nextFloat() } }
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                var v = 0f
                grids.forEachIndexed { i, (g, weight) ->
                    v += weight * sample(layers[i], g, x * g / size.toFloat(), y * g / size.toFloat())
                }
                // 細かい粒も少し混ぜる
                v = v * 0.85f + rnd.nextFloat() * 0.15f
                val c = (v.coerceIn(0f, 1f) * 255).toInt()
                pixels[y * size + x] = (0xFF shl 24) or (c shl 16) or (c shl 8) or c
            }
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    /** 端がつながる（タイル状に並べても継ぎ目が出ない）補間 */
    private fun sample(grid: FloatArray, g: Int, fx: Float, fy: Float): Float {
        val x0 = floor(fx).toInt()
        val y0 = floor(fy).toInt()
        val tx = smooth(fx - x0)
        val ty = smooth(fy - y0)
        fun at(x: Int, y: Int) = grid[((y % g + g) % g) * g + ((x % g + g) % g)]
        val a = at(x0, y0) + (at(x0 + 1, y0) - at(x0, y0)) * tx
        val b = at(x0, y0 + 1) + (at(x0 + 1, y0 + 1) - at(x0, y0 + 1)) * tx
        return a + (b - a) * ty
    }

    private fun smooth(t: Float) = t * t * (3 - 2 * t)
}
