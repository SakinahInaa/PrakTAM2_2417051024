package com.example.praktam2_2417051024.model

import com.google.gson.annotations.SerializedName

data class Habit(

    @SerializedName("nama")
    val nama: String,

    @SerializedName("deskripsi")
    val deskripsi: String,

    @SerializedName("deskripsi_panjang")
    val deskripsiPanjang: String,

    @SerializedName("kategori")
    val kategori: String,

    @SerializedName("image_url")
    val imageUrl: String
)