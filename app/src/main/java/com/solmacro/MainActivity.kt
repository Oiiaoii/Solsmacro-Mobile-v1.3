package com.solmacro

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.animation.ObjectAnimator
import android.graphics.drawable.GradientDrawable
import android.view.animation.OvershootInterpolator
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewOutlineProvider
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

private const val FILL = ViewGroup.LayoutParams.MATCH_PARENT
private const val HUG = ViewGroup.LayoutParams.WRAP_CONTENT

class MainActivity : Activity() {
    private val cBg = 0xFF060608.toInt()
    private val cCard = 0xFF121216.toInt()
    private val cBd = 0xFF2A2A31.toInt()
    private val cTx = 0xFFE8EEFC.toInt()
    private val cMut = 0xFF9A9AA5.toInt()
    private val cAcc = 0xFFEDEDF0.toInt()
    private val cOk = 0xFF38D39F.toInt()
    private val cBad = 0xFFFF6B6B.toInt()

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var root: LinearLayout
    private lateinit var pill: TextView
    private lateinit var stateTv: TextView
    private lateinit var statsTv: TextView
    private lateinit var posTv: TextView
    private lateinit var perfTv: TextView
    private lateinit var logo: ImageView
    private lateinit var prevIcon: ImageView
    private var cardN = 0
    private var autoReturn = false
    private var logoAnim: ObjectAnimator? = null
    private lateinit var logTv: TextView

