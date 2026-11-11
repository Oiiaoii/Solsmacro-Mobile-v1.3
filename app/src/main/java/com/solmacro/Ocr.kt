package com.solmacro

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.TimeUnit

// On-device text reading (ML Kit). Call from a background thread only.
object Ocr {
    private val rec by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    fun read(b: Bitmap): String? = try {
        Tasks.await(rec.process(InputImage.fromBitmap(b, 0)), 5, TimeUnit.SECONDS).text
    } catch (e: Throwable) {
        Dbg.add("OCR failed: " + e.message)
        null
    }
}
