package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.IrisViewModel
import kotlin.math.sqrt

@Composable
fun MemoryGraphScreen(
    viewModel: IrisViewModel,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.memories.collectAsState()
    val knowledgeNodes by viewModel.knowledgeNodes.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IrisBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "MEMORY & KNOWLEDGE GRAPH",
                    color = IrisCyanPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Local-First Vector Records & JSON Schema",
                    color = IrisTextMuted,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(IrisCyanPrimary)
                    .testTag("add_memory_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Memory",
                    tint = IrisBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search bar with Cosine Similarity preview
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Vector cosine search memory index...", color = IrisTextMuted, fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_memory_input"),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = IrisCyanPrimary)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IrisCyanPrimary,
                unfocusedBorderColor = IrisBorder,
                focusedTextColor = IrisTextPrimary,
                unfocusedTextColor = IrisTextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Knowledge Graph Nodes Section
        Text(
            text = "ACTIVE KNOWLEDGE GRAPH NODES (${knowledgeNodes.size})",
            color = IrisTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(0.45f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(knowledgeNodes, key = { it.id }) { node ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, IrisBorder, RoundedCornerShape(10.dp)),
                    color = IrisSurfaceElevated
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IrisVioletSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (node.nodeType) {
                                    "USER" -> Icons.Default.Person
                                    "SYSTEM" -> Icons.Default.Hub
                                    else -> Icons.Default.Terminal
                                },
                                contentDescription = node.nodeType,
                                tint = IrisVioletSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = node.label,
                                    color = IrisTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Badge(
                                    containerColor = IrisSurfaceVariant,
                                    contentColor = IrisCyanPrimary
                                ) {
                                    Text(node.nodeType, fontSize = 9.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = node.metadataJson,
                                color = IrisTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Vector Memory Records
        Text(
            text = "ENCRYPTED VECTOR MEMORIES (${memories.size})",
            color = IrisTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        val filteredMemories = remember(memories, searchQuery) {
            if (searchQuery.isBlank()) memories
            else memories.filter {
                it.key.contains(searchQuery, ignoreCase = true) ||
                        it.value.contains(searchQuery, ignoreCase = true)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(0.55f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredMemories, key = { it.key }) { mem ->
                // Simulate cosine similarity
                val similarityScore = calculateSimulatedCosineScore(mem.key, searchQuery)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, IrisBorder, RoundedCornerShape(10.dp)),
                    color = IrisSurface
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = mem.key,
                                    color = IrisCyanPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Badge(
                                    containerColor = IrisEmeraldAccent.copy(alpha = 0.2f),
                                    contentColor = IrisEmeraldAccent
                                ) {
                                    Text("cos: ${String.format("%.2f", similarityScore)}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mem.value,
                                color = IrisTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteMemory(mem.key) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Memory",
                                tint = IrisAlertRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Memory Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Store Knowledge Memory", color = IrisCyanPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Memory Key (e.g. executive_briefing_time)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IrisCyanPrimary,
                            unfocusedBorderColor = IrisBorder,
                            focusedTextColor = IrisTextPrimary,
                            unfocusedTextColor = IrisTextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Memory Value") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IrisCyanPrimary,
                            unfocusedBorderColor = IrisBorder,
                            focusedTextColor = IrisTextPrimary,
                            unfocusedTextColor = IrisTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank() && newValue.isNotBlank()) {
                            viewModel.saveMemory(newKey.trim(), newValue.trim(), "USER")
                            newKey = ""
                            newValue = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IrisCyanPrimary, contentColor = IrisBackground)
                ) {
                    Text("Store Encrypted", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = IrisTextSecondary)
                }
            },
            containerColor = IrisSurfaceElevated
        )
    }
}

fun calculateSimulatedCosineScore(key: String, query: String): Float {
    if (query.isBlank()) return 0.95f
    val common = key.lowercase().toSet().intersect(query.lowercase().toSet()).size
    val total = sqrt((key.length * query.length).toDouble()).toFloat()
    return if (total > 0f) (common / total).coerceIn(0.2f, 0.99f) else 0.5f
}
