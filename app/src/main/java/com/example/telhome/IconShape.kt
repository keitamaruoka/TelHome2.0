package com.example.telhome

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable

/**
 * アプリのアイコンを「角を丸くした四角」にそろえる。
 * 端末の設定（丸・しずく型など）に関係なく、このランチャーでは同じ形で表示する。
 */
object IconShape {
    /** 角の丸み（一辺に対する割合） */
    private const val CORNER = 0.13f

    /** 絵柄の拡大量（0.25 が標準。大きいほど中の絵が大きくなる） */
    private const val FOREGROUND_INSET = 0.40f

    fun roundedSquare(drawable: Drawable, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = size.toFloat()
        val clip = Path().apply { addRoundRect(RectF(0f, 0f, s, s), s * CORNER, s * CORNER, Path.Direction.CW) }

        if (drawable is AdaptiveIconDrawable) {
            // 新しい形式のアイコン：背景と絵柄を自分で重ねて、角丸四角で切り抜く
            // （各レイヤーは見える部分の1.5倍の大きさなので、外側に1/4ずつはみ出させる）
            canvas.save()
            canvas.clipPath(clip)
            // 背景は標準どおり（見える部分の1.5倍）、絵柄だけ大きめ（1.8倍）に描いて中の絵を大きく見せる
            drawable.background?.let { bg ->
                val inset = size / 4
                bg.setBounds(-inset, -inset, size + inset, size + inset)
                bg.draw(canvas)
            }
            drawable.foreground?.let { fg ->
                val inset = (size * FOREGROUND_INSET).toInt()
                fg.setBounds(-inset, -inset, size + inset, size + inset)
                fg.draw(canvas)
            }
            canvas.restore()
        } else {
            // 古い形式のアイコン：白い角丸四角の上に少し小さく載せる
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
            canvas.drawPath(clip, paint)
            val pad = (size * 0.07f).toInt()
            drawable.setBounds(pad, pad, size - pad, size - pad)
            canvas.save()
            canvas.clipPath(clip)
            drawable.draw(canvas)
            canvas.restore()
        }
        return bmp
    }
}
