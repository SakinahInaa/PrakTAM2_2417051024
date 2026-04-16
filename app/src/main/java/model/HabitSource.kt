package com.example.praktam2_2417051024.model

import com.example.praktam2_2417051024.R

object HabitSource {

    val dummyHabit = listOf(

        Habit(
            nama = "Minum Air",
            deskripsi = "Jaga tubuh tetap terhidrasi setiap hari.",
            deskripsiPanjang = "Air merupakan komponen penting dalam tubuh manusia yang berperan dalam menjaga keseimbangan cairan, membantu proses metabolisme, serta menjaga fungsi organ tetap optimal. Mengonsumsi minimal 8 gelas air setiap hari dapat membantu meningkatkan konsentrasi, menjaga kesehatan kulit, serta mencegah dehidrasi. Biasakan membawa botol minum sendiri dan minum secara berkala sepanjang hari, terutama setelah beraktivitas.",
            imageRes = R.drawable.air,
            kategori = "Pagi"
        ),

        Habit(
            nama = "Baca Buku",
            deskripsi = "Luangkan waktu membaca setiap hari.",
            deskripsiPanjang = "Membaca buku adalah kebiasaan yang dapat memperluas wawasan, meningkatkan daya pikir kritis, serta melatih fokus dan konsentrasi. Luangkan waktu sekitar 10–15 menit setiap hari untuk membaca, baik buku pengetahuan, novel, maupun artikel edukatif. Dengan membaca secara konsisten, kamu tidak hanya mendapatkan informasi baru, tetapi juga melatih otak agar tetap aktif dan produktif.",
            imageRes = R.drawable.buku,
            kategori = "Malam"
        ),

        Habit(
            nama = "Olahraga",
            deskripsi = "Lakukan aktivitas fisik ringan.",
            deskripsiPanjang = "Olahraga secara rutin membantu menjaga kebugaran tubuh, meningkatkan daya tahan, serta mengurangi risiko berbagai penyakit. Kamu tidak perlu melakukan olahraga berat, cukup dengan aktivitas ringan seperti stretching, jogging, atau workout singkat selama 15–30 menit. Lakukan secara konsisten setiap hari agar tubuh tetap sehat, bugar, dan lebih berenergi dalam menjalani aktivitas.",
            imageRes = R.drawable.olahraga,
            kategori = "Pagi"
        ),

        Habit(
            nama = "Belajar",
            deskripsi = "Tingkatkan ilmu setiap hari.",
            deskripsiPanjang = "Belajar secara konsisten merupakan kunci utama dalam mengembangkan diri dan mencapai tujuan. Luangkan waktu minimal 1 jam setiap hari untuk mempelajari hal baru atau mengulang materi yang sudah dipelajari. Dengan belajar secara rutin, kamu dapat meningkatkan pemahaman, memperkuat ingatan, serta membangun kebiasaan disiplin yang akan sangat bermanfaat di masa depan.",
            imageRes = R.drawable.belajar,
            kategori = "Siang"
        ),

        Habit(
            nama = "Tidur Cepat",
            deskripsi = "Istirahat cukup untuk tubuh.",
            deskripsiPanjang = "Tidur yang cukup dan berkualitas sangat penting untuk menjaga kesehatan fisik dan mental. Usahakan tidur sebelum pukul 23.00 agar tubuh mendapatkan waktu istirahat yang optimal. Tidur yang baik membantu proses pemulihan tubuh, meningkatkan konsentrasi, serta menjaga suasana hati tetap stabil. Hindari penggunaan gadget sebelum tidur agar kualitas tidur lebih maksimal.",
            imageRes = R.drawable.tidur,
            kategori = "Malam"
        )
    )
}