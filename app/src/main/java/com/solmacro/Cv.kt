package com.solmacro

import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

// OpenCV based finder for the reeling bar (color mask + morphology + contours),
// the same kind of pipeline PC macros use. Returns the same array layout as MacroService.autoScan
// so it can replace it. Returns null on any problem so the built-in detector takes over.
object Cv {
    @Volatile var ready = false

    fun init() {
        if (ready) return
        ready = try { OpenCVLoader.initLocal() } catch (e: Throwable) { false }
        Dbg.add(if (ready) "OpenCV ready" else "OpenCV not available, using the built-in detector")
    }

    private fun toMat(px: IntArray, w: Int, h: Int): Mat {
        val b = ByteArray(w * h * 3)
        var o = 0
        for (i in 0 until w * h) {
            val v = px[i]
            b[o++] = (v shr 16).toByte()
            b[o++] = (v shr 8).toByte()
            b[o++] = v.toByte()
        }
        val m = Mat(h, w, CvType.CV_8UC3)
        m.put(0, 0, b)
        return m
    }

    private fun mask(src: Mat, color: Int, tol: Int): Mat {
        val r = ((color shr 16) and 255).toDouble()
        val g = ((color shr 8) and 255).toDouble()
        val b = (color and 255).toDouble()
        val t = tol.toDouble()
        val m = Mat()
        Core.inRange(src, Scalar(r - t, g - t, b - t), Scalar(r + t, g + t, b + t), m)
        return m
    }

    // [zonePx, zoneLeft, zoneRight, blueCols, tipX(-1 if none), 1, boxX1, boxY1, boxX2, boxY2, redSamples]
    fun scanFull(px: IntArray, w: Int, h: Int, step: Int, zc: Int, mk: Int, tol: Int): DoubleArray? {
        if (!ready || w < 8 || h < 8) return null
        val mats = ArrayList<Mat>()
        try {
            val src = toMat(px, w, h)
            mats.add(src)
            val zm = mask(src, zc, tol)
            mats.add(zm)
            val bm = mask(src, mk, tol)
            mats.add(bm)
            val rm = mask(src, 0xFFFF4041.toInt(), 40)
            mats.add(rm)
            val out = doubleArrayOf(0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 0.0, Core.countNonZero(rm).toDouble())

            // close small gaps so the zone stays one blob
            val kern = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(maxOf(3.0, w * 0.01), 1.0))
            mats.add(kern)
            val zj = Mat()
            mats.add(zj)
            Imgproc.morphologyEx(zm, zj, Imgproc.MORPH_CLOSE, kern)
            val cs = ArrayList<MatOfPoint>()
            val hier = Mat()
            mats.add(hier)
            Imgproc.findContours(zj, cs, hier, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            val barL = w / 2.0 - 0.386 * h
            val barR = w / 2.0 + 0.386 * h
            var bx = 0
            var by = 0
            var bw = 0
            var bh = 0
            var barea = 0
            for (cn in cs) {
                val r = Imgproc.boundingRect(cn)
                cn.release()
                val cw = r.width
                val ch = r.height
                if (ch < 3 || cw < 2 * ch) continue
                if (ch < 0.025 * h || ch > 0.06 * h) continue
                if (cw < 0.02 * w || cw > w / 2) continue
                val cx = r.x + cw / 2.0
                val cy = r.y + ch / 2.0
                if (cx < barL || cx > barR || cy < 0.4 * h || cy > 0.8 * h) continue
                val sub = zm.submat(r)
                val area = Core.countNonZero(sub)
                sub.release()
                if (area < cw * ch / 2) continue
                if (area > barea) {
                    barea = area
                    bx = r.x
                    by = r.y
                    bw = cw
                    bh = ch
                }
            }
            if (barea == 0) return out

            out[0] = barea.toDouble() * step * step / 2.0
            out[1] = (bx * step).toDouble()
            out[2] = ((bx + bw) * step).toDouble()
            val ya = by + (bh - 1) / 4
            val yb = by + (bh - 1) - (bh - 1) / 4
            val xa = maxOf(0, barL.toInt() - 1)
            val xb = minOf(w - 1, bx + 2 * bw - 1)
            if (yb >= ya && xb >= xa) {
                val sub = bm.submat(ya, yb + 1, xa, xb + 1)
                val colSum = Mat()
                Core.reduce(sub, colSum, 0, Core.REDUCE_SUM, CvType.CV_32S)
                val n = xb - xa + 1
                val arr = IntArray(n)
                colSum.get(0, 0, arr)
                sub.release()
                colSum.release()
                val rows = yb - ya + 1
                var run = 0
                var runStart = -1
                var tip = -1
                var bn = 0
                for (i in 0..n) {
                    val ok = i < n && (arr[i] / 255) * 2 >= rows
                    if (ok) {
                        if (run == 0) runStart = i
                        run++
                    } else {
                        if (run >= 3 && runStart <= 0.03 * w) { tip = i; bn = run }
                        run = 0
                    }
                }
                out[3] = bn.toDouble()
                out[4] = if (tip >= 0) ((xa + tip) * step).toDouble() else -1.0
            }
            out[6] = (xa * step).toDouble()
            out[7] = ((by - 2) * step).toDouble()
            out[8] = ((xb + 1) * step).toDouble()
            out[9] = ((by + bh + 2) * step).toDouble()
            return out
        } catch (e: Throwable) {
            return null
        } finally {
            for (m in mats) m.release()
        }
    }
}
