/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.phone.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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

/** Diálogo para elegir en qué estás trabajando: etiquetas recientes, una nueva o ninguna. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelDialog(
    currentLabel: String?,
    recentLabels: List<String>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentLabel.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿En qué trabajas?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(MAX_LABEL_LENGTH) },
                    singleLine = true,
                    label = { Text("Materia o tarea") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (recentLabels.isNotEmpty()) {
                    Text("Recientes")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        recentLabels.forEach { label ->
                            AssistChip(onClick = { onSelect(label) }, label = { Text(label) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(text) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = { onSelect(null) }) { Text("Sin etiqueta") }
        }
    )
}

private const val MAX_LABEL_LENGTH = 30
