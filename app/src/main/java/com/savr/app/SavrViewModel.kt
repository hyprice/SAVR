package com.savr.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Lapisan overlay (sheet) yang tampil di atas halaman utama. */
sealed class Overlay(val key: String) {
    class TxEditor(val editing: SavingsTransaction?, val preset: String, val presetGoalId: Long?) : Overlay("tx-edit")
    class TxDetail(val tx: SavingsTransaction) : Overlay("tx-detail")
    class GoalEditor(val goal: Goal?) : Overlay("goal-edit")
    class Confirm(
        val title: String,
        val message: String,
        val confirmLabel: String,
        val danger: Boolean,
        val onConfirm: () -> Unit
    ) : Overlay("confirm")
    data object About : Overlay("about")
}

class SavrVmFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = SavrViewModel(app) as T
}

class SavrViewModel(app: Application) : AndroidViewModel(app) {
    private val db = SavingsDatabase.get(app)
    private val dao = db.savingsDao()

    private val retry = MutableStateFlow(0)
    private val _dbError = MutableStateFlow<String?>(null)
    val dbError: StateFlow<String?> = _dbError

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _overlay = MutableStateFlow<Overlay?>(null)
    val overlay: StateFlow<Overlay?> = _overlay

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<String> = _events

    /** null = masih memuat. Semua agregasi dihitung di thread latar. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val ui: StateFlow<UiModel?> = retry
        .flatMapLatest {
            combine(dao.observeTransactions(), dao.observeGoals(), dao.observeSettings()) { tx, goals, st ->
                UiModel.build(tx, goals, st)
            }.catch { _dbError.value = it.message ?: "Database tidak dapat dibuka." }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun retryDatabase() {
        _dbError.value = null
        retry.value += 1
    }

    fun show(o: Overlay) { _overlay.value = o }
    fun dismiss() { _overlay.value = null }
    private fun toast(msg: String) { _events.tryEmit(msg) }

    private fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _busy.value = false
                toast("Terjadi kesalahan: ${e.message ?: "database"}")
            }
        }
    }

    // ------------------------------------------------------------ Transaksi

    /** Mengembalikan pesan error (untuk ditampilkan di form) atau null bila valid dan sedang disimpan. */
    fun saveTransaction(
        editingId: Long?, type: String, amount: Long, category: String,
        goalId: Long?, note: String, whenMs: Long
    ): String? {
        val m = ui.value ?: return "Data belum siap, coba lagi."
        val cleanNote = note.trim().ifEmpty { Categories.label(category) }
        val cand = SavingsTransaction(
            id = editingId ?: 0L, amount = amount, note = cleanNote, type = type,
            category = category, goalId = goalId, createdAt = whenMs
        )
        Rules.validateTx(m.tx, m.goals.map { it.goal.id }.toSet(), cand, editingId)?.let { return it }
        launchSafe {
            if (editingId == null) dao.insertTransaction(cand) else dao.updateTransaction(cand)
            _overlay.value = null
            toast(if (editingId == null) "Transaksi ditambahkan" else "Perubahan disimpan")
        }
        return null
    }

    fun deleteTransaction(t: SavingsTransaction) {
        val m = ui.value ?: return
        if (Rules.breaksIfRemoved(m.tx, t)) {
            _overlay.value = null
            toast("Tidak bisa dihapus: pengeluaran setelahnya membutuhkan saldo ini.")
            return
        }
        launchSafe {
            dao.deleteTransaction(t)
            _overlay.value = null
            toast("Transaksi dihapus")
        }
    }

    // ------------------------------------------------------------ Goals

    fun saveGoal(editing: Goal?, name: String, target: Long?, makeMain: Boolean): String? {
        val m = ui.value ?: return "Data belum siap, coba lagi."
        val n = name.trim()
        if (n.isEmpty() || n.length > Rules.MAX_GOAL) return "Nama goal wajib diisi (maksimal ${Rules.MAX_GOAL} karakter)."
        if (target == null || target <= 0 || target > Rules.MAX_AMOUNT) return "Masukkan nominal target yang valid."
        launchSafe {
            db.withTransaction {
                val id = if (editing == null) {
                    dao.insertGoal(Goal(name = n, target = target, isMain = false))
                } else {
                    dao.updateGoal(editing.copy(name = n, target = target))
                    editing.id
                }
                if (makeMain || m.goals.isEmpty()) dao.setMain(id)
            }
            _overlay.value = null
            toast(if (editing == null) "Goal ditambahkan" else "Goal diperbarui")
        }
        return null
    }

    fun setMainGoal(g: Goal) = launchSafe {
        dao.setMain(g.id)
        toast("\"${g.name}\" kini goal utama")
    }

    fun deleteGoal(g: Goal) = launchSafe {
        db.withTransaction {
            dao.detachTransactions(g.id)
            dao.deleteGoal(g.id)
            if (g.isMain) dao.firstGoalId()?.let { dao.setMain(it) }
        }
        _overlay.value = null
        toast("Goal dihapus. Saldonya dipindah ke \"Tanpa goal\".")
    }

    // ------------------------------------------------------------ Pengaturan

    fun setSetting(name: String, value: String) = launchSafe { dao.putSetting(Setting(name, value)) }

    fun saveName(name: String) = launchSafe {
        dao.putSetting(Setting(Keys.NAME, name.trim().take(Rules.MAX_NAME)))
        toast("Nama disimpan")
    }

    // ------------------------------------------------------------ Backup & restore

    fun exportBackup(uri: Uri) = launchSafe {
        _busy.value = true
        try {
            withContext(Dispatchers.IO) {
                val json = Backup.toJson(dao.allGoals(), dao.allTransactions(), dao.allSettings())
                val out = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                    ?: throw IOException("Lokasi file tidak bisa ditulis.")
                out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }
            toast("Backup tersimpan")
        } finally {
            _busy.value = false
        }
    }

    fun requestRestore(uri: Uri) {
        show(
            Overlay.Confirm(
                title = "Pulihkan data?",
                message = "Semua transaksi, goal, dan pengaturan saat ini akan DIGANTI dengan isi file backup. " +
                    "Tindakan ini tidak bisa dibatalkan. Disarankan membuat backup dulu.",
                confirmLabel = "Pulihkan",
                danger = true,
                onConfirm = { restoreBackup(uri) }
            )
        )
    }

    private fun restoreBackup(uri: Uri) = launchSafe {
        _overlay.value = null
        _busy.value = true
        try {
            val parsed = withContext(Dispatchers.IO) { readBackup(uri) }
            db.withTransaction {
                dao.clearTransactions()
                dao.clearGoals()
                dao.clearSettings()
                dao.insertGoals(parsed.goals)
                dao.insertTransactions(parsed.tx)
                dao.putSettings(parsed.settings)
            }
            toast("Data dipulihkan: ${parsed.tx.size} transaksi, ${parsed.goals.size} goal")
        } catch (e: BackupException) {
            toast(e.message ?: "File backup tidak valid.")
        } finally {
            _busy.value = false
        }
    }

    private fun readBackup(uri: Uri): Backup.Parsed {
        val input = getApplication<Application>().contentResolver.openInputStream(uri)
            ?: throw BackupException("File tidak bisa dibuka.")
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0
        input.use {
            while (true) {
                val n = it.read(buf)
                if (n < 0) break
                total += n
                if (total > Backup.MAX_BYTES) throw BackupException("File terlalu besar untuk backup SAVR.")
                out.write(buf, 0, n)
            }
        }
        return Backup.parse(out.toString(Charsets.UTF_8.name()))
    }
}
