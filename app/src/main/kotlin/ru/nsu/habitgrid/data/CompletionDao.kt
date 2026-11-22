package ru.nsu.habitgrid.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions WHERE date BETWEEN :from AND :to")
    fun observeForRange(from: LocalDate, to: LocalDate): Flow<List<CompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: CompletionEntity): Long

    @Query("DELETE FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun delete(habitId: Long, date: LocalDate): Int
}
