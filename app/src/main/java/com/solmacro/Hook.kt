package com.solmacro

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL

object Hook {
    // one or more webhook URLs, separated by new lines, spaces or commas
    private fun urls(c: Context): List<String> =
        Cfg.gets(c, "webhook").split(Regex("[\\s,;]+")).map { it.trim() }.filter { it.startsWith("https://") }

    // "<@id>" for a user, "<@&id>" for a role (type & before the number in the settings). Empty if not set.
    fun mention(c: Context): String {
        val raw = Cfg.gets(c, "pingid").trim()
        val id = raw.filter { it.isDigit() }
        if (id.isEmpty()) return ""
        return if (raw.startsWith("&")) "<@&" + id + ">" else "<@" + id + ">"
    }

    fun send(c: Context, title: String, desc: String, color: Int = 0x3EA6FF, ping: Boolean = false) {
        val list = urls(c)
        if (Cfg.get(c, "whon") != 1 || list.isEmpty()) return
        val m = if (ping) mention(c) else ""
        val content = if (m.isEmpty()) "" else "\"content\":\"" + m + "\","
        Thread {
            for (url in list) {
                try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("User-Agent", "SolMacro/1.0")
                    conn.doOutput = true
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    val body = "{\"username\":\"Sol Macro\"," + content + "\"embeds\":[{\"title\":\"" + esc(title) +
                        "\",\"description\":\"" + esc(desc) + "\",\"color\":" + color + "}]}"
                    conn.outputStream.use { it.write(body.toByteArray()) }
                    Dbg.add("Webhook replied HTTP " + conn.responseCode)
                    conn.disconnect()
                } catch (e: Exception) {
                    Dbg.add("Webhook failed: " + e.message)
                }
            }
        }.start()
    }

    // sends a JPEG picture (periodic screenshots) to every webhook
    fun sendImage(c: Context, text: String, jpg: ByteArray) {
        val list = urls(c)
        if (Cfg.get(c, "whon") != 1 || list.isEmpty()) return
        Thread {
            for (url in list) {
                try {
                    val b = "----solmacro" + System.currentTimeMillis()
                    val nl = "\r\n"
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + b)
                    conn.setRequestProperty("User-Agent", "SolMacro/1.0")
                    conn.doOutput = true
                    conn.connectTimeout = 8000
                    conn.readTimeout = 20000
                    conn.outputStream.use { o ->
                        o.write(("--" + b + nl + "Content-Disposition: form-data; name=\"payload_json\"" + nl +
                            "Content-Type: application/json" + nl + nl +
                            "{\"username\":\"Sol Macro\",\"content\":\"" + esc(text) + "\"}" + nl).toByteArray())
                        o.write(("--" + b + nl + "Content-Disposition: form-data; name=\"files[0]\"; filename=\"screen.jpg\"" + nl +
                            "Content-Type: image/jpeg" + nl + nl).toByteArray())
                        o.write(jpg)
                        o.write((nl + "--" + b + "--" + nl).toByteArray())
                    }
                    Dbg.add("Screenshot webhook replied HTTP " + conn.responseCode)
                    conn.disconnect()
                } catch (e: Exception) {
                    Dbg.add("Screenshot webhook failed: " + e.message)
                }
            }
        }.start()
    }

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
}
