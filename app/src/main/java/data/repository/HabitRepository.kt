package com.example.praktam2_2417051024.data.repository

import com.example.praktam2_2417051024.data.api.RetrofitClient
import com.example.praktam2_2417051024.data.model.Habit

class HabitRepository {
    private val apiService = RetrofitClient.instance

    suspend fun getHabits(): List<Habit> {
        return try {
            apiService.getHabits()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}