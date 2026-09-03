package com.mobileapp.xpensa.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobileapp.xpensa.data.api.PantryShareRequestResponse
import com.mobileapp.xpensa.ui.PantryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryInfoScreen(
    viewModel: PantryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var kcalThresholdInput by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    val isCurrentUsersOwner = remember(uiState.pantryCreatorId, uiState.currentUsername) {
        !uiState.pantryCreatorId.isNullOrBlank() &&
                uiState.pantryCreatorId.equals(uiState.currentUsername, ignoreCase = true)
    }

    var userToRemoveForConfirmation by remember { mutableStateOf<String?>(null) }
    var addMemberUsername by remember { mutableStateOf("") }
    var requestToApproveForConfirmation by remember { mutableStateOf<PantryShareRequestResponse?>(null) }
    var requestToRejectForConfirmation by remember { mutableStateOf<PantryShareRequestResponse?>(null) }
    var showLeavePantryConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.kcalThreshold) {
        kcalThresholdInput = uiState.kcalThreshold?.toString() ?: ""
    }

    LaunchedEffect(uiState.shareActionSuccessMessage) {
        if (uiState.shareActionSuccessMessage != null && 
            uiState.shareActionSuccessMessage!!.contains("request sent", ignoreCase = true)
        ) {
            addMemberUsername = ""
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pantry Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Kitchen,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Card 1: Pantry Info & Kcal Threshold
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow(label = "Pantry ID", value = uiState.pantryId?.toString() ?: "N/A")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "Creator", value = uiState.pantryCreatorId ?: "N/A")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    if (isEditing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedTextField(
                                value = kcalThresholdInput,
                                onValueChange = { kcalThresholdInput = it },
                                label = { Text("Kcal Threshold") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val newThreshold = kcalThresholdInput.toIntOrNull()
                                    if (newThreshold != null) {
                                        viewModel.updateThreshold(newThreshold)
                                        isEditing = false
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Save, 
                                    contentDescription = "Save",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoRowContent(label = "Kcal Threshold", value = "${uiState.kcalThreshold ?: "N/A"} kcal")
                            TextButton(onClick = { isEditing = true }) {
                                Text("Edit")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card 2: Connected Users
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Pantry Users",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.pantryUsers.isEmpty()) {
                        Text(
                            text = "No other users connected to your pantry",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.pantryUsers.forEach { username ->
                                val isCurrentUser = !uiState.currentUsername.isNullOrBlank() &&
                                        username.equals(uiState.currentUsername, ignoreCase = true)
                                val displayName = if (isCurrentUser) "me" else username
                                val canRemoveThisUser = isCurrentUsersOwner && username != uiState.pantryCreatorId && !isCurrentUser

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    if (canRemoveThisUser) {
                                        IconButton(
                                            onClick = { userToRemoveForConfirmation = username },
                                            enabled = !uiState.isRemovingUser
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove user",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card 3: Add Member (Inline input & button)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Join pantry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = addMemberUsername,
                            onValueChange = { 
                                addMemberUsername = it 
                                if (uiState.shareActionError != null || uiState.shareActionSuccessMessage != null) {
                                    viewModel.clearShareActionMessages()
                                }
                            },
                            label = { Text("Username") },
                            placeholder = { Text("e.g. john_doe") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isSendingShareRequest
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { viewModel.sendShareRequest(addMemberUsername) },
                            enabled = addMemberUsername.isNotBlank() && !uiState.isSendingShareRequest
                        ) {
                            if (uiState.isSendingShareRequest) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send request",
                                    tint = if (addMemberUsername.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            // Feedback Banners (Error / Success)
            uiState.shareActionError?.let { errorMsg ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearShareActionMessages() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            uiState.shareActionSuccessMessage?.let { successMsg ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = successMsg,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearShareActionMessages() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card 4: Received Requests
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mail,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Received Requests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.shareRequests.isNotEmpty()) {
                            Badge {
                                Text(uiState.shareRequests.size.toString())
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.isFetchingShareRequests) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else if (uiState.shareRequests.isEmpty()) {
                        Text(
                            text = "No pending share requests",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.shareRequests.forEach { request ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = request.requesterName ?: request.displayUsername,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (!request.requesterName.isNullOrBlank() && request.requesterName != request.displayUsername) {
                                            Text(
                                                text = "@${request.displayUsername}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { requestToApproveForConfirmation = request },
                                            enabled = !uiState.isProcessingShareRequest
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Accept",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { requestToRejectForConfirmation = request },
                                            enabled = !uiState.isProcessingShareRequest
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Reject",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isCurrentUsersOwner) {
                Spacer(modifier = Modifier.height(24.dp))

                // Leave Pantry Button
                Button(
                    onClick = { showLeavePantryConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    enabled = !uiState.isLeavingPantry
                ) {
                    if (uiState.isLeavingPantry) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Leaving pantry...")
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Leave Pantry")
                    }
                }
            }
        }

        // Confirmation Dialog: Remove User
        userToRemoveForConfirmation?.let { user ->
            AlertDialog(
                onDismissRequest = { userToRemoveForConfirmation = null },
                title = { Text("Remove User") },
                text = { Text("Are you sure you want to remove '$user' from the pantry?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.removeUserFromPantry(user)
                            userToRemoveForConfirmation = null
                        }
                    ) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToRemoveForConfirmation = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Confirmation Dialog: Approve Share Request
        requestToApproveForConfirmation?.let { req ->
            AlertDialog(
                onDismissRequest = { requestToApproveForConfirmation = null },
                title = { Text("Accept Request") },
                text = { Text("Do you want to accept the share request from '${req.requesterName ?: req.displayUsername}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.approveShareRequest(req.id)
                            requestToApproveForConfirmation = null
                        }
                    ) {
                        Text("Accept")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { requestToApproveForConfirmation = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Confirmation Dialog: Reject Share Request
        requestToRejectForConfirmation?.let { req ->
            AlertDialog(
                onDismissRequest = { requestToRejectForConfirmation = null },
                title = { Text("Reject Request") },
                text = { Text("Do you want to reject the share request from '${req.requesterName ?: req.displayUsername}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.rejectShareRequest(req.id)
                            requestToRejectForConfirmation = null
                        }
                    ) {
                        Text("Reject", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { requestToRejectForConfirmation = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Confirmation Dialog: Leave Pantry
        if (showLeavePantryConfirmation) {
            AlertDialog(
                onDismissRequest = { showLeavePantryConfirmation = false },
                title = { Text("Leave Pantry") },
                text = { Text("Are you sure you want to leave this shared pantry? You will no longer have access to its products.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLeavePantryConfirmation = false
                            viewModel.leavePantry(onSuccess = { onNavigateBack() })
                        }
                    ) {
                        Text("Leave", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeavePantryConfirmation = false }) {
                        Text("Cancel")
                    }
                }
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
        InfoRowContent(label = label, value = value)
    }
}

@Composable
private fun InfoRowContent(label: String, value: String) {
    Text(text = label, fontWeight = FontWeight.SemiBold)
    Text(text = value, color = MaterialTheme.colorScheme.secondary)
}
