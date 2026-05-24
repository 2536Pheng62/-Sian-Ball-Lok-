package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String = "#3F51B5", // Hex representation of color for UI
    val championGuess: String = "",   // Predicted champion team name
    val googleEmail: String? = null,
    val isGoogleUser: Boolean = false
)

@Entity(tableName = "matches")
data class Match(
    @PrimaryKey val id: Int,
    val teamA: String,
    val teamAFlag: String, // Emoji representation
    val teamB: String,
    val teamBFlag: String, // Emoji representation
    val stage: String,     // e.g. "รอบแบ่งกลุ่ม", "รอบ 16 ทีม", "รอบ 8 ทีม", "รอบรองชนะเลิศ", "รอบชิงชนะเลิศ"
    val actualResult: String = "PENDING", // PENDING, A_WIN, DRAW, B_WIN
    val teamAScore: Int = -1, // -1 means not played yet
    val teamBScore: Int = -1  // -1 means not played yet
) {
    fun isFinished(): Boolean {
        return actualResult != "PENDING"
    }
}

@Entity(tableName = "predictions", primaryKeys = ["userId", "matchId"])
data class Prediction(
    val userId: Int,
    val matchId: Int,
    val predictedResult: String // A_WIN, DRAW, B_WIN, PENDING
)

@Dao
interface PredictionDao {
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsersDirectly(): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Int)

    @Query("SELECT * FROM matches ORDER BY id ASC")
    fun getAllMatches(): Flow<List<Match>>

    @Query("SELECT * FROM matches ORDER BY id ASC")
    suspend fun getAllMatchesDirectly(): List<Match>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<Match>)

    @Update
    suspend fun updateMatch(match: Match)

    @Query("UPDATE matches SET actualResult = 'PENDING', teamAScore = -1, teamBScore = -1")
    suspend fun resetAllMatchResults()

    @Query("SELECT * FROM predictions")
    fun getAllPredictions(): Flow<List<Prediction>>

    @Query("SELECT * FROM predictions WHERE userId = :userId")
    fun getPredictionsForUser(userId: Int): Flow<List<Prediction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: Prediction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPredictions(predictions: List<Prediction>)

    @Query("DELETE FROM predictions WHERE userId = :userId")
    suspend fun deletePredictionsForUser(userId: Int)
}

@Database(entities = [User::class, Match::class, Prediction::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun predictionDao(): PredictionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "world_cup_predictor.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
