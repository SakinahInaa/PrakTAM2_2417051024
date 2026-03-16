package com.example.praktam2_2417051024

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.praktam2_2417051024.model.Habit
import com.example.praktam2_2417051024.model.HabitSource

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DaftarHabitScreen()
        }
    }
}

@Composable
fun DaftarHabitScreen() {

    val habits = HabitSource.dummyHabit

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = "DailyCheck 🌿",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Build your daily habits tracker",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        habits.forEach { habit ->

            DetailScreen(habit)

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun DetailScreen(habit: Habit) {

    var isDone by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var count by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {

        Column {

            Box {

                Image(
                    painter = painterResource(id = habit.imageRes),
                    contentDescription = habit.nama,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                IconButton(
                    onClick = { isDone = !isDone },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {

                    Icon(
                        imageVector =
                            if (isDone) Icons.Filled.CheckCircle
                            else Icons.Outlined.CheckCircle,

                        contentDescription = "Done",

                        tint =
                            if (isDone) Color(0xFF2E7D32)
                            else Color.White
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = habit.nama,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Deskripsi",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = if (expanded) "▲" else "▼"
                    )
                }

                if (expanded) {

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = habit.deskripsi,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Button(
                        onClick = { count++ },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+1 Progress")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Count: $count",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}