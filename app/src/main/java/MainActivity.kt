@file:Suppress("UNUSED_VALUE", "AssignedValueDoubleCheck")
package com.example.praktam2_2417051024

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.praktam2_2417051024.data.local.*
import com.example.praktam2_2417051024.data.receiver.HabitReminderReceiver
import com.example.praktam2_2417051024.data.repository.HabitRepository
import com.example.praktam2_2417051024.ui.theme.DailyCheckTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        setContent {
            val context = LocalContext.current
            val database = remember { HabitDatabase.getDatabase(context) }
            val repository = remember { HabitRepository(database.habitDao()).apply { initPrefs(context) } }
            val userProfile by repository.getUserProfileFlow().collectAsState(initial = null)

            val themeColor = userProfile?.themeColor ?: "Hijau"
            val isDark = userProfile?.isDarkMode ?: false

            DailyCheckTheme(themeColor = themeColor, isDarkTheme = isDark) {
                DailyCheckApp(repository)
            }
        }
    }
}

@SuppressLint("ScheduleExactAlarm")
fun scheduleHabitAlarm(context: Context, habit: HabitEntity) {
    if (habit.reminderTime.isNullOrBlank()) return
    try {
        val parts = habit.reminderTime.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra("habit_name", habit.nama)
            putExtra("habit_id", habit.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (se: SecurityException) {
            se.printStackTrace()
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun cancelHabitAlarm(context: Context, habitId: Int) {
    try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun getHabitImage(imageUrl: String): Any {
    return when (imageUrl) {
        "local_air" -> R.drawable.air
        "local_buku" -> R.drawable.buku
        "local_olahraga" -> R.drawable.olahraga
        "local_belajar" -> R.drawable.belajar
        "local_tidur" -> R.drawable.tidur
        else -> {
            if (imageUrl.startsWith("http")) imageUrl
            else R.drawable.ic_launcher_foreground // default fallback
        }
    }
}

fun getUserTitle(level: Int, streak: Int, xp: Int): String {
    return when {
        streak >= 30 -> "🏆 Master"
        streak >= 14 -> "🏆 Suhu"
        streak >= 7 -> "🏆 Konsisten"
        streak >= 3 -> "🏆 Rajin"
        streak >= 1 -> "🏆 Pemula"
        else -> "🌱 Pendatang Baru"
    }
}

@Composable
fun DailyCheckApp(repository: HabitRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController, repository)
        }
        composable("login") {
            LoginScreen(navController, repository)
        }
        composable("register") {
            RegisterScreen(navController, repository)
        }
        composable("main") {
            MainShell(repository, navController)
        }
        composable(
            "detail/{habitId}",
            arguments = listOf(navArgument("habitId") { type = NavType.IntType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getInt("habitId") ?: 0
            DetailScreen(habitId, repository, navController)
        }
    }
}

@Composable
fun SplashScreen(navController: NavHostController, repository: HabitRepository) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "Alpha Splash"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1000, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)),
        label = "Scale Splash"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        this.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.seedSakinahAccount()
                if (repository.hasValidSession()) {
                    repository.checkAndProcessDailyReset()
                }
            } catch (e: Exception) {}
        }

        val startTime = System.currentTimeMillis()
        val hasSession = repository.hasValidSession()

        if (hasSession) {
            kotlinx.coroutines.withTimeoutOrNull(1200) {
                try {
                    repository.getUserProfile()
                } catch (e: Exception) {}
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        val remainingDelay = maxOf(0L, 1500L - elapsed)
        delay(remainingDelay)

        if (hasSession) {
            navController.navigate("main") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alphaAnim)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scaleAnim)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(70.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "DailyCheck",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Your Ultimate Habit Tracker 🌿",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun LoginScreen(navController: NavHostController, repository: HabitRepository) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Masuk DailyCheck 🌿",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Mulai kebiasaan baik Anda hari ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Filled.Email, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, null) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Silakan isi semua bidang", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            isLoading = true
                            val success = repository.loginUser(email, password)
                            isLoading = false
                            if (success) {
                                Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "Email atau password salah", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Masuk", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                TextButton(onClick = { navController.navigate("register") }) {
                    Text("Belum punya akun? Daftar Sekarang")
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(navController: NavHostController, repository: HabitRepository) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Daftar Akun Baru 📝",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Buat akun untuk melacak kebiasaan Anda secara offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nama Lengkap") },
                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Filled.Email, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, null) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )

                Button(
                    onClick = {
                        if (username.isBlank() || email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Silakan isi semua bidang", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            isLoading = true
                            val success = repository.registerUser(username, email, password)
                            isLoading = false
                            if (success) {
                                Toast.makeText(context, "Registrasi Berhasil! Silakan login", Toast.LENGTH_LONG).show()
                                navController.navigate("login") {
                                    popUpTo("register") { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "Email sudah terdaftar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Daftar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                TextButton(onClick = { navController.navigate("login") }) {
                    Text("Sudah punya akun? Masuk")
                }
            }
        }
    }
}

@Composable
fun MainShell(repository: HabitRepository, navController: NavHostController) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showConfetti by remember { mutableStateOf(false) }

    val userProfile by repository.getUserProfileFlow().collectAsState(initial = null)

    val habits by remember(userProfile) {
        if (userProfile != null) repository.getAllHabitsFlow(userProfile!!.id)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    var prevAllCompleted by remember { mutableStateOf<Boolean?>(null) }
    var prevTitle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userProfile, habits) {
        val profile = userProfile
        if (profile != null) {
            val currentTitle = getUserTitle(profile.level, profile.streak, profile.xp)
            val allCompleted = habits.isNotEmpty() && habits.all { it.isCompleted }
            var trigger = false
            if (prevAllCompleted != null && allCompleted && !prevAllCompleted!!) {
                trigger = true
            }
            if (prevTitle != null && currentTitle != prevTitle!!) {
                val titlesOrder = listOf("🌱 Pendatang Baru", "🏆 Pemula", "🏆 Rajin", "🏆 Konsisten", "🏆 Suhu", "🏆 Master")
                val oldIdx = titlesOrder.indexOf(prevTitle)
                val newIdx = titlesOrder.indexOf(currentTitle)
                if (newIdx > oldIdx) {
                    trigger = true
                }
            }
            if (trigger) {
                showConfetti = true
                delay(4000)
                showConfetti = false
            }
            prevAllCompleted = allCompleted
            prevTitle = currentTitle
        } else {
            prevAllCompleted = null
            prevTitle = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Home, "Beranda") },
                    label = { Text("Beranda") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.CheckCircle, "Habit") },
                    label = { Text("Habit") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Star, "Analisis") },
                    label = { Text("Analisis") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.Person, "Profil") },
                    label = { Text("Profil") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    repository = repository,
                    userProfile = userProfile,
                    habits = habits,
                    snackbarHostState = snackbarHostState,
                    onNavigateToHabits = { selectedTab = 1 }
                )
                1 -> HabitListScreen(
                    repository = repository,
                    navController = navController,
                    habits = habits,
                    userProfile = userProfile,
                    snackbarHostState = snackbarHostState
                )
                2 -> AnalyticsScreen(habits = habits, repository = repository, userProfile = userProfile)
                3 -> ProfileScreen(repository = repository, userProfile = userProfile, mainNavController = navController)
            }

            if (showConfetti) {
                ConfettiCelebration(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun HomeScreen(
    repository: HabitRepository,
    userProfile: UserProfileEntity?,
    habits: List<HabitEntity>,
    snackbarHostState: SnackbarHostState,
    onNavigateToHabits: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showGuide by rememberSaveable { mutableStateOf(true) }
    val profile = userProfile ?: UserProfileEntity()
    val challengeProgress = profile.challengeProgress
    val challengeTarget = profile.challengeTarget
    val isChallengeCompleted = profile.challengeCompleted
    val totalCount = habits.size
    val completedCount = habits.count { it.isCompleted }
    val completionPercent = if (totalCount > 0) (completedCount.toFloat() / totalCount * 100).toInt() else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DailyCheck 🌿",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Halo, ${profile.username}!",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${profile.xp} XP",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Streak Harian", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            Text("${profile.streak} Hari Aktif", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFFF6D00))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Gelar Anda", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = getUserTitle(profile.level, profile.streak, profile.xp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Target Hari Ini",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "$completedCount/$totalCount Selesai",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val animatedProgress by animateFloatAsState(
                        targetValue = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f,
                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        label = "Progress Bar Harian"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "$completionPercent% Kebiasaan Selesai",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isChallengeCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isChallengeCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "🏆 Challenge Hari Ini",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "✓ ${profile.challengeName} ($challengeProgress/$challengeTarget)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Hadiah: +20 XP (Daily Double)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isChallengeCompleted) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Transparent, CircleShape)
                                    .clickable {
                                        scope.launch {
                                            val nextProgress = challengeProgress - 1
                                            val nextXp = maxOf(0, profile.xp - 20)
                                            val level = (nextXp / 100) + 1
                                            repository.insertOrUpdateProfile(profile.copy(
                                                xp = nextXp,
                                                level = level,
                                                challengeProgress = nextProgress,
                                                challengeCompleted = false
                                            ))
                                            snackbarHostState.showSnackbar("Challenge dibatalkan. XP berkurang 20.")
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    "Challenge Selesai",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (challengeProgress > 0) {
                                            val nextProgress = challengeProgress - 1
                                            scope.launch {
                                                repository.insertOrUpdateProfile(profile.copy(challengeProgress = nextProgress))
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                                ) {
                                    Text("-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (challengeProgress < challengeTarget) {
                                            val nextProgress = challengeProgress + 1
                                            val completed = nextProgress == challengeTarget
                                            scope.launch {
                                                val nextXp = if (completed) profile.xp + 20 else profile.xp
                                                val level = (nextXp / 100) + 1
                                                if (completed) {
                                                    snackbarHostState.showSnackbar("Challenge Selesai! +20 XP! 🎉")
                                                }
                                                repository.insertOrUpdateProfile(profile.copy(
                                                    challengeProgress = nextProgress,
                                                    challengeCompleted = completed,
                                                    xp = nextXp,
                                                    level = level
                                                ))
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                                ) {
                                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToHabits() }
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ayo selesaikan aktivitasmu! 🎯",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ketuk untuk melihat daftar kebiasaan hari ini",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        if (showGuide) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💡", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tips Penggunaan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { showGuide = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("🔍 Cari Habit: Ketik nama kebiasaan untuk menyaring daftar.", fontSize = 11.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("⏳ Tombol Panah Kebawah: Tekan untuk mengurutkan daftar berdasarkan Nama, Kategori, atau Progress.", fontSize = 11.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🏷️ Kategori Kustom: Tambahkan kategori baru di tab Profil agar muncul sebagai filter chip di atas.", fontSize = 11.sp, color = Color.DarkGray)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SortIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(x = size.width * 0.25f, y = size.height * 0.15f),
            end = androidx.compose.ui.geometry.Offset(x = size.width * 0.25f, y = size.height * 0.85f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(x = size.width * 0.25f, y = size.height * 0.85f),
            end = androidx.compose.ui.geometry.Offset(x = size.width * 0.1f, y = size.height * 0.65f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(x = size.width * 0.25f, y = size.height * 0.85f),
            end = androidx.compose.ui.geometry.Offset(x = size.width * 0.4f, y = size.height * 0.65f),
            strokeWidth = strokeWidth
        )
        val startX = size.width * 0.5f
        val lineThickness = 3.dp.toPx()
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(x = startX, y = size.height * 0.2f),
            end = androidx.compose.ui.geometry.Offset(x = size.width * 0.9f, y = size.height * 0.2f),
            strokeWidth = lineThickness
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(x = startX, y = size.height * 0.4f),
            end = androidx.compose.ui.geometry.Offset(x = size.width * 0.8f, y = size.height * 0.4f),
            strokeWidth = lineThickness
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(x = startX, y = size.height * 0.6f),
            end = androidx.compose.ui.geometry.Offset(x = size.width * 0.7f, y = size.height * 0.6f),
            strokeWidth = lineThickness
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(x = startX, y = size.height * 0.8f),
            end = androidx.compose.ui.geometry.Offset(x = size.width * 0.6f, y = size.height * 0.8f),
            strokeWidth = lineThickness
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    repository: HabitRepository,
    navController: NavHostController,
    habits: List<HabitEntity>,
    userProfile: UserProfileEntity?,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Semua") }
    var sortBy by remember { mutableStateOf("Nama") }
    var showAddDialog by remember { mutableStateOf(false) }

    val customCategories by repository.getAllCategoriesFlow().collectAsState(initial = emptyList())

    val profile = userProfile ?: UserProfileEntity()

    val categories = remember(customCategories) {
        listOf("Semua", "Pagi", "Siang", "Malam") + customCategories.map { it.name }
    }

    val filteredHabits = habits.filter { habit ->
        val matchesSearch = habit.nama.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "Semua" || habit.kategori == selectedCategory
        matchesSearch && matchesCategory
    }.sortedWith { h1, h2 ->
        when (sortBy) {
            "Kategori" -> h1.kategori.compareTo(h2.kategori)
            "Progress" -> (h2.currentProgress.toFloat() / h2.targetProgress).compareTo(h1.currentProgress.toFloat() / h1.targetProgress)
            else -> h1.nama.compareTo(h2.nama)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, "Tambah Habit", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Kebiasaan Saya 🌿",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari Habit...",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { query -> searchQuery = query },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                        ) {
                            SortIcon(modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Nama") },
                                onClick = { sortBy = "Nama"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Kategori") },
                                onClick = { sortBy = "Kategori"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Progress") },
                                onClick = { sortBy = "Progress"; showSortMenu = false }
                            )
                        }
                    }
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            if (filteredHabits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Info, null, tint = Color.LightGray, modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Tidak ada kebiasaan.", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                items(filteredHabits, key = { it.nama }) { habit ->
                    HabitCardItem(
                        habit = habit,
                        onCardClick = {
                            navController.navigate("detail/${habit.id}")
                        },
                        onIncrement = {
                            scope.launch {
                                val curr = habit.currentProgress
                                if (curr < habit.targetProgress) {
                                    val next = curr + 1
                                    val completed = next == habit.targetProgress
                                    val updated = habit.copy(currentProgress = next, isCompleted = completed)
                                    repository.updateHabit(updated)

                                    if (completed) {
                                        val nextXp = profile.xp + 10
                                        val levelVal = (nextXp / 100) + 1

                                        val todayStr = repository.getTodayDateString()
                                        var newStreak = profile.streak
                                        if (profile.lastActiveDate != todayStr) {
                                            newStreak++
                                        }
                                        val bestStreak = maxOf(newStreak, profile.bestStreak)

                                        repository.insertOrUpdateProfile(profile.copy(
                                            xp = nextXp,
                                            level = levelVal,
                                            streak = newStreak,
                                            bestStreak = bestStreak,
                                            lastActiveDate = todayStr
                                        ))

                                        repository.insertHistory(
                                            HabitHistoryEntity(
                                                userId = profile.id,
                                                habitName = habit.nama,
                                                date = todayStr,
                                                completed = true
                                            )
                                        )

                                        snackbarHostState.showSnackbar("Berhasil menyelesaikan ${habit.nama}! +10 XP 🔥")
                                    }
                                }
                            }
                        },
                        onDecrement = {
                            scope.launch {
                                val curr = habit.currentProgress
                                if (curr > 0) {
                                    val next = curr - 1
                                    val wasCompleted = habit.isCompleted
                                    val updated = habit.copy(currentProgress = next, isCompleted = false)
                                    repository.updateHabit(updated)

                                    if (wasCompleted) {
                                        val nextXp = maxOf(0, profile.xp - 10)
                                        val level = (nextXp / 100) + 1
                                        repository.insertOrUpdateProfile(profile.copy(xp = nextXp, level = level))
                                        snackbarHostState.showSnackbar("Progres kebiasaan dikurangi. XP berkurang 10.")
                                    }
                                }
                            }
                        },
                        onComplete = {
                            scope.launch {
                                if (!habit.isCompleted) {
                                    val updated = habit.copy(
                                        currentProgress = habit.targetProgress,
                                        isCompleted = true
                                    )
                                    repository.updateHabit(updated)

                                    val nextXp = profile.xp + 10
                                    val level = (nextXp / 100) + 1
                                    val todayStr = repository.getTodayDateString()
                                    var newStreak = profile.streak
                                    if (profile.lastActiveDate != todayStr) {
                                        newStreak++
                                    }
                                    val bestStreak = maxOf(newStreak, profile.bestStreak)

                                    repository.insertOrUpdateProfile(profile.copy(
                                        xp = nextXp,
                                        level = level,
                                        streak = newStreak,
                                        bestStreak = bestStreak,
                                        lastActiveDate = todayStr
                                    ))

                                    repository.insertHistory(
                                        HabitHistoryEntity(
                                            userId = profile.id,
                                            habitName = habit.nama,
                                            date = todayStr,
                                            completed = true
                                        )
                                    )

                                    snackbarHostState.showSnackbar("Berhasil menyelesaikan ${habit.nama}! +10 XP 🔥")
                                }
                            }
                        },
                        onFavoriteToggle = {
                            scope.launch {
                                repository.updateHabit(habit.copy(isFavorite = !habit.isFavorite))
                            }
                        }
                    )
                }
            }


        }

        if (showAddDialog) {
            AddEditHabitDialog(
                categories = categories.filter { it != "Semua" },
                onDismiss = { showAddDialog = false },
                onSave = { name, desc, detail, cat, target, time, imgKey ->
                    scope.launch {
                        val generatedId = Math.abs(java.util.UUID.randomUUID().hashCode())
                        val newHabit = HabitEntity(
                            id = generatedId,
                            userId = profile.id,
                            nama = name,
                            deskripsi = desc,
                            deskripsiPanjang = detail,
                            kategori = cat,
                            imageUrl = imgKey,
                            targetProgress = target,
                            reminderTime = time.ifBlank { null },
                            isCustom = true
                        )
                        repository.insertHabit(newHabit)
                        if (time.isNotBlank()) {
                            scheduleHabitAlarm(context, newHabit)
                        }
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun HabitCardItem(
    habit: HabitEntity,
    onCardClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onComplete: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val progress = habit.currentProgress.toFloat() / habit.targetProgress

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = getHabitImage(habit.imageUrl) as Int),
                contentDescription = null,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = habit.nama,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (habit.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            null,
                            tint = if (habit.isFavorite) Color(0xFFFFD600) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = habit.deskripsi,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "Card Progress Anim"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${habit.currentProgress}/${habit.targetProgress}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
                                .clickable { onDecrement() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 16.sp)
                        }
                        Text(
                            text = habit.currentProgress.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { onIncrement() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (habit.isCompleted) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Selesai ✓",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Selesai", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    habitId: Int,
    repository: HabitRepository,
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var habit by remember { mutableStateOf<HabitEntity?>(null) }
    val userProfile by repository.getUserProfileFlow().collectAsState(initial = null)

    LaunchedEffect(habitId) {
        habit = repository.getHabitById(habitId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Kebiasaan") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    habit?.let { h ->
                        IconButton(
                            onClick = {
                                scope.launch {
                                    repository.updateHabit(h.copy(isFavorite = !h.isFavorite))
                                    habit = h.copy(isFavorite = !h.isFavorite)
                                }
                            }
                        ) {
                            Icon(
                                if (h.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                null,
                                tint = if (h.isFavorite) Color(0xFFFFD600) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (h.isCustom) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        cancelHabitAlarm(context, h.id)
                                        repository.deleteHabit(h)
                                        Toast.makeText(context, "Habit dihapus", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Delete, "Hapus", tint = Color.Red)
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        val h = habit
        if (h == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            var isEditing by remember { mutableStateOf(false) }

            var nameInput by remember { mutableStateOf(h.nama) }
            var descInput by remember { mutableStateOf(h.deskripsi) }
            var detailInput by remember { mutableStateOf(h.deskripsiPanjang) }
            var targetInput by remember { mutableStateOf(h.targetProgress.toString()) }
            var timeInput by remember { mutableStateOf(h.reminderTime ?: "") }
            var selectedImgKey by remember { mutableStateOf(h.imageUrl) }

            val illustrations = listOf(
                Pair("💧 Air", "local_air"),
                Pair("📚 Buku", "local_buku"),
                Pair("🏃 Olahraga", "local_olahraga"),
                Pair("💻 Belajar", "local_belajar"),
                Pair("🌙 Tidur", "local_tidur")
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Image(
                    painter = painterResource(id = getHabitImage(if (isEditing) selectedImgKey else h.imageUrl) as Int),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nama Habit") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = descInput,
                            onValueChange = { descInput = it },
                            label = { Text("Deskripsi Singkat") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = detailInput,
                            onValueChange = { detailInput = it },
                            label = { Text("Deskripsi Panjang") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = targetInput,
                            onValueChange = { targetInput = it },
                            label = { Text("Target Harian") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = timeInput,
                            onValueChange = { timeInput = it },
                            label = { Text("Jam Reminder (format HH:mm, misal 07:00)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Pilih Ilustrasi/Ikon:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(illustrations) { item ->
                                FilterChip(
                                    selected = selectedImgKey == item.second,
                                    onClick = { selectedImgKey = item.second },
                                    label = { Text(item.first) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val targetVal = targetInput.toIntOrNull() ?: h.targetProgress
                                    scope.launch {
                                        val updated = h.copy(
                                            nama = nameInput,
                                            deskripsi = descInput,
                                            deskripsiPanjang = detailInput,
                                            targetProgress = targetVal,
                                            imageUrl = selectedImgKey,
                                            reminderTime = timeInput.ifBlank { null }
                                        )
                                        repository.updateHabit(updated)
                                        habit = updated
                                        if (timeInput.isNotBlank()) {
                                            scheduleHabitAlarm(context, updated)
                                        } else {
                                            cancelHabitAlarm(context, updated.id)
                                        }
                                        isEditing = false
                                        Toast.makeText(context, "Perubahan disimpan", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Simpan")
                            }
                            OutlinedButton(
                                onClick = { isEditing = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Batal")
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    h.kategori,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(h.nama, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(h.deskripsiPanjang, style = MaterialTheme.typography.bodyLarge, color = Color.Gray, lineHeight = 24.sp)

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Progress Hari Ini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                                .border(1.dp, Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Progress: ${h.currentProgress} / ${h.targetProgress}", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { h.currentProgress.toFloat() / h.targetProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
                                        .clickable {
                                            scope.launch {
                                                val curr = h.currentProgress
                                                if (curr > 0) {
                                                    val next = curr - 1
                                                    val wasCompleted = h.isCompleted
                                                    val updated = h.copy(currentProgress = next, isCompleted = false)
                                                    repository.updateHabit(updated)
                                                    habit = updated
                                                    if (wasCompleted) {
                                                        val p = userProfile ?: return@launch
                                                        val nextXp = maxOf(0, p.xp - 10)
                                                        val levelVal = (nextXp / 100) + 1
                                                        repository.insertOrUpdateProfile(p.copy(xp = nextXp, level = levelVal))
                                                        Toast.makeText(context, "Kebiasaan dikurangi. XP berkurang 10", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .clickable {
                                            scope.launch {
                                                if (h.currentProgress < h.targetProgress) {
                                                    val next = h.currentProgress + 1
                                                    val completed = next == h.targetProgress
                                                    val updated = h.copy(currentProgress = next, isCompleted = completed)
                                                    repository.updateHabit(updated)
                                                    habit = updated

                                                    if (completed) {
                                                         val p = userProfile ?: return@launch
                                                         val nextXp = p.xp + 10
                                                         val levelVal = (nextXp / 100) + 1
                                                         val todayStr = repository.getTodayDateString()
                                                         var newStreak = p.streak
                                                         if (p.lastActiveDate != todayStr) {
                                                             newStreak++
                                                         }
                                                         val bestStreak = maxOf(newStreak, p.bestStreak)
                                                         repository.insertOrUpdateProfile(p.copy(
                                                             xp = nextXp,
                                                             level = levelVal,
                                                             streak = newStreak,
                                                             bestStreak = bestStreak,
                                                             lastActiveDate = todayStr
                                                         ))
                                                         repository.insertHistory(
                                                             HabitHistoryEntity(
                                                                 userId = p.id,
                                                                 habitName = h.nama,
                                                                 date = todayStr,
                                                                 completed = true
                                                             )
                                                         )
                                                         Toast.makeText(context, "Selesai! +10 XP 🔥", Toast.LENGTH_SHORT).show()
                                                     }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Notifikasi Reminder", fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = h.reminderTime ?: "Tidak aktif",
                                        fontWeight = FontWeight.Bold,
                                        color = if (h.reminderTime != null) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                                if (h.reminderTime != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Frekuensi: ${h.reminderDays}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsScreen(habits: List<HabitEntity>, repository: HabitRepository, userProfile: UserProfileEntity?) {
    val profileId = userProfile?.id ?: 0
    val habitHistory by repository.getAllHistoryFlow(profileId).collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Statistik Dashboard 📊",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Pencapaian produktivitas dan kebiasaan harian Anda.", color = Color.Gray)
        }

        // Summary Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val completed = habits.count { it.isCompleted }
                val remaining = habits.size - completed

                StatCard("Total Habit", habits.size.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatCard("Selesai", completed.toString(), Color(0xFF2E7D32), Modifier.weight(1f))
                StatCard("Belum", remaining.toString(), Color(0xFFC62828), Modifier.weight(1f))
            }
        }

        // Weekly Progress Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Grafik Mingguan 📈", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyChart(habitHistory)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Kalender Riwayat Habit 📅", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    RealCalendarSheet(habitHistory, habits)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pencapaian / Achievement 🏆", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    AchievementsList(repository)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Riwayat Aktivitas ⏳", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    ActivityHistoryList(habitHistory)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun RealCalendarSheet(history: List<HabitHistoryEntity>, habits: List<HabitEntity>) {
    var currentMonthYear by remember {
        val cal = Calendar.getInstance()
        mutableStateOf(Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)))
    }
    val year = currentMonthYear.first
    val month = currentMonthYear.second

    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Bulan"

    val javaDay = calendar.get(Calendar.DAY_OF_WEEK)
    val startOffset = when (javaDay) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    val monthPrefix = String.format(Locale.getDefault(), "%d-%02d", year, month + 1)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = sdf.format(Date())
    val totalHabits = if (habits.isNotEmpty()) habits.size else 5

    val prevMonth = {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            add(Calendar.MONTH, -1)
        }
        currentMonthYear = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    val nextMonth = {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            add(Calendar.MONTH, 1)
        }
        currentMonthYear = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    val themePrimary = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = prevMonth) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sebelumnya", tint = themePrimary)
            }
            Text(
                text = "$monthName $year",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = themePrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = nextMonth) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Berikutnya", tint = themePrimary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            val weekdays = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
            for (day in weekdays) {
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val totalCells = daysInMonth + startOffset
        val rowsCount = (totalCells + 6) / 7

        for (r in 0 until rowsCount) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    val dateNum = cellIndex - startOffset + 1
                    
                    if (dateNum in 1..daysInMonth) {
                        val dateStr = String.format(Locale.getDefault(), "%s-%02d", monthPrefix, dateNum)
                        val completedCount = history.filter { it.date == dateStr && it.completed }.map { it.habitName }.distinct().size
                        val isFuture = dateStr > todayStr

                        val isDayCompleted = if (dateStr == todayStr) {
                            // For today: use actual habit completion state, NOT history records
                            habits.isNotEmpty() && habits.all { it.isCompleted }
                        } else {
                            // For past dates: check history records vs total habits
                            completedCount >= habits.size && habits.isNotEmpty()
                        }

                        val cellBgColor = when {
                            isFuture -> Color.LightGray.copy(alpha = 0.2f)
                            isDayCompleted -> Color(0xFF2E7D32).copy(alpha = 0.25f)
                            completedCount > 0 -> Color(0xFFFBC02D).copy(alpha = 0.25f)
                            else -> Color(0xFFD32F2F).copy(alpha = 0.25f)
                        }

                        val cellBorderColor = when {
                            isFuture -> Color.Transparent
                            isDayCompleted -> Color(0xFF2E7D32)
                            completedCount > 0 -> Color(0xFFFBC02D)
                            else -> Color(0xFFD32F2F)
                        }

                        val cellTextColor = when {
                            isFuture -> MaterialTheme.colorScheme.onSurface
                            isDayCompleted -> Color(0xFF2E7D32)
                            completedCount > 0 -> Color(0xFFF57F17)
                            else -> Color(0xFFD32F2F)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    color = cellBgColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = if (isFuture) 0.dp else 1.dp,
                                    color = cellBorderColor,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dateNum.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = cellTextColor
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Keterangan: ", fontSize = 10.sp, color = Color.Gray)
            Box(Modifier.size(10.dp).background(Color(0xFF2E7D32).copy(alpha = 0.25f), RoundedCornerShape(2.dp)).border(0.5.dp, Color(0xFF2E7D32), RoundedCornerShape(2.dp)))
            Text("Selesai", fontSize = 10.sp, color = Color.Gray)
            Box(Modifier.size(10.dp).background(Color(0xFFFBC02D).copy(alpha = 0.25f), RoundedCornerShape(2.dp)).border(0.5.dp, Color(0xFFFBC02D), RoundedCornerShape(2.dp)))
            Text("Belum", fontSize = 10.sp, color = Color.Gray)
            Box(Modifier.size(10.dp).background(Color(0xFFD32F2F).copy(alpha = 0.25f), RoundedCornerShape(2.dp)).border(0.5.dp, Color(0xFFD32F2F), RoundedCornerShape(2.dp)))
            Text("Tidak Selesai", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun WeeklyChart(history: List<HabitHistoryEntity>) {
    val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

    val data = remember(history) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        val counts = MutableList(7) { 0f }
        val historyMap = history.groupBy { it.date }.mapValues { it.value.size.toFloat() }

        repeat(7) { dayIndex ->
            val dateStr = sdf.format(cal.time)
            counts[dayIndex] = (historyMap[dateStr] ?: 0f) * 20f
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        counts
    }

    val themePrimary = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val width = size.width
        val height = size.height
        val barWidth = 32.dp.toPx()
        val spacing = (width - (barWidth * days.size)) / (days.size + 1)

        for (i in 1..4) {
            val y = height * (i * 0.25f)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        for (i in days.indices) {
            val x = spacing + i * (barWidth + spacing)
            val valPercent = minOf(100f, data[i])
            val barHeight = (valPercent / 100f) * height
            val y = height - barHeight

            if (barHeight > 0) {
                drawRoundRect(
                    color = themePrimary,
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        for (day in days) {
            Text(day, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        }
    }
}

@Composable
fun ProfileScreen(
    repository: HabitRepository,
    userProfile: UserProfileEntity?,
    mainNavController: NavHostController
) {
    val scope = rememberCoroutineScope()
    var showDialogCategories by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    val p = userProfile ?: UserProfileEntity()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                "Personalisasi & Profil 👤",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Text(
                "Sesuaikan tema, kelola kategori kustom, dan keluar masuk akun Anda.",
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(76.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        p.username,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(p.email, color = Color.Gray, fontSize = 13.sp)
                    Text(
                        text = "Gelar: ${getUserTitle(p.level, p.streak, p.xp)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showLogoutConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Keluar Akun", color = Color.White)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pengaturan UI / UX 🎨", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (p.isDarkMode) "🌙 Mode Gelap" else "☀️ Mode Terang",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = p.isDarkMode,
                            onCheckedChange = { isChecked ->
                                scope.launch {
                                    repository.insertOrUpdateProfile(p.copy(isDarkMode = isChecked))
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Pilihan Tema Warna", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val themes = listOf("Hijau", "Biru", "Ungu")
                    val themeColors = listOf(Color(0xFF2E7D32), Color(0xFF1565C0), Color(0xFF6A1B9A))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (i in themes.indices) {
                            val thName = themes[i]
                            val thCol = themeColors[i]
                            val isSelected = p.themeColor == thName

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(thCol, shape = CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        scope.launch {
                                            repository.insertOrUpdateProfile(p.copy(themeColor = thName))
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, null, tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDialogCategories = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏷️ Kelola Kategori Kustom", fontWeight = FontWeight.SemiBold)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Gray)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("Konfirmasi Keluar") },
            text = { Text("Apakah Anda yakin ingin keluar dari aplikasi DailyCheck?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.logoutActiveUser()
                            showLogoutConfirmation = false
                            mainNavController.navigate("login") {
                                popUpTo("main") { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Keluar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Batal")
                }
            }
        )
    }
    if (showDialogCategories) {
        var newCatInput by remember { mutableStateOf("") }
        val customCats by repository.getAllCategoriesFlow().collectAsState(initial = emptyList())

        Dialog(onDismissRequest = { showDialogCategories = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Kelola Kategori Kustom", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCatInput,
                            onValueChange = { newCatInput = it },
                            placeholder = { Text("Kategori Baru...") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newCatInput.isNotBlank()) {
                                    scope.launch {
                                        repository.insertCategory(CustomCategoryEntity(newCatInput))
                                        newCatInput = ""
                                    }
                                }
                            }
                        ) {
                            Text("Tambah")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Daftar Kategori Anda:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.height(150.dp).verticalScroll(rememberScrollState())) {
                        Column {
                            for (cat in customCats) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cat.name, fontWeight = FontWeight.SemiBold)
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                repository.deleteCategory(cat)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Delete, null, tint = Color.Red)
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showDialogCategories = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Tutup")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditHabitDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, detail: String, cat: String, target: Int, time: String, imgKey: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("1") }
    var reminderTime by remember { mutableStateOf("") } // HH:mm

    var selectedCat by remember { mutableStateOf(if (categories.isNotEmpty()) categories[0] else "Pagi") }
    var showDropdown by remember { mutableStateOf(false) }
    var selectedImgKey by remember { mutableStateOf("local_air") }

    val illustrations = listOf(
        Pair("💧 Air", "local_air"),
        Pair("📚 Buku", "local_buku"),
        Pair("🏃 Olahraga", "local_olahraga"),
        Pair("💻 Belajar", "local_belajar"),
        Pair("🌙 Tidur", "local_tidur")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Buat Kebiasaan Baru 🌿",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Habit (misal: Meditasi)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Deskripsi Singkat") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("Deskripsi Lengkap") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = {
                            Icon(Icons.Filled.ArrowDropDown, null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDropdown = true }
                    )
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        for (cat in categories) {
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCat = cat
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("Target Harian") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reminderTime,
                    onValueChange = { reminderTime = it },
                    label = { Text("Notifikasi Jam (HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text("Pilih Ilustrasi/Ikon:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(illustrations) { item ->
                        FilterChip(
                            selected = selectedImgKey == item.second,
                            onClick = { selectedImgKey = item.second },
                            label = { Text(item.first) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val target = targetStr.toIntOrNull() ?: 1
                            if (name.isNotBlank()) {
                                onSave(name, desc, detail, selectedCat, target, reminderTime, selectedImgKey)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tambah")
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                }
            }
        }
    }
}

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var color: Color,
    var size: Float,
    var speedY: Float,
    var speedX: Float,
    var rotation: Float,
    var rotationSpeed: Float
)

@Composable
fun ConfettiCelebration(modifier: Modifier = Modifier) {
    val colors = listOf(
        Color(0xFFFFEB3B), Color(0xFFFF5722), Color(0xFFE91E63),
        Color(0xFF4CAF50), Color(0xFF00BCD4), Color(0xFF9C27B0)
    )

    val particles = remember {
        List(80) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = -Random.nextFloat() * 500f - 50f,
                color = colors[Random.nextInt(colors.size)],
                size = Random.nextFloat() * 15f + 10f,
                speedY = Random.nextFloat() * 8f + 5f,
                speedX = Random.nextFloat() * 4f - 2f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 10f - 5f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Confetti Ticker")
    val ticker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Confetti Frame Anim"
    )

    Canvas(modifier = modifier) {
        @Suppress("UNUSED_EXPRESSION")
        ticker
        val width = size.width
        val height = size.height

        for (p in particles) {
            p.y += p.speedY
            p.x += p.speedX / width

            if (p.x !in 0f..1f) {
                p.speedX = -p.speedX
            }

            p.rotation += p.rotationSpeed

            if (p.y > height) {
                p.y = -Random.nextFloat() * 200f - 20f
                p.x = Random.nextFloat()
            }

            val absX = p.x * width
            rotate(degrees = p.rotation, pivot = androidx.compose.ui.geometry.Offset(absX, p.y)) {
                drawRect(
                    color = p.color,
                    topLeft = androidx.compose.ui.geometry.Offset(absX - p.size / 2, p.y - p.size / 2),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size * 1.5f)
                )
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun AchievementsList(repository: HabitRepository) {
    val userProfile by repository.getUserProfileFlow().collectAsState(initial = null)
    val streak = userProfile?.streak ?: 0

    val badges = listOf(
        BadgeInfo("🏆 Pemula", "Selesaikan kebiasaan pertama", streak >= 1),
        BadgeInfo("🏆 Rajin", "Capai streak 3 hari", streak >= 3),
        BadgeInfo("🏆 Konsisten", "Capai streak 7 hari", streak >= 7),
        BadgeInfo("🏆 Suhu", "Capai streak 14 hari", streak >= 14),
        BadgeInfo("🏆 Master", "Capai streak 30 hari", streak >= 30)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in badges.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val badge1 = badges[i]
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    BadgeItem(badge = badge1)
                }
                if (i + 1 < badges.size) {
                    val badge2 = badges[i + 1]
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        BadgeItem(badge = badge2)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun BadgeItem(badge: BadgeInfo) {
    val color = if (badge.isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else Color.LightGray.copy(alpha = 0.15f)
    val border = if (badge.isUnlocked) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color, RoundedCornerShape(12.dp))
            .border(border, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            badge.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (badge.isUnlocked) MaterialTheme.colorScheme.primary else Color.Gray
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                badge.desc,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                color = if (badge.isUnlocked) Color.DarkGray else Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ActivityHistoryList(history: List<HabitHistoryEntity>) {
    var showAllDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (history.isEmpty()) {
            ActivityRow("07 Juni", "✓ Minum Air")
            ActivityRow("06 Juni", "✓ Belajar Kotlin")
            ActivityRow("05 Juni", "✓ Olahraga Pagi")
        } else {
            history.take(5).forEach { item ->
                ActivityRow(item.date, "✓ ${item.habitName}")
            }
            if (history.size > 5) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = { showAllDialog = true }
                ) {
                    Text("Lihat Semua", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showAllDialog) {
        AlertDialog(
            onDismissRequest = { showAllDialog = false },
            title = {
                Text(
                    text = "Riwayat Aktivitas Lengkap ⏳",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(history) { item ->
                            ActivityRow(item.date, "✓ ${item.habitName}")
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllDialog = false }) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ActivityRow(date: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(date, color = Color.Gray, fontSize = 11.sp)
    }
}

data class BadgeInfo(val title: String, val desc: String, val isUnlocked: Boolean)