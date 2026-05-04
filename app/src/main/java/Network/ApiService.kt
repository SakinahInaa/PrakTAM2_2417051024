package com.example.praktam2_2417051024.network

import com.example.praktam2_2417051024.model.Habit
import retrofit2.http.GET

interface ApiService {

    @GET("habit.json")
    suspend fun getHabits(): List<Habit>
}