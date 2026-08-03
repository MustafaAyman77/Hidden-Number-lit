package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "match_records")
data class MatchRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val opponentName: String,
    val gameMode: String,
    val gameType: String,
    val secretNumber: String,
    val attemptsCount: Int,
    val isWin: Boolean,
    val durationSeconds: Int,
    val scoreEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MatchRecordDao {
    @Query("SELECT * FROM match_records ORDER BY timestamp DESC")
    fun getAllMatches(): Flow<List<MatchRecord>>

    @Query("SELECT COUNT(*) FROM match_records WHERE isWin = 1")
    fun getWinsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM match_records")
    fun getTotalMatchesCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(matchRecord: MatchRecord)

    @Query("DELETE FROM match_records")
    suspend fun clearHistory()
}

@Database(entities = [MatchRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun matchRecordDao(): MatchRecordDao
}
