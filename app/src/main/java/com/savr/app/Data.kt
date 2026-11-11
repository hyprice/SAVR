package com.savr.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions")
data class SavingsTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val note: String,
    val type: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface SavingsDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavingsTransaction>>

    @Insert
    suspend fun insert(item: SavingsTransaction)

    @Query("DELETE FROM transactions")
    suspend fun clear()
}

@Database(entities = [SavingsTransaction::class], version = 1, exportSchema = false)
abstract class SavingsDatabase : RoomDatabase() {
    abstract fun savingsDao(): SavingsDao
    companion object {
        @Volatile private var INSTANCE: SavingsDatabase? = null
        fun get(context: Context): SavingsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SavingsDatabase::class.java,
                    "savr-local.db"
                ).build().also { INSTANCE = it }
            }
    }
}
