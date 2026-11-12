package com.savr.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

const val TYPE_IN = "IN"
const val TYPE_OUT = "OUT"

/** Tabel `transactions` (v1 + kolom category & goalId). Nilai type tetap "IN"/"OUT". */
@Entity(tableName = "transactions", indices = [Index("createdAt"), Index("goalId")])
data class SavingsTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val note: String,
    val type: String,
    val category: String,
    val goalId: Long?,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Long,
    val isMain: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class Setting(
    @PrimaryKey val name: String,
    val value: String
)

@Dao
interface SavingsDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC, id DESC")
    fun observeTransactions(): Flow<List<SavingsTransaction>>

    @Query("SELECT * FROM goals ORDER BY isMain DESC, createdAt ASC, id ASC")
    fun observeGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM settings")
    fun observeSettings(): Flow<List<Setting>>

    @Query("SELECT * FROM transactions ORDER BY createdAt ASC, id ASC")
    suspend fun allTransactions(): List<SavingsTransaction>

    @Query("SELECT * FROM goals ORDER BY id ASC")
    suspend fun allGoals(): List<Goal>

    @Query("SELECT * FROM settings")
    suspend fun allSettings(): List<Setting>

    @Insert
    suspend fun insertTransaction(item: SavingsTransaction): Long

    @Update
    suspend fun updateTransaction(item: SavingsTransaction)

    @Delete
    suspend fun deleteTransaction(item: SavingsTransaction)

    @Insert
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("UPDATE transactions SET goalId = NULL WHERE goalId = :id")
    suspend fun detachTransactions(id: Long)

    @Query("UPDATE goals SET isMain = (id = :id)")
    suspend fun setMain(id: Long)

    @Query("SELECT id FROM goals ORDER BY createdAt ASC, id ASC LIMIT 1")
    suspend fun firstGoalId(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSetting(setting: Setting)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSettings(list: List<Setting>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(list: List<SavingsTransaction>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(list: List<Goal>)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    @Query("DELETE FROM settings")
    suspend fun clearSettings()
}

@Database(
    entities = [SavingsTransaction::class, Goal::class, Setting::class],
    version = 2,
    exportSchema = false
)
abstract class SavingsDatabase : RoomDatabase() {
    abstract fun savingsDao(): SavingsDao

    companion object {
        @Volatile private var INSTANCE: SavingsDatabase? = null

        fun get(context: Context): SavingsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SavingsDatabase::class.java,
                    "savr-local.db" // nama file TIDAK boleh diubah: data v0.1 ada di sini
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(SeedCallback)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

/**
 * v1 -> v2. Semua transaksi lama dipertahankan (id, nominal, catatan, tipe, waktu sama persis).
 * Perilaku v0.1 (satu target, semua setoran menuju target itu) dipertahankan: dibuat goal utama
 * "Target Utama" Rp7.000.000 dan seluruh transaksi lama ditautkan ke goal tersebut.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        db.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `target` INTEGER NOT NULL, `isMain` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `settings` (`name` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`name`))")
        db.execSQL("INSERT INTO goals (id, name, target, isMain, createdAt) VALUES (1, 'Target Utama', 7000000, 1, $now)")

        db.execSQL("ALTER TABLE transactions RENAME TO transactions_old")
        db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` INTEGER NOT NULL, `note` TEXT NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, `goalId` INTEGER, `createdAt` INTEGER NOT NULL)")
        db.execSQL(
            "INSERT INTO transactions (id, amount, note, type, category, goalId, createdAt) " +
                "SELECT id, amount, note, type, CASE WHEN type = 'IN' THEN 'SAVING' ELSE 'WITHDRAW' END, 1, createdAt FROM transactions_old"
        )
        db.execSQL("DROP TABLE transactions_old")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_createdAt` ON `transactions` (`createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_goalId` ON `transactions` (`goalId`)")
    }
}

/** Instalasi baru: langsung ada satu goal utama supaya Home tidak kosong. */
private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO goals (name, target, isMain, createdAt) VALUES ('Target Utama', 7000000, 1, ${System.currentTimeMillis()})")
    }
}
