package com.example.praktam2_2417051024

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.praktam2_2417051024.model.HabitSource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DailyCheckApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyCheckApp() {
    val habits = HabitSource.dummyHabit

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("DailyCheck Habit Tracker") })
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            items(habits) { habit ->
                HabitItem(habit)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HabitItem(habit: com.example.praktam2_2417051024.model.Habit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            Image(
                painter = painterResource(id = habit.imageRes),
                contentDescription = habit.nama,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = habit.nama, style = MaterialTheme.typography.titleMedium)
                Text(text = habit.deskripsi)
            }
        }
    }
}