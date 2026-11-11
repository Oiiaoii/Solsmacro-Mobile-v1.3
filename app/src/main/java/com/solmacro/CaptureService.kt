package com.solmacro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class Reg(val px: IntArray, val x: Int, val y: Int, val w: Int, val h: Int, val step: Int = 1, val id: Long = 0L, val ns: Long = 0L)

object Cap {
    @Volatile var active = false
    // counts every new frame taken from the screen, and the time Android stamped it (nanoseconds)
    @Volatile var frameId = 0L
    @Volatile var frameNs = 0L
    var proj: MediaProjection? = null
    private var vd: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var last: Image? = null
    private var w = 0
    private var h = 0

    @Synchronized
    fun ensure(nw: Int, nh: Int, dpi: Int) {
        val p = proj ?: return
        if (reader != null && nw == w && nh == h) return
        last?.close()
        last = null
        val nr = ImageReader.newInstance(nw, nh, PixelFormat.RGBA_8888, 2)
        val v = vd
        if (v == null) {
            vd = p.createVirtualDisplay("solmacro", nw, nh, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, nr.surface, null, null)
        } else {
            v.resize(nw, nh, dpi)
            v.surface = nr.surface
        }
        reader?.close()
        reader = nr
        w = nw
        h = nh
        Dbg.add("Fast capture size ${nw}x$nh")
    }

    // Takes the newest frame (if there is one) without copying pixels. Returns the frame counter.
    @Synchronized
    fun poll(): Long {
        val rd = reader ?: return frameId
        val n = try { rd.acquireLatestImage() } catch (e: Exception) { null }
        if (n != null) {
            last?.close()
            last = n
            frameId++
            frameNs = n.timestamp
        }
        return frameId
    }

    @Synchronized
    fun region(x1: Int, y1: Int, rw: Int, rh: Int, step: Int = 1): Reg? {
        poll()
        val img = last ?: return null
        val pl = img.planes[0]
        val buf = pl.buffer
        val rs = pl.rowStride
        val ps = pl.pixelStride
        val cx = x1.coerceIn(0, img.width - 1)
        val cy = y1.coerceIn(0, img.height - 1)
        val cw = minOf(rw, img.width - cx)
        val ch = minOf(rh, img.height - cy)
        if (cw <= 0 || ch <= 0) return null
        val ow = cw / step
        val oh = ch / step
        if (ow <= 0 || oh <= 0) return null
        val out = IntArray(ow * oh)
        // copy one whole row at a time instead of one byte at a time (much faster)
        val rowLen = ((ow - 1) * step + 1) * ps + 3
        val row = ByteArray(rowLen)
        val lim = buf.limit()
        var o = 0
        for (yy in 0 until oh) {
            val start = (cy + yy * step) * rs + cx * ps
            val n = minOf(rowLen, lim - start)
            if (n < ps) { o += ow; continue }
            (buf as java.nio.Buffer).position(start)
            buf.get(row, 0, n)
            var idx = 0
            for (xx in 0 until ow) {
                if (idx + 2 >= n) { out[o++] = 0xFF000000.toInt(); idx += ps * step; continue }
                val r = row[idx].toInt() and 255
                val g = row[idx + 1].toInt() and 255
                val b = row[idx + 2].toInt() and 255
                out[o++] = (255 shl 24) or (r shl 16) or (g shl 8) or b
                idx += ps * step
            }
        }
        return Reg(out, cx, cy, ow, oh, step, frameId, frameNs)
    }

    @Synchronized
    fun release() {
        active = false
        last?.close()
        last = null
        vd?.release()
        vd = null
        reader?.close()
        reader = null
        try { proj?.stop() } catch (_: Exception) {}
        proj = null
        w = 0
        h = 0
        frameId = 0L
        frameNs = 0L
    }
}

class CaptureService : Service() {
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        val ch = "cap"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(ch, "Screen capture", NotificationManager.IMPORTANCE_LOW))
        }
        val n = Notification.Builder(this, ch)
            .setContentTitle("Sol Macro is reading the screen")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, n)
        }
        val code = i?.getIntExtra("code", 0) ?: 0
        val data = i?.getParcelableExtra<Intent>("data")
        if (data == null || code == 0) {
            stopSelf()
            return START_NOT_STICKY
        }
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val p = mpm.getMediaProjection(code, data)
        p.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Cap.release()
                Dbg.add("Fast capture stopped")
                stopSelf()
            }
        }, Handler(Looper.getMainLooper()))
        Cap.proj = p
        Cap.active = true
        Dbg.add("Fast capture ready")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Cap.release()
        super.onDestroy()
    }
}
