package com.solmacro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

class DebugView(ctx: Context) : View(ctx) {
    val rect = Rect()
    var color = Color.RED
    var label = ""
    var vert = false
    var zmin = -1.0
    var zmax = -1.0
    var mpos = -1.0

    private val p = Paint().apply { style = Paint.Style.STROKE; isAntiAlias = true }
    private val t = Paint().apply {
        this.color = Color.WHITE
        textSize = 34f
        isAntiAlias = true
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    override fun onDraw(c: Canvas) {
        p.strokeWidth = 5f
        p.color = color
        c.drawRect(rect, p)
        p.strokeWidth = 3f
        if (zmax >= 0) {
            p.color = Color.GREEN
            line(c, zmin)
            line(c, zmax)
        }
        if (mpos >= 0) {
            p.color = Color.WHITE
            line(c, mpos)
        }
        c.drawText(label, rect.left.toFloat(), maxOf(rect.top - 12, 40).toFloat(), t)
    }

    private fun line(c: Canvas, pos: Double) {
        val v = pos.toFloat()
        if (vert) c.drawLine(rect.left.toFloat(), v, rect.right.toFloat(), v, p)
        else c.drawLine(v, rect.top.toFloat(), v, rect.bottom.toFloat(), p)
    }
}
