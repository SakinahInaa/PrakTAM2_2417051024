package com.example.praktam2_2417051024

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.praktam2_2417051024.model.Habit
import com.example.praktam2_2417051024.model.HabitSource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyCheckApp()
        }
    }
}

@Composable
fun DailyCheckApp() {

    val habits: List<Habit> = HabitSource.dummyHabit
    var selectedCategory by remember { mutableStateOf("Semua") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: CRUD create */ },
                containerColor = Color(0xFF2E7D32)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Habit")
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(padding)
                .padding(16.dp)
        ) {

            item {
                HeaderSection()
                Spacer(modifier = Modifier.height(16.dp))
                CategoryRow(selectedCategory) {
                    selectedCategory = it
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(habits) { habit ->
                HabitCard(habit)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Column {
        Text(
            text = "DailyCheck 🌿",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Bangun kebiasaan baik setiap hari",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
fun CategoryRow(
    selected: String,
    onSelected: (String) -> Unit
) {

    val categories = listOf("Semua", "Pagi", "Siang", "Malam")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->

            val isSelected = category == selected

            Button(
                onClick = { onSelected(category) },
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (isSelected) Color(0xFF2E7D32)
                        else Color.LightGray,
                    contentColor =
                        if (isSelected) Color.White
                        else Color.Black
                )
            ) {
                Text(category)
            }
        }
    }
}

@Composable
fun HabitCard(habit: Habit) {

    var isDone by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {

        Box {

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Image(
                    painter = painterResource(id = habit.imageRes),
                    contentDescription = habit.nama,
                    modifier = Modifier.size(90.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = habit.nama,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.clickable { expanded = !expanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "Deskripsi",
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(text = if (expanded) "▲" else "▼")
                    }

                    if (expanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = habit.deskripsi,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Button(
                            onClick = { count++ },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+1 Progress")
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text("Progress: $count")
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
                    imageVector =
                        if (isDone) Icons.Filled.CheckCircle
                        else Icons.Outlined.CheckCircle,
                    contentDescription = "Done",
                    tint =
                        if (isDone) Color(0xFF2E7D32)
                        else Color.Gray
                )
            }
        }
    }
}