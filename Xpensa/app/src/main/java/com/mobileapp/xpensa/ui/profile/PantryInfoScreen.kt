package com.mobileapp.xpensa.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobileapp.xpensa.ui.PantryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryInfoScreen(
    viewModel: PantryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informazioni Dispensa") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Kitchen,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow(label = "ID Dispensa", value = uiState.pantryId?.toString() ?: "N/D")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "ID Creatore", value = uiState.pantryCreatorId?.toString() ?: "N/D")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "Soglia Kcal", value = "${uiState.kcalThreshold ?: "N/D"} kcal")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Nota: Queste informazioni sono caricate direttamente dal server.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = MaterialTheme.colorScheme.secondary)
    }
}
