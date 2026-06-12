package com.example.praktam2_2417051024.data.api

import com.example.praktam2_2417051024.data.model.Habit
import retrofit2.http.GET

interface ApiService {

    @GET("habit.json")
    suspend funetHabits(): List<Habit>
}