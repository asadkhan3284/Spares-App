package com.sparesapp.register.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory_rows ORDER BY mat ASC")
    fun observeAll(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory_rows ORDER BY mat ASC")
    suspend fun getAllOnce(): List<InventoryEntity>

    @Query("SELECT COUNT(*) FROM inventory_rows")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<InventoryEntity>)

    @Query("DELETE FROM inventory_rows")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(rows: List<InventoryEntity>) {
        clear()
        insertAll(rows)
    }
}
