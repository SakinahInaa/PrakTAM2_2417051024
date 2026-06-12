package com.example.praktam2_2417051024.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val nama: String,
    val deskripsi: String,
    val deskripsiPanjang: String,
    val kategori: String,
    val imageUrl: String,
    val isFavorite: Boolean = false,
    val currentProgress: Int = 0,
    val targetProgress: Int = 1,
    val isCompleted: Boolean = false,
    val reminderTime: String? = null,
    val reminderDays: String = "Setiap Hari",
    val isCustom: Boolean = false
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String = "User",
    val email: String = "user@gmail.com",
    val password: String? = null,
    val photoUrl: String? = null,
    val xp: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDate: String? = null,
    val isDarkMode: Boolean = false,
    val themeColor: String = "Hijau",
    val isLoggedIn: Boolean = false,
    val challengeName: String = "Minum Air 8 Gelas",
    val challengeTarget: Int = 8,
    val challengeProgress: Int = 0,
    val challengeCompleted: Boolean = false
)

@Entity(tableName = "habit_history")
data class HabitHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val habitName: String,
    val date: String,
    val completed: Boolean
)

@Entity(tableName = "custom_categories")
data class CustomCategoryEntity(
    @PrimaryKey val name: String
)