    private val tick = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 500)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun shape(color: Int, r: Int, stroke: Int = 0) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(r).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }

    private fun tv(t: String, size: Float, color: Int = cTx, bold: Boolean = false) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun add(p: LinearLayout, v: View, top: Int = 8) {
        p.addView(v, LinearLayout.LayoutParams(FILL, HUG).apply { topMargin = dp(top) })
    }

    private fun card(title: String?): LinearLayout {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(cCard, 16, cBd)
            setPadding(dp(16), dp(14), dp(16), dp(16))
        }
        add(root, c, 12)
        c.alpha = 0f
        c.translationY = dp(18).toFloat()
        c.animate().alpha(1f).translationY(0f).setStartDelay((cardN * 70).toLong()).setDuration(350).start()
        cardN++
        if (title != null) c.addView(tv(title, 16f, cTx, true))
        return c
    }

    private fun btn(t: String, color: Int, onClick: () -> Unit) = Button(this).apply {
        text = t
        isAllCaps = false
        setTextColor(if (color == cOk) 0xFF06251C.toInt() else if (color == cAcc) 0xFF0A0A0C.toInt() else Color.WHITE)
        background = shape(color, 12)
        setOnClickListener { onClick() }
        setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            false
        }
    }

    private fun row(p: LinearLayout, vararg b: Button) {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((i, x) in b.withIndex()) {
            r.addView(x, LinearLayout.LayoutParams(0, HUG, 1f).apply { if (i > 0) leftMargin = dp(8) })
        }
        add(p, r, 10)
    }

    private fun field(p: LinearLayout, label: String, key: String) {
        add(p, tv(label, 12f, cMut), 10)
        val e = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(Cfg.get(this@MainActivity, key).toString())
            setTextColor(cTx)
            background = shape(cBg, 10, cBd)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    Cfg.put(this@MainActivity, key, s.toString().toIntOrNull() ?: 0)
                }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        add(p, e, 4)
    }

    private fun bump(t: TextView, text: String) {
        if (t.text.toString() == text) return
        t.text = text
        t.scaleX = 1.12f
        t.scaleY = 1.12f
        t.animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(OvershootInterpolator(2f)).start()
    }

    private fun textField(p: LinearLayout, label: String, key: String, hintText: String = "") {
        add(p, tv(label, 12f, cMut), 10)
        val e = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(Cfg.gets(this@MainActivity, key))
            hint = hintText
            setHintTextColor(cMut)
            setTextColor(cTx)
            background = shape(cBg, 10, cBd)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { Cfg.puts(this@MainActivity, key, s.toString().trim()) }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        add(p, e, 4)
    }

    private fun sw(p: LinearLayout, label: String, key: String) {
        val s = Switch(this).apply {
            text = label
            setTextColor(cTx)
            isChecked = Cfg.get(this@MainActivity, key) == 1
            setOnCheckedChangeListener { _, on -> Cfg.put(this@MainActivity, key, if (on) 1 else 0) }
        }
        add(p, s, 10)
    }

    private fun roundIcon(sizeDp: Int): ImageView = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = GradientDrawable().apply { setShape(GradientDrawable.OVAL); setColor(0xFF000000.toInt()) }
        clipToOutline = true
        outlineProvider = ViewOutlineProvider.BACKGROUND
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)).apply { topMargin = dp(10) }
    }

    private fun applyIcon(v: ImageView) {
        val f = File(filesDir, "float_icon.png")
        if (Cfg.get(this, "icon_custom") == 1 && f.exists()) {
            val b = BitmapFactory.decodeFile(f.absolutePath)
            if (b != null) { v.setImageBitmap(b); return }
        }
        v.setImageResource(R.drawable.brry_icon)
    }

    private fun pickIcon() {
        val i = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(i, 101)
    }

    private fun resetIcon() {
        Cfg.put(this, "icon_custom", 0)
        applyIcon(prevIcon)
        applyIcon(logo)
        MacroService.inst?.refreshIcon()
    }

    private fun saveIcon(uri: android.net.Uri) {
        try {
            val opt = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opt) }
            val o2 = BitmapFactory.Options().apply { inSampleSize = maxOf(1, maxOf(opt.outWidth, opt.outHeight) / 1024) }
            val bmp = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) }
                ?: throw Exception("no image")
            val side = minOf(bmp.width, bmp.height)
            val sq = android.graphics.Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
            val out = android.graphics.Bitmap.createScaledBitmap(sq, 192, 192, true)
            FileOutputStream(File(filesDir, "float_icon.png")).use { out.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            Cfg.put(this, "icon_custom", 1)
            applyIcon(prevIcon)
            applyIcon(logo)
            MacroService.inst?.refreshIcon()
            Toast.makeText(this, "Icon updated", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not use that image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun script(p: LinearLayout, label: String, key: String) {
        add(p, tv(label, 12f, cMut), 10)
        val e = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            gravity = Gravity.TOP
            typeface = Typeface.MONOSPACE
            setText(Cfg.gets(this@MainActivity, key))
            setTextColor(cTx)
            background = shape(cBg, 10, cBd)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { Cfg.puts(this@MainActivity, key, s.toString()) }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        add(p, e, 4)
    }

    private fun requestCapture() {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), 100)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == 101 && res == RESULT_OK && data?.data != null) saveIcon(data.data!!)
        if (req == 100) {
            if (res == RESULT_OK && data != null) {
                val i = Intent(this, CaptureService::class.java).putExtra("code", res).putExtra("data", data)
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
                Toast.makeText(this, "Fast capture starting", Toast.LENGTH_SHORT).show()
                if (autoReturn) {
                    autoReturn = false
                    // go back to Roblox so you can press Start right away
                    handler.postDelayed({
                        val i2 = packageManager.getLaunchIntentForPackage(Cfg.ROBLOX)
                        if (i2 != null) startActivity(i2)
                    }, 900L)
                }
            } else {
                autoReturn = false
                Toast.makeText(this, "Permission not given", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun preset(cast: Int, wait: Int, taps: Int, gap: Int) {
        Cfg.put(this, "cast", cast); Cfg.put(this, "wait", wait); Cfg.put(this, "taps", taps); Cfg.put(this, "gap", gap)
        recreate()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(40), dp(16), dp(32))
        }
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        logo = roundIcon(48)
        applyIcon(logo)
        head.addView(logo)
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(tv("Brry", 28f, cTx, true))
        titles.addView(tv("Sol’s RNG Macro", 13f, cMut))
        head.addView(titles, LinearLayout.LayoutParams(HUG, HUG).apply { leftMargin = dp(12) })
        root.addView(head)
        root.addView(tv("Fishing and selling helper for Sol’s RNG. Not affiliated with Roblox. Macros can break game rules and get accounts banned.", 12f, cMut))

        val st = card(null)
        pill = tv("", 12f, Color.WHITE, true).apply { setPadding(dp(14), dp(6), dp(14), dp(6)) }
        st.addView(pill, LinearLayout.LayoutParams(HUG, HUG))
        stateTv = tv("", 15f)
        add(st, stateTv, 10)
        statsTv = tv("", 13f, cMut)
        add(st, statsTv, 4)
        perfTv = tv("", 12f, cMut)
        add(st, perfTv, 4)
        posTv = tv("", 11f, cMut)
        add(st, posTv, 10)
        row(st,
            btn("Turn on service", cAcc) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            btn("Open Roblox", cBd) {
                val i = packageManager.getLaunchIntentForPackage(Cfg.ROBLOX)
                if (i != null) startActivity(i) else Toast.makeText(this, "Roblox is not installed", Toast.LENGTH_SHORT).show()
            })

        val how = card("How to use")
        add(how, tv("1. Turn on the service, then open Roblox. A round Brry button appears.\n2. Tap it to open the panel and tap it again to close it.\n3. Panel > Settings > Set button positions: tap the 9 spots it asks for (the last one is the X on the caught or failed panel).\n4. Start fast capture below (Android asks for permission).\n5. Press the big play button or the Toggle to start switch under it. If Start is refused, the panel shows exactly what is missing and you can tap the red row to fix it. Use Test detection while the reeling bar shows.", 12f, cMut), 6)

        val fish = card("Fishing")
        field(fish, "Cast hold (ms)", "cast")
        field(fish, "Stop after N catches (0 = never)", "maxcatch")

        val det = card("Detection (whole screen)")
        sw(det, "Debug overlay (green, yellow, red box)", "debug")
        field(det, "Minimum bar size (pixels)", "minpx")
        field(det, "Max seconds to wait for the bar", "maxwait")
        field(det, "Time between reel taps (ms)", "tapms")
        field(det, "Close button X (caught or failed panel)", "dismiss_x")
        field(det, "Close button Y (caught or failed panel)", "dismiss_y")
        sw(det, "Learn the real drain speed while reeling", "learn")
        sw(det, "Widen the color match after a missed bar", "widen")
        add(det, tv("Timing", 14f, cTx, true), 14)
        field(det, "Result panel wait (ms)", "resultms")
        field(det, "Wait after closing the result (ms)", "dismisswait")
        sw(det, "Adaptive recast time (learns how long bites take)", "adapt")
        field(det, "Wait after casting before looking (ms)", "castdelay")
        field(det, "Bar counts as finished after (ms)", "gonems")
        field(det, "Pause after a catch (ms)", "postcatch")

        val fl = card("Floating button")
        add(fl, tv("A round button stays on screen over Roblox. Tap it to open the control bar and tap it again to close it. Drag it to move it.", 12f, cMut), 6)
        prevIcon = roundIcon(72)
        applyIcon(prevIcon)
        fl.addView(prevIcon)
        row(fl, btn("Use This Icon", cAcc) { pickIcon() }, btn("Use default icon", cBd) { resetIcon() })

        val cap = card("Fast screen capture")
        add(cap, tv("Reads the screen about 30 times a second instead of 3. Android asks for permission each time you start it.", 12f, cMut), 6)
        add(cap, btn("Start fast capture", cOk) { requestCapture() }, 10)
        add(cap, btn("Stop fast capture", cBd) { Cap.release(); stopService(Intent(this, CaptureService::class.java)) }, 8)

        val mv = card("Movement, camera and Shift Lock")
        add(mv, tv("Scripts run with swipes on Roblox's on-screen joystick and camera area. One command per line:\nmove up 1.5   (also down, left, right, w, a, s, d)\ncamera left 250\nshiftlock\ntap inv\nwait 1\n# comment", 12f, cMut), 6)
        field(mv, "Joystick push distance (pixels)", "joyr")
        script(mv, "Run once when you press START", "script_start")
        script(mv, "Run before selling (walk to the seller)", "script_pre")
        script(mv, "Run after selling (walk back)", "script_post")

        val sell = card("Selling")
        sw(sell, "Auto sell", "sell")
        field(sell, "Sell every N catches", "every")
        field(sell, "Menu delay (ms)", "mdelay")

        val wh = card("Discord webhook")
        sw(wh, "Send webhook messages", "whon")
        add(wh, tv("Webhook URLs (one per line, you can add several channels)", 12f, cMut), 10)
        val url = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_VARIATION_URI
            setText(Cfg.gets(this@MainActivity, "webhook"))
            setTextColor(cTx)
            background = shape(cBg, 10, cBd)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { Cfg.puts(this@MainActivity, "webhook", s.toString().trim()) }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        add(wh, url, 4)
        field(wh, "Progress message every N catches (0 = off)", "whevery")
        add(wh, btn("Send test message", cAcc) {
            Hook.send(this, "Test", "Sol Macro webhook works", 0x38D39F)
            Toast.makeText(this, "Sent, check the log below", Toast.LENGTH_SHORT).show()
        }, 12)

        val ex = card("Anti-kick and screenshots")
        sw(ex, "Anti-kick (small camera move when nothing was tapped)", "afk")
        field(ex, "Anti-kick after N minutes without taps", "afkmin")
        sw(ex, "Send a screenshot to the webhooks (needs fast capture)", "shoton")
        field(ex, "Screenshot every N minutes", "shotmin")

        val bw = card("Biome watch (reads the screen text)")
        add(bw, tv("Reads the biome name from the screen and sends a webhook message when it changes. Needs fast capture. Set it up in the panel: Settings > Set biome area, then tap the top-left and bottom-right corners of the biome name text. Reading can be wrong.", 12f, cMut), 6)
        sw(bw, "Biome watch", "bio")
        field(bw, "Check every N seconds (minimum 5)", "biosec")

        val rj = card("Auto rejoin")
        add(rj, tv("If Roblox closes, crashes or shows a disconnect screen, the macro opens Roblox again (and your private server link if you add one) and carries on. Needs fast capture for the disconnect screen check.", 12f, cMut), 6)
        sw(rj, "Auto rejoin", "rejoin")
        textField(rj, "Private server link (optional)", "pslink", "https://www.roblox.com/games/...?privateServerLinkCode=...")
        field(rj, "Seconds to wait for Roblox to load", "rejoinsec")

        val ww = card("Word watch (merchant, auras, anything)")
        add(ww, tv("Reads the screen text and sends a webhook message with a picture when one of your words shows up, for example Mari, Jester or an aura name. Set the area in the panel: Settings > Set watch area (leave it unset to read the whole screen, slower). Reading can be wrong. The same word is only reported once every 5 minutes.", 12f, cMut), 6)
        sw(ww, "Word watch", "watch")
        textField(ww, "Words (separated by commas)", "watchwords", "mari, jester")
        field(ww, "Check every N seconds (minimum 3)", "watchsec")
        sw(ww, "Ping me when a word is seen", "pingwatch")
        sw(ww, "Ping me on rare biomes (needs biome watch)", "pingrare")
        textField(ww, "Discord user ID to ping (type & before a role ID)", "pingid", "123456789012345678")

        val tk = card("Timed tasks (auto craft, buy, biome items)")
        add(tk, tv("Runs your script every N minutes between casts. Use the same commands as above, plus: tap p1 (extra points P1 to P6, set in the panel: Settings > Set extra points) or tap 540 1200 (screen pixels). Example for a craft: tap p1, wait 1, tap p2, wait 1.", 12f, cMut), 6)
        for (n in 1..3) {
            sw(tk, "Task $n on", "t" + n + "on")
            field(tk, "Task $n every N minutes", "t" + n + "min")
            script(tk, "Task $n script", "t" + n + "s")
        }

        val lg = card("Log")
        logTv = tv("", 11f, cMut).apply { typeface = Typeface.MONOSPACE }
        add(lg, logTv, 8)
        add(lg, btn("Copy log", cBd) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Sol Macro log", Dbg.tail(150)))
            Toast.makeText(this, "Log copied", Toast.LENGTH_SHORT).show()
        }, 10)

        if (intent.getBooleanExtra("capture", false)) {
            autoReturn = true
            requestCapture()
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(cBg)
            addView(root)
        })
    }

    // the panel asked for fast capture while this screen was already open
    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        setIntent(i)
        if (i.getBooleanExtra("capture", false)) {
            autoReturn = true
            requestCapture()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(tick)
        logoAnim?.cancel()
        logoAnim = ObjectAnimator.ofFloat(logo, "rotation", -4f, 4f).apply {
            duration = 2400
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
        logoAnim?.cancel()
        logoAnim = null
    }

    private fun refresh() {
        val on = (Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: "")
            .contains("$packageName/")
        bump(pill, if (on) "Service ON" else "Service OFF")
        pill.background = shape(if (on) cOk else cBad, 20)
        pill.setTextColor(if (on) 0xFF06251C.toInt() else Color.WHITE)
        bump(stateTv, "State: " + Dbg.state)
        val run = if (Dbg.startMs > 0 && Dbg.state != "Idle") (SystemClock.uptimeMillis() - Dbg.startMs) / 1000 else 0L
        statsTv.text = "Catches ${Dbg.catches}    Failed ${Dbg.fails}    Sells ${Dbg.sells}    Run ${run / 60}:${(run % 60).toString().padStart(2, '0')}"
        perfTv.text = "Capture: " + (if (Cap.active) "fast" else "slow (screenshots)") + "    ${Dbg.fps} checks/s    ${Dbg.latency} ms    bite ${Dbg.biteMs} ms"
        posTv.text = Cfg.keys.joinToString("   ") { k -> "${Cfg.label[k]}: ${Cfg.get(this, k + "_x")},${Cfg.get(this, k + "_y")}" }
        logTv.text = Dbg.tail(25).ifEmpty { "No log yet" }
    }
}
