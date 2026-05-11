package com.example.praktam2_2417051024

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.praktam2_2417051024.data.model.Habit
import com.example.praktam2_2417051024.data.repository.HabitRepository
import com.example.praktam2_2417051024.ui.theme.DailyCheckTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyCheckTheme {
                DailyCheckApp()
            }
        }
    }
}

@Composable
fun DailyCheckApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "list") {

        composable("list") {
            HabitListScreen(navController)
        }

        composable(
            "detail/{name}/{longDesc}/{image}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("longDesc") { type = NavType.StringType },
                navArgument("image") { type = NavType.StringType }
            )
        ) {
            val name = it.arguments?.getString("name") ?: ""
            val longDesc = Uri.decode(it.arguments?.getString("longDesc") ?: "")
            val image = Uri.decode(it.arguments?.getString("image") ?: "")

            DetailScreen(name, longDesc, image, navController)
        }
    }
}

@Composable
fun HabitListScreen(navController: NavHostController) {
    val repository = remember { HabitRepository() }

    var habits by remember { mutableStateOf<List<Habit>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("Semua") }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            habits = repository.getHabits()
            isLoading = false
            isError = habits.isEmpty()
        } catch (e: Exception) {
            isLoading = false
            isError = true
        }
    }

    val filtered = if (selectedCategory == "Semua") habits
    else habits.filter { it.kategori == selectedCategory }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                containerColor = Color(0xFF2E7D32)
            ) {
                Icon(Icons.Filled.Add, null)
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        } else if (isError) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Gagal memuat data", color = Color.Red)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {

                item {
                    Text("DailyCheck 🌿", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    val categories = listOf("Semua", "Pagi", "Siang", "Malam")

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            Button(
                                onClick = { selectedCategory = category },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedCategory == category)
                                        Color(0xFF2E7D32)
                                    else Color.LightGray
                                )
                            ) {
                                Text(category)
                            }
                        }
                    }
                }

                items(filtered) { habit ->
                    HabitCard(habit) {
                        navController.navigate(
                            "detail/${habit.nama}/${Uri.encode(habit.deskripsiPanjang)}/${Uri.encode(habit.imageUrl)}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HabitCard(habit: Habit, onClick: () -> Unit) {
    var done by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {

            AsyncImage(
                model = habit.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(habit.nama, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(habit.deskripsi, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onClick
                ) {
                    Text("Detail")
                }
            }

            IconButton(onClick = { done = !done }) {
                Icon(
                    if (done) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = if (done) Color(0xFF2E7D32) else Color.Gray
                )
            }
        }
    }
}

@Composable
fun DetailScreen(
    name: String,
    longDesc: String,
    image: String,
    navController: NavHostController
) {
    var count by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    Box(Modifier.fillMaxSize()) {

        Column(Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
                Text("Detail Kebiasaan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(longDesc, color = Color.Gray)

            Spacer(modifier = Modifier.height(20.dp))

            Text("Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { count++ }
                ) { Text("+1") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { if (count > 0) count-- }
                ) { Text("-1") }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Total: $count")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        delay(800)
                        snackbar.showSnackbar("Berhasil menyelesaikan $name")
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Menyimpan...")
                } else {
                    Text("Selesai")
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}