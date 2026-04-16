package com.example.praktam2_2417051024

import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.praktam2_2417051024.model.Habit
import com.example.praktam2_2417051024.model.HabitSource
import com.example.praktam2_2417051024.ui.theme.DailyCheckTheme

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
                navArgument("image") { type = NavType.IntType }
            )
        ) { backStackEntry ->

            val name = backStackEntry.arguments?.getString("name") ?: ""
            val longDesc = Uri.decode(backStackEntry.arguments?.getString("longDesc") ?: "")
            val image = backStackEntry.arguments?.getInt("image") ?: 0

            DetailScreen(name, longDesc, image, navController)
        }
    }
}

@Composable
fun HabitListScreen(navController: NavHostController) {
    val habits = HabitSource.dummyHabit
    var selectedCategory by remember { mutableStateOf("Semua") }

    val filteredHabits = if (selectedCategory == "Semua") {
        habits
    } else {
        habits.filter { it.kategori == selectedCategory }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { },
                containerColor = Color(0xFF2E7D32)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah")
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            item {
                Text(
                    "DailyCheck 🌿",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text("Bangun kebiasaan baik setiap hari", color = Color.Gray)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Semua", "Pagi", "Siang", "Malam").forEach { category ->
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

                Spacer(modifier = Modifier.height(20.dp))
            }

            items(filteredHabits) { habit ->
                HabitCard(habit) {

                    val encodedLongDesc = Uri.encode(habit.deskripsiPanjang)

                    navController.navigate(
                        "detail/${habit.nama}/$encodedLongDesc/${habit.imageRes}"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HabitCard(habit: Habit, onClick: () -> Unit) {

    var isDone by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box {

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Image(
                    painter = painterResource(habit.imageRes),
                    contentDescription = habit.nama,
                    modifier = Modifier.size(85.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        habit.nama,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        habit.deskripsi,
                        color = Color.Gray,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Button(
                            onClick = { onClick() }
                        ) {
                            Text("Lihat Detail")
                        }

                        Text(
                            text = "Progress: $count",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            IconButton(
                onClick = { isDone = !isDone },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isDone)
                        Icons.Filled.CheckCircle
                    else
                        Icons.Outlined.CheckCircle,
                    contentDescription = "Done",
                    tint = if (isDone) Color(0xFF2E7D32) else Color.Gray
                )
            }
        }
    }
}
@Composable
fun DetailScreen(
    name: String,
    longDesc: String,
    image: Int,
    navController: NavHostController
) {

    var count by remember { mutableIntStateOf(0) }

    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    "Detail Kebiasaan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(image),
                contentDescription = name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(longDesc, color = Color.Gray)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {

                Button(onClick = { count++ }) {
                    Text("+1")
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(onClick = { if (count > 0) count-- }) {
                    Text("-1")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text("Total: $count")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        delay(800)
                        snackbarHostState.showSnackbar(
                            "Kebiasaan '$name' berhasil diselesaikan!"
                        )
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Menyimpan...")
                } else {
                    Text("Selesai")
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}