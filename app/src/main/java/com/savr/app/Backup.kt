package com.savr.app

import org.json.JSONArray
import org.json.JSONObject

class BackupException(message: String) : Exception(message)

/** Format backup SAVR: satu file JSON berisi goals, transaksi, dan pengaturan. */
object Backup {
    const val FORMAT = 1
    const val MAX_BYTES = 20 * 1024 * 1024
    private const val MAX_TX = 200_000
    private const val MAX_GOALS = 500

    class Parsed(val goals: List<Goal>, val tx: List<SavingsTransaction>, val settings: List<Setting>)

    fun toJson(goals: List<Goal>, tx: List<SavingsTransaction>, settings: List<Setting>): String {
        val root = JSONObject()
        root.put("app", "SAVR")
        root.put("format", FORMAT)
        root.put("exportedAt", System.currentTimeMillis())
        val g = JSONArray()
        for (x in goals) {
            g.put(
                JSONObject().put("id", x.id).put("name", x.name).put("target", x.target)
                    .put("isMain", x.isMain).put("createdAt", x.createdAt)
            )
        }
        val t = JSONArray()
        for (x in tx) {
            val o = JSONObject().put("id", x.id).put("amount", x.amount).put("note", x.note)
                .put("type", x.type).put("category", x.category).put("createdAt", x.createdAt)
            if (x.goalId != null) o.put("goalId", x.goalId)
            t.put(o)
        }
        val s = JSONObject()
        for (x in settings) s.put(x.name, x.value)
        root.put("goals", g)
        root.put("transactions", t)
        root.put("settings", s)
        return root.toString()
    }

    fun parse(text: String): Parsed {
        try {
            val root = JSONObject(text)
            if (root.optString("app") != "SAVR") throw BackupException("Ini bukan file backup SAVR.")
            if (root.optInt("format", 0) !in 1..FORMAT) throw BackupException("Versi file backup tidak didukung.")

            val ga = root.optJSONArray("goals") ?: JSONArray()
            val ta = root.optJSONArray("transactions") ?: JSONArray()
            if (ga.length() > MAX_GOALS || ta.length() > MAX_TX) throw BackupException("Isi backup terlalu besar.")

            val goals = ArrayList<Goal>(ga.length())
            val goalIds = HashSet<Long>()
            var mainSeen = false
            for (i in 0 until ga.length()) {
                val o = ga.getJSONObject(i)
                val id = o.getLong("id")
                val name = o.getString("name").trim()
                val target = o.getLong("target")
                if (id <= 0 || !goalIds.add(id)) throw BackupException("Data goal tidak valid.")
                if (name.isEmpty() || name.length > Rules.MAX_GOAL) throw BackupException("Nama goal tidak valid.")
                if (target <= 0 || target > Rules.MAX_AMOUNT) throw BackupException("Target goal tidak valid.")
                val main = o.optBoolean("isMain", false) && !mainSeen
                if (main) mainSeen = true
                goals.add(Goal(id, name, target, main, o.optLong("createdAt", System.currentTimeMillis())))
            }
            if (!mainSeen && goals.isNotEmpty()) goals[0] = goals[0].copy(isMain = true)

            val txs = ArrayList<SavingsTransaction>(ta.length())
            val txIds = HashSet<Long>()
            for (i in 0 until ta.length()) {
                val o = ta.getJSONObject(i)
                val id = o.getLong("id")
                val type = o.getString("type")
                val amount = o.getLong("amount")
                val at = o.getLong("createdAt")
                if (id <= 0 || !txIds.add(id)) throw BackupException("ID transaksi ganda atau tidak valid.")
                if (type != TYPE_IN && type != TYPE_OUT) throw BackupException("Jenis transaksi tidak valid.")
                if (amount <= 0 || amount > Rules.MAX_AMOUNT) throw BackupException("Nominal transaksi tidak valid.")
                if (at < Rules.MIN_TIME) throw BackupException("Tanggal transaksi tidak valid.")
                val cat = o.optString("category", "").let { if (Categories.isValid(it, type)) it else "OTHER" }
                val gid = if (o.has("goalId") && !o.isNull("goalId")) o.getLong("goalId").takeIf { it in goalIds } else null
                txs.add(SavingsTransaction(id, amount, o.optString("note", "").take(Rules.MAX_NOTE), type, cat, gid, at))
            }
            val buckets: Set<Long?> = goalIds.toSet() + setOf<Long?>(null)
            if (Rules.hasNegative(txs, buckets)) throw BackupException("Data backup tidak konsisten (saldo negatif).")

            val so = root.optJSONObject("settings") ?: JSONObject()
            val settings = ArrayList<Setting>()
            so.optString(Keys.NAME).takeIf { it.isNotBlank() }?.let { settings.add(Setting(Keys.NAME, it.trim().take(Rules.MAX_NAME))) }
            so.optString(Keys.THEME).takeIf { v -> ThemeMode.entries.any { it.name == v } }?.let { settings.add(Setting(Keys.THEME, it)) }
            so.optString(Keys.MOTION).takeIf { it == "0" || it == "1" }?.let { settings.add(Setting(Keys.MOTION, it)) }
            so.optString(Keys.HAPTICS).takeIf { it == "0" || it == "1" }?.let { settings.add(Setting(Keys.HAPTICS, it)) }
            return Parsed(goals, txs, settings)
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException("File backup rusak atau bukan dari SAVR.")
        }
    }
}
