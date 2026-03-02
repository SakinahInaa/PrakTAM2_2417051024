package com.example.praktam2_2417051024.model

import androidx.annotation.DrawableRes

data class Habit(
    val nama: String,
    val deskripsi: String,
    @DrawableRes val imageRes: Int
)