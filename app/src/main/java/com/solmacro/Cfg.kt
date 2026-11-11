package com.solmacro

import android.content.Context

object Cfg {
    const val ROBLOX = "com.roblox.client"
    val keys = listOf("cast", "reel", "inv", "sellall", "confirm", "joy", "cam", "shift", "dismiss")
    val label = mapOf(
        "cast" to "Cast button", "reel" to "Reel area", "inv" to "Inventory button",
        "sellall" to "Sell all button", "confirm" to "Confirm button",
        "joy" to "Movement joystick center", "cam" to "Camera swipe start", "shift" to "Shift Lock button",
        "dismiss" to "Close (X) button on the caught or failed panel"
    )
    val extra = listOf("p1", "p2", "p3", "p4", "p5", "p6")
    val bioKeys = listOf("bio1", "bio2")
    val watKeys = listOf("wat1", "wat2")
    val label2: Map<String, String> = mapOf(
        "bio1" to "TOP-LEFT corner of the biome name text",
        "bio2" to "BOTTOM-RIGHT corner of the biome name text",
        "wat1" to "TOP-LEFT corner of the area to watch for words (chat, merchant, aura text)",
        "wat2" to "BOTTOM-RIGHT corner of the area to watch for words"
    ) + extra.associateWith { "extra point " + it.uppercase() + " (use it in scripts as: tap " + it + ")" }
    private val def = mapOf(
        "cast" to 900, "wait" to 4000, "taps" to 12, "gap" to 120, "mdelay" to 700,
        "every" to 10, "sell" to 1,
        "cast_x" to 2000, "cast_y" to 820, "reel_x" to 1170, "reel_y" to 330,
        "inv_x" to 120, "inv_y" to 500, "sellall_x" to 1100, "sellall_y" to 760,
        "confirm_x" to 1250, "confirm_y" to 660,
        "joy_x" to 250, "joy_y" to 800, "cam_x" to 1700, "cam_y" to 500, "shift_x" to 2150, "shift_y" to 250, "joyr" to 130,
        "icon_custom" to 0, "full" to 1, "castdelay" to 150, "postcatch" to 600, "gonems" to 500, "adapt" to 1, "detect" to 1, "debug" to 0, "cv" to 1, "afk" to 1, "afkmin" to 10, "shoton" to 0, "shotmin" to 30, "bio" to 0, "biosec" to 5, "t1min" to 30, "t2min" to 30, "t3min" to 30, "whon" to 1, "whevery" to 10, "maxcatch" to 0, "color2" to -14581335, "dismiss_x" to 1500, "dismiss_y" to 110, "tapms" to 70, "axis" to 0, "holddir" to 1, "deadband" to 15, "holdms" to 220, "tol" to 28, "minpx" to 150, "maxwait" to 30, "color" to -15039198,
        "r1_x" to 900, "r1_y" to 300, "r2_x" to 1500, "r2_y" to 500,
        "dismisswait" to 900, "resultms" to 500, "learn" to 1, "widen" to 1,
        "rejoin" to 0, "rejoinsec" to 45, "watch" to 0, "watchsec" to 4, "pingrare" to 1, "pingwatch" to 1
    )

    private fun sp(c: Context) = c.getSharedPreferences("macro", Context.MODE_PRIVATE)
    fun get(c: Context, k: String): Int = sp(c).getInt(k, def[k] ?: 0)
    fun gets(c: Context, k: String): String = sp(c).getString(k, "") ?: ""
    fun puts(c: Context, k: String, v: String) = sp(c).edit().putString(k, v).apply()
    fun put(c: Context, k: String, v: Int) = sp(c).edit().putInt(k, v).apply()
}
