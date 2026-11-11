package com.solmacro

object Dbg {
    private val lines = ArrayList<String>()
    @Volatile var state = "Idle"
    @Volatile var catches = 0
    @Volatile var sells = 0
    @Volatile var startMs = 0L
    @Volatile var fps = 0
    @Volatile var latency = 0
    @Volatile var biteMs = 0
    @Volatile var fails = 0
    @Volatile var inZone = 0
    @Volatile var biome = ""
    // why Start was refused (shown in the panel)
    @Volatile var blocker = ""
    private val catchTimes = ArrayList<Long>()

    @Synchronized
    fun markCatch() {
        val now = android.os.SystemClock.uptimeMillis()
        catchTimes.add(now)
        while (catchTimes.isNotEmpty() && now - catchTimes[0] > 40 * 60000L) catchTimes.removeAt(0)
    }

    @Synchronized
    fun resetCatches() { catchTimes.clear() }

    // catches in each of the last n minutes, oldest first
    @Synchronized
    fun perMinute(n: Int): IntArray {
        val now = android.os.SystemClock.uptimeMillis()
        val out = IntArray(n)
        for (t in catchTimes) {
            val back = ((now - t) / 60000L).toInt()
            if (back in 0 until n) out[n - 1 - back]++
        }
        return out
    }

    @Synchronized
    fun add(s: String) {
        val t = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        lines.add("$t $s")
        if (lines.size > 150) lines.removeAt(0)
    }

    @Synchronized
    fun tail(n: Int): String = lines.takeLast(n).joinToString("\n")
}
