/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.phone.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuspause.data.ReviewItem
import com.example.focuspause.data.ReviewScheduler
import com.example.focuspause.ui.phone.theme.FocusOrange
import com.example.focuspause.ui.phone.theme.LongBreakBlue
import com.example.focuspause.ui.phone.theme.ShortBreakGreen
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneReviewScreen(
    reviews: List<ReviewItem>,
    onMarkReviewed: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val today = remember { LocalDate.now().toEpochDay() }
    val due = remember(reviews) { ReviewScheduler.dueToday(reviews, today) }
    val upcoming = remember(reviews) { ReviewScheduler.upcoming(reviews, today) }
    val finished = remember(reviews) { reviews.count { it.isDone } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repasos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Repasar a los 1, 3 y 7 días ayuda a que lo aprendido se quede en la memoria.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionTitle("Para repasar hoy (${due.size})", FocusOrange)
            if (due.isEmpty()) {
                Text(
                    text = if (reviews.isEmpty()) {
                        "Aún no hay apuntes. Al terminar una sesión de enfoque, anota qué aprendiste."
                    } else {
                        "¡Nada pendiente hoy!"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            due.forEach { item ->
                ReviewCard(
                    item = item,
                    footer = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onMarkReviewed(item.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ShortBreakGreen,
                                    contentColor = Color.Black
                                )
                            ) { Text("Ya lo repasé") }
                            TextButton(onClick = { onDelete(item.id) }) { Text("Borrar") }
                        }
                    }
                )
            }

            if (upcoming.isNotEmpty()) {
                SectionTitle("Próximos (${upcoming.size})", LongBreakBlue)
                upcoming.forEach { item ->
                    ReviewCard(
                        item = item,
                        footer = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dueLabel(item.dueEpochDay - today),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { onDelete(item.id) }) { Text("Borrar") }
                            }
                        }
                    )
                }
            }

            if (finished > 0) {
                Text(
                    text = "Apuntes ya repasados 3 veces: $finished",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun dueLabel(daysLeft: Long): String = when {
    daysLeft <= 1L -> "Toca mañana"
    else -> "Toca en $daysLeft días"
}

@Composable
private fun SectionTitle(text: String, color: Color) {
    Text(text = text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
}

@Composable
private fun ReviewCard(item: ReviewItem, footer: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = listOfNotNull(item.subject, "Repaso ${item.nextReviewNumber} de ${ReviewScheduler.TOTAL_REVIEWS}")
                    .joinToString(" · "),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.note,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            footer()
        }
    }
}
