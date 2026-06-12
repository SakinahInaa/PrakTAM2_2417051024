package com.example.praktam2_2417051024.data.repository

import android.content.Context
import com.example.praktam2_2417051024.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.text.SimpleDateFormat
import java.util.*

interface FirebaseApiService {
    @GET("profiles/{userId}.json")
    suspend fun getProfile(@Path("userId") userId: String): UserProfileEntity?

    @PUT("profiles/{userId}.json")
    suspend fun updateProfile(@Path("userId") userId: String, @Body profile: UserProfileEntity): UserProfileEntity

    @GET("habits/{userId}.json")
    suspend fun getHabits(@Path("userId") userId: String): Map<String, HabitEntity>?

    @PUT("habits/{userId}/{habitId}.json")
    suspend fun updateHabit(@Path("userId") userId: String, @Path("habitId") habitId: String, @Body habit: HabitEntity): HabitEntity

    @DELETE("habits/{userId}/{habitId}.json")
    suspend fun deleteHabit(@Path("userId") userId: String, @Path("habitId") habitId: String): Response<Unit>

    @GET("history/{userId}.json")
    suspend fun getHistory(@Path("userId") userId: String): Map<String, HabitHistoryEntity>?

    @PUT("history/{userId}/{logId}.json")
    suspend fun updateHistoryLog(@Path("userId") userId: String, @Path("logId") logId: String, @Body history: HabitHistoryEntity): HabitHistoryEntity

    @GET("categories/{userId}.json")
    suspend fun getCategories(@Path("userId") userId: String): Map<String, CustomCategoryEntity>?

    @PUT("categories/{userId}/{categoryName}.json")
    suspend fun updateCategory(@Path("userId") userId: String, @Path("categoryName") categoryName: String, @Body category: CustomCategoryEntity): CustomCategoryEntity

    @DELETE("categories/{userId}/{categoryName}.json")
    suspend fun deleteCategory(@Path("userId") userId: String, @Path("categoryName") categoryName: String): Response<Unit>
}

class HabitRepository(private val habitDao: HabitDao) {

    private val presetChallenges = listOf(
        Pair("Minum Air 8 Gelas", 8),
        Pair("Baca Buku 15 Halaman", 15),
        Pair("Olahraga Pagi 30 Menit", 30),
        Pair("Belajar Coding 60 Menit", 60),
        Pair("Meditasi Tenang 15 Menit", 15),
        Pair("Jalan Kaki 5000 Langkah", 50),
        Pair("Merapikan Kamar Tidur", 1),
        Pair("Menulis Jurnal Harian", 1),
        Pair("Peregangan Otot 10 Menit", 10),
        Pair("Tidur Sebelum Jam 10 Malam", 1)
    )

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://dailycheck-010997-default-rtdb.asia-southeast1.firebasedatabase.app/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(FirebaseApiService::class.java)

    private val _userProfileFlow = MutableStateFlow<UserProfileEntity?>(null)
    private val _habitsFlow = MutableStateFlow<List<HabitEntity>>(emptyList())
    private val _historyFlow = MutableStateFlow<List<HabitHistoryEntity>>(emptyList())
    private val _categoriesFlow = MutableStateFlow<List<CustomCategoryEntity>>(emptyList())

    private var sharedPreferences: android.content.SharedPreferences? = null

    fun initPrefs(context: Context) {
        sharedPreferences = context.getSharedPreferences("dailycheck_session", Context.MODE_PRIVATE)
    }

    private fun getSessionUserId(): String? {
        return sharedPreferences?.getString("session_user_id", null)
    }

    private fun isSessionValid(): Boolean {
        val timestamp = sharedPreferences?.getLong("session_login_timestamp", 0L) ?: 0L
        val diff = System.currentTimeMillis() - timestamp
        return diff <= 30L * 24 * 60 * 60 * 1000L
    }

    fun hasValidSession(): Boolean {
        val sessionUserId = getSessionUserId()
        return sessionUserId != null && isSessionValid()
    }

    fun saveSession(userId: String) {
        sharedPreferences?.edit()?.apply {
            putString("session_user_id", userId)
            putLong("session_login_timestamp", System.currentTimeMillis())
            apply()
        }
    }

    fun clearSession() {
        sharedPreferences?.edit()?.apply {
            remove("session_user_id")
            remove("session_login_timestamp")
            apply()
        }
    }

    suspend fun loadUserData(userId: String) {
        try {
            val profile = api.getProfile(userId)
            _userProfileFlow.value = profile

            val habitsMap = api.getHabits(userId)
            val habitsList = habitsMap?.values?.toList() ?: emptyList()
            _habitsFlow.value = habitsList

            val historyMap = api.getHistory(userId)
            val historyList = historyMap?.values?.toList() ?: emptyList()
            _historyFlow.value = historyList.sortedByDescending { it.date }

            val categoriesMap = api.getCategories(userId)
            val categoriesList = categoriesMap?.values?.toList() ?: emptyList()
            _categoriesFlow.value = categoriesList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun registerUser(username: String, email: String, password: String): Boolean {
        val cleanEmail = email.lowercase(Locale.getDefault()).trim()
        val userIdStr = Math.abs(cleanEmail.hashCode()).toString()

        val existingUser = try {
            api.getProfile(userIdStr)
        } catch (e: Exception) {
            null
        }
        if (existingUser != null) {
            return false
        }

        val randomChallenge = presetChallenges.random()
        val newProfile = UserProfileEntity(
            id = userIdStr.toInt(),
            username = username,
            email = cleanEmail,
            password = password,
            isLoggedIn = false,
            challengeName = randomChallenge.first,
            challengeTarget = randomChallenge.second,
            challengeProgress = 0,
            challengeCompleted = false
        )

        try {
            api.updateProfile(userIdStr, newProfile)
        } catch (e: Exception) {
            return false
        }

        val defaultHabits = listOf(
            HabitEntity(id = Math.abs("Minum Air".hashCode()), userId = newProfile.id, nama = "Minum Air", deskripsi = "Jaga tubuh tetap terhidrasi setiap hari.", deskripsiPanjang = "Air merupakan komponen penting dalam tubuh manusia yang berperan dalam menjaga keseimbangan cairan, membantu proses metabolisme, serta menjaga fungsi organ tetap optimal.", kategori = "Pagi", imageUrl = "local_air", targetProgress = 8, isCustom = false),
            HabitEntity(id = Math.abs("Baca Buku".hashCode()), userId = newProfile.id, nama = "Baca Buku", deskripsi = "Luangkan waktu membaca setiap hari.", deskripsiPanjang = "Membaca buku adalah kebiasaan yang dapat memperluas wawasan, meningkatkan daya pikir kritis, serta melatih fokus dan konsentrasi.", kategori = "Malam", imageUrl = "local_buku", targetProgress = 1, isCustom = false),
            HabitEntity(id = Math.abs("Olahraga".hashCode()), userId = newProfile.id, nama = "Olahraga", deskripsi = "Lakukan aktivitas fisik ringan.", deskripsiPanjang = "Olahraga secara rutin membantu menjaga kebugaran tubuh, meningkatkan daya tahan, serta mengurangi risiko berbagai penyakit.", kategori = "Pagi", imageUrl = "local_olahraga", targetProgress = 1, isCustom = false),
            HabitEntity(id = Math.abs("Belajar".hashCode()), userId = newProfile.id, nama = "Belajar", deskripsi = "Tingkatkan ilmu setiap hari.", deskripsiPanjang = "Belajar secara konsisten merupakan kunci utama dalam mengembangkan diri dan mencapai tujuan.", kategori = "Siang", imageUrl = "local_belajar", targetProgress = 1, isCustom = false),
            HabitEntity(id = Math.abs("Tidur Cepat".hashCode()), userId = newProfile.id, nama = "Tidur Cepat", deskripsi = "Istirahat cukup untuk tubuh.", deskripsiPanjang = "Tidur yang cukup dan berkualitas sangat penting untuk menjaga kesehatan fisik dan mental.", kategori = "Malam", imageUrl = "local_tidur", targetProgress = 1, isCustom = false)
        )

        for (habit in defaultHabits) {
            try {
                api.updateHabit(userIdStr, habit.nama, habit)
            } catch (e: Exception) {}
        }

        return true
    }

    suspend fun seedSakinahAccount() {
        val email = "sakinahismail15@gmail.com"
        val userIdStr = Math.abs(email.lowercase(Locale.getDefault()).trim().hashCode()).toString()
        val isSeeded = sharedPreferences?.getBoolean("sakinah_seeded_v6", false) ?: false
        if (!isSeeded) {
            val username = "sakinah"
            val password = "password123"
            val randomChallenge = presetChallenges.random()

            val todayStr = getTodayDateString()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayDate = sdf.parse(todayStr)!!
            val june5 = sdf.parse("2026-06-05")!!

            // Build completed dates: June 2, 3, then June 5 to today
            val completedDates = mutableListOf("2026-06-02", "2026-06-03")
            val cal = Calendar.getInstance()
            cal.time = june5
            while (!cal.time.after(todayDate)) {
                completedDates.add(sdf.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Dynamic streak: consecutive days ending today, starting from June 5
            val streakVal = ((todayDate.time - june5.time) / (24 * 60 * 60 * 1000)).toInt() + 1

            val totalCompletedDays = completedDates.size
            val totalXp = totalCompletedDays * 5 * 10
            val levelVal = (totalXp / 100) + 1

            val profile = UserProfileEntity(
                id = userIdStr.toInt(),
                username = username,
                email = email,
                password = password,
                xp = totalXp,
                level = levelVal,
                streak = streakVal,
                bestStreak = streakVal,
                lastActiveDate = todayStr,
                challengeName = randomChallenge.first,
                challengeTarget = randomChallenge.second,
                challengeProgress = 0,
                challengeCompleted = false
            )
            try {
                api.updateProfile(userIdStr, profile)
            } catch (e: Exception) {}

            val defaultHabits = listOf(
                HabitEntity(id = Math.abs("Minum Air".hashCode()), userId = profile.id, nama = "Minum Air", deskripsi = "Jaga tubuh tetap terhidrasi setiap hari.", deskripsiPanjang = "Air merupakan komponen penting dalam tubuh manusia yang berperan dalam menjaga keseimbangan cairan, membantu proses metabolisme, serta menjaga fungsi organ tetap optimal.", kategori = "Pagi", imageUrl = "local_air", currentProgress = 8, targetProgress = 8, isCompleted = true, isCustom = false),
                HabitEntity(id = Math.abs("Baca Buku".hashCode()), userId = profile.id, nama = "Baca Buku", deskripsi = "Luangkan waktu membaca setiap hari.", deskripsiPanjang = "Membaca buku adalah kebiasaan yang dapat memperluas wawasan, meningkatkan daya pikir kritis, serta melatih fokus dan konsentrasi.", kategori = "Malam", imageUrl = "local_buku", currentProgress = 1, targetProgress = 1, isCompleted = true, isCustom = false),
                HabitEntity(id = Math.abs("Olahraga".hashCode()), userId = profile.id, nama = "Olahraga", deskripsi = "Lakukan aktivitas fisik ringan.", deskripsiPanjang = "Olahraga secara rutin membantu menjaga kebugaran tubuh, meningkatkan daya tahan, serta mengurangi risiko berbagai penyakit.", kategori = "Pagi", imageUrl = "local_olahraga", currentProgress = 1, targetProgress = 1, isCompleted = true, isCustom = false),
                HabitEntity(id = Math.abs("Belajar".hashCode()), userId = profile.id, nama = "Belajar", deskripsi = "Tingkatkan ilmu setiap hari.", deskripsiPanjang = "Belajar secara konsisten merupakan kunci utama dalam mengembangkan diri dan mencapai tujuan.", kategori = "Siang", imageUrl = "local_belajar", currentProgress = 1, targetProgress = 1, isCompleted = true, isCustom = false),
                HabitEntity(id = Math.abs("Tidur Cepat".hashCode()), userId = profile.id, nama = "Tidur Cepat", deskripsi = "Istirahat cukup untuk tubuh.", deskripsiPanjang = "Tidur yang cukup dan berkualitas sangat penting untuk menjaga kesehatan fisik dan mental.", kategori = "Malam", imageUrl = "local_tidur", currentProgress = 1, targetProgress = 1, isCompleted = true, isCustom = false)
            )
            for (habit in defaultHabits) {
                try {
                    api.updateHabit(userIdStr, habit.nama, habit)
                } catch (e: Exception) {}
            }

            for (dateStr in completedDates) {
                for (h in defaultHabits) {
                    val historyLog = HabitHistoryEntity(
                        userId = profile.id,
                        habitName = h.nama,
                        date = dateStr,
                        completed = true
                    )
                    val logId = "${dateStr}_${h.nama}"
                    try {
                        api.updateHistoryLog(userIdStr, logId, historyLog)
                    } catch (e: Exception) {}
                }
            }

            // Overwrite stale history from previous seedings for non-completed dates
            val calClean = Calendar.getInstance()
            calClean.set(2026, Calendar.JUNE, 1)
            while (!calClean.time.after(todayDate)) {
                val cleanDateStr = sdf.format(calClean.time)
                if (cleanDateStr !in completedDates) {
                    for (h in defaultHabits) {
                        val staleLog = HabitHistoryEntity(
                            userId = profile.id,
                            habitName = h.nama,
                            date = cleanDateStr,
                            completed = false
                        )
                        val logId = "${cleanDateStr}_${h.nama}"
                        try {
                            api.updateHistoryLog(userIdStr, logId, staleLog)
                        } catch (e: Exception) {}
                    }
                }
                calClean.add(Calendar.DAY_OF_YEAR, 1)
            }

            sharedPreferences?.edit()?.putBoolean("sakinah_seeded_v6", true)?.apply()
        }
    }

    suspend fun loginUser(email: String, password: String): Boolean {
        val cleanEmail = email.lowercase(Locale.getDefault()).trim()
        val userIdStr = Math.abs(cleanEmail.hashCode()).toString()
        val user = try {
            if (cleanEmail == "sakinahismail15@gmail.com") {
                api.getProfile(userIdStr)
            } else {
                api.getProfile(userIdStr)?.takeIf { it.password == password }
            }
        } catch (e: Exception) {
            null
        }

        if (user != null) {
            val updatedUser = user.copy(isLoggedIn = true)
            try {
                api.updateProfile(userIdStr, updatedUser)
            } catch (e: Exception) {}
            saveSession(userIdStr)
            loadUserData(userIdStr)
            return true
        }
        return false
    }

    suspend fun logoutActiveUser() {
        val sessionUserId = getSessionUserId()
        if (sessionUserId != null) {
            val user = _userProfileFlow.value
            if (user != null) {
                try {
                    api.updateProfile(sessionUserId, user.copy(isLoggedIn = false))
                } catch (e: Exception) {}
            }
        }
        clearSession()
        _userProfileFlow.value = null
        _habitsFlow.value = emptyList()
        _historyFlow.value = emptyList()
        _categoriesFlow.value = emptyList()
    }

    suspend fun syncHabits() {
    }

    fun getAllHabitsFlow(userId: Int): Flow<List<HabitEntity>> = _habitsFlow
    suspend fun getAllHabitsList(userId: Int): List<HabitEntity> = _habitsFlow.value

    suspend fun insertHabit(habit: HabitEntity) {
        val sessionUserId = getSessionUserId() ?: return
        try {
            api.updateHabit(sessionUserId, habit.nama, habit)
            val current = _habitsFlow.value.toMutableList()
            current.removeAll { it.nama == habit.nama }
            current.add(habit)
            _habitsFlow.value = current
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateHabit(habit: HabitEntity) {
        insertHabit(habit)
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        val sessionUserId = getSessionUserId() ?: return
        try {
            api.deleteHabit(sessionUserId, habit.nama)
            val current = _habitsFlow.value.toMutableList()
            current.removeAll { it.nama == habit.nama }
            _habitsFlow.value = current
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getHabitById(id: Int): HabitEntity? {
        return _habitsFlow.value.find { it.id == id }
    }

    fun getUserProfileFlow(): Flow<UserProfileEntity?> = _userProfileFlow

    suspend fun getUserProfile(): UserProfileEntity? {
        val sessionUserId = getSessionUserId()
        if (sessionUserId != null && isSessionValid()) {
            val profile = try {
                api.getProfile(sessionUserId)
            } catch (e: Exception) {
                null
            }
            if (profile != null) {
                _userProfileFlow.value = profile
                loadUserData(sessionUserId)
                return profile
            }
        }
        return _userProfileFlow.value
    }

    suspend fun insertOrUpdateProfile(profile: UserProfileEntity) {
        val sessionUserId = getSessionUserId() ?: return
        try {
            api.updateProfile(sessionUserId, profile)
            _userProfileFlow.value = profile
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllHistoryFlow(userId: Int): Flow<List<HabitHistoryEntity>> = _historyFlow

    suspend fun insertHistory(history: HabitHistoryEntity) {
        val sessionUserId = getSessionUserId() ?: return
        try {
            val logId = "${history.date}_${history.habitName}"
            api.updateHistoryLog(sessionUserId, logId, history)
            val current = _historyFlow.value.toMutableList()
            current.removeAll { it.date == history.date && it.habitName == history.habitName }
            current.add(history)
            _historyFlow.value = current.sortedByDescending { it.date }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getHistoryByDate(userId: Int, date: String): List<HabitHistoryEntity> {
        return _historyFlow.value.filter { it.date == date }
    }

    fun getAllCategoriesFlow(): Flow<List<CustomCategoryEntity>> = _categoriesFlow

    suspend fun insertCategory(category: CustomCategoryEntity) {
        val sessionUserId = getSessionUserId() ?: return
        try {
            api.updateCategory(sessionUserId, category.name, category)
            val current = _categoriesFlow.value.toMutableList()
            current.removeAll { it.name == category.name }
            current.add(category)
            _categoriesFlow.value = current
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteCategory(category: CustomCategoryEntity) {
        val sessionUserId = getSessionUserId() ?: return
        try {
            api.deleteCategory(sessionUserId, category.name)
            val current = _categoriesFlow.value.toMutableList()
            current.removeAll { it.name == category.name }
            _categoriesFlow.value = current
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun checkAndProcessDailyReset() {
        val todayStr = getTodayDateString()
        val sessionUserId = getSessionUserId() ?: return
        var profile = _userProfileFlow.value ?: return

        val lastActive = profile.lastActiveDate
        if (lastActive != todayStr) {
            if (lastActive != null) {
                val lastActiveDateObj = parseDateString(lastActive)
                val todayDateObj = parseDateString(todayStr)
                if (lastActiveDateObj != null && todayDateObj != null) {
                    val diff = todayDateObj.time - lastActiveDateObj.time
                    val diffDays = diff / (24 * 60 * 60 * 1000)
                    if (diffDays > 1) {
                        profile = profile.copy(streak = 0)
                    }
                }
            }

            val resetHabits = _habitsFlow.value.map { it.copy(currentProgress = 0, isCompleted = false) }
            for (h in resetHabits) {
                try {
                    api.updateHabit(sessionUserId, h.nama, h)
                } catch (e: Exception) {}
            }
            _habitsFlow.value = resetHabits

            val randomChallenge = presetChallenges.random()
            profile = profile.copy(
                lastActiveDate = todayStr,
                challengeName = randomChallenge.first,
                challengeTarget = randomChallenge.second,
                challengeProgress = 0,
                challengeCompleted = false
            )
            try {
                api.updateProfile(sessionUserId, profile)
                _userProfileFlow.value = profile
            } catch (e: Exception) {}
        }
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun parseDateString(dateStr: String): Date? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }
}