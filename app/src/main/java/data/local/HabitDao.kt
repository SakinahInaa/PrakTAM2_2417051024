package com.example.praktam2_2417051024.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // Habits queries
    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY isFavorite DESC, id ASC")
    fun getAllHabitsFlow(userId: Int): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY isFavorite DESC, id ASC")
    suspend fun getAllHabitsList(userId: Int): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHabitsIgnore(habits: List<HabitEntity>)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Int): HabitEntity?

    @Query("SELECT * FROM habits WHERE userId = :userId AND nama = :nama LIMIT 1")
    suspend fun getHabitByName(userId: Int, nama: String): HabitEntity?

    @Query("UPDATE habits SET isCompleted = 0, currentProgress = 0 WHERE userId = :userId")
    suspend fun resetDailyProgress(userId: Int)

    // User Profile / Auth queries
    @Query("SELECT * FROM user_profile WHERE isLoggedIn = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE email = :email AND password = :password LIMIT 1")
    suspend fun getUserByEmailAndPassword(email: String, password: String): UserProfileEntity?

    @Query("UPDATE user_profile SET isLoggedIn = 0")
    suspend fun resetAllLoggedInUsers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity): Long

    // Habit History queries
    @Query("SELECT * FROM habit_history WHERE userId = :userId ORDER BY date DESC, id DESC")
    fun getAllHistoryFlow(userId: Int): Flow<List<HabitHistoryEntity>>

    @Query("SELECT * FROM habit_history WHERE userId = :userId AND date = :date")
    suspend fun getHistoryByDate(userId: Int, date: String): List<HabitHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HabitHistoryEntity)

    // Custom Categories queries
    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<CustomCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CustomCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CustomCategoryEntity)
}
