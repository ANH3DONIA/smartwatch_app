/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.phone.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.focuspause.data.ReviewItem

/** Se muestra al terminar una sesión de enfoque: lo que se anote se repasará a 1, 3 y 7 días. */
@Composable
fun LearnedDialog(
    subject: String?,
    onSave: (String) -> Unit,
    onSkip: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("¡Sesión completada!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (subject != null) "¿Qué aprendiste de $subject? Te lo recordaremos para repasarlo."
                    else "¿Qué aprendiste? Te lo recordaremos para repasarlo."
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(ReviewItem.MAX_NOTE_LENGTH) },
                    label = { Text("Una línea basta") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
                Text("Guardar para repasar")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("Omitir") }
        }
    )
}
