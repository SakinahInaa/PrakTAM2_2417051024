package com.example.praktam2_2417051024

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import com.example.praktam2_2417051024.model.Habit
import com.example.praktam2_2417051024.network.RetrofitClient
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

    var habits by remember { mutableStateOf<List<Habit>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("Semua") }

    LaunchedEffect(Unit) {
        try {
            habits = RetrofitClient.instance.getHabits()
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            isError = true
        }
    }

    val filtered = if (selectedCategory == "Semua") habits
    else habits.filter { it.kategori == selectedCategory }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {}) {
                Icon(Icons.Filled.Add, null)
            }
        }
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
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
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {

            AsyncImage(
                model = habit.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(habit.nama, fontWeight = FontWeight.Bold)
                Text(habit.deskripsi, color = Color.Gray)

                Button(onClick = onClick) {
                    Text("Detail")
                }
            }

            IconButton(onClick = { done = !done }) {
                Icon(
                    if (done) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = null
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

            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }

            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Text(name, fontWeight = FontWeight.Bold)
            Text(longDesc)

            Row {
                Button(onClick = { count++ }) { Text("+1") }
                Button(onClick = { if (count > 0) count-- }) { Text("-1") }
            }

            Text("Total: $count")

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        delay(800)
                        snackbar.showSnackbar("Berhasil menyelesaikan $name")
                        isLoading = false
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Selesai")
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}