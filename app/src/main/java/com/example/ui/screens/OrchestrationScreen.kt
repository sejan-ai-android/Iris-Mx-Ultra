package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.IrisViewModel
import kotlinx.coroutines.launch

@Composable
fun OrchestrationScreen(
    viewModel: IrisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by viewModel.messages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val audioRmsDb by viewModel.audioRmsDb.collectAsState()
    val swarmStates by viewModel.swarmStates.collectAsState()
    val currentDag by viewModel.currentActiveDag.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val handsFreeContinuous by viewModel.handsFreeContinuous.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()

    // Permission launcher for RECORD_AUDIO
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleListening()
        }
    }

    // Scroll to bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IrisBackground)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "IRIS-MX ULTRA",
                        color = IrisCyanPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Badge(
                        containerColor = IrisCyanPrimary.copy(alpha = 0.2f),
                        contentColor = IrisCyanPrimary
                    ) {
                        Text("Companion", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = if (language == AppLanguage.BENGALI) "আপনার সার্বক্ষণিক ব্যক্তিগত সহকারী" else "Your Personal Assistant & Companion",
                    color = IrisTextMuted,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Language badge
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val nextLang = if (language == AppLanguage.ENGLISH) AppLanguage.BENGALI else AppLanguage.ENGLISH
                            viewModel.setLanguage(nextLang)
                        },
                    color = IrisSurfaceVariant
                ) {
                    Text(
                        text = if (language == AppLanguage.ENGLISH) "EN" else "বাংলা",
                        color = IrisCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Hands-Free Continuous Mode Pill
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.toggleHandsFreeContinuous() },
                    color = if (handsFreeContinuous) IrisCyanPrimary.copy(alpha = 0.15f) else IrisSurfaceVariant,
                    border = BorderStroke(1.dp, if (handsFreeContinuous) IrisCyanPrimary else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (handsFreeContinuous) IrisCyanPrimary else IrisTextMuted)
                        )
                        Text(
                            text = if (handsFreeContinuous) (if (language == AppLanguage.BENGALI) "হ্যান্ডস-ফ্রি" else "Hands-Free") else (if (language == AppLanguage.BENGALI) "ম্যানুয়াল" else "Manual"),
                            color = if (handsFreeContinuous) IrisCyanPrimary else IrisTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Ready Badge
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    color = IrisSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(IrisEmeraldAccent)
                        )
                        Text(
                            text = if (language == AppLanguage.BENGALI) "প্রস্তুত" else "Ready",
                            color = IrisEmeraldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Accessibility Service Notice Banner
        AnimatedVisibility(visible = !isAccessibilityActive) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { viewModel.openAccessibilitySettings() },
                color = IrisSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, IrisVioletSecondary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = "Accessibility",
                            tint = IrisVioletSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (language == AppLanguage.BENGALI) "হ্যান্ডস-ফ্রি স্ক্রিন ও ডিভাইস নিয়ন্ত্রণে Accessibility চালু করুন" else "Enable Accessibility for hands-free device control",
                            color = IrisTextPrimary,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = if (language == AppLanguage.BENGALI) "চালু করুন" else "Enable",
                        color = IrisCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Center Hero Neural Orb & Reactive Audio Wave
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NeuralOrbVisualizer(
                size = 110.dp,
                isListening = isListening,
                isProcessing = isProcessing,
                isSpeaking = isSpeaking,
                audioRmsDb = audioRmsDb,
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.toggleListening()
                    } else {
                        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // State caption with Hands-Free readiness
            Text(
                text = when {
                    isListening -> if (language == AppLanguage.BENGALI) "আপনাকে শুনছি... যেকোনো কথা বলুন" else "Listening to you... speak naturally"
                    isProcessing -> if (language == AppLanguage.BENGALI) "আপনার কাজটি দ্রুত প্রস্তুত করছি..." else "Handling immediately..."
                    isSpeaking -> if (language == AppLanguage.BENGALI) "উত্তর দিচ্ছি..." else "Speaking..."
                    handsFreeContinuous -> if (language == AppLanguage.BENGALI) "হ্যান্ডস-ফ্রি মোড সক্রিয়—সরাসরি কথা বলুন" else "Hands-free continuous mode ready—speak anytime"
                    else -> if (language == AppLanguage.BENGALI) "ভয়েস বা টেক্সটে যেকোনো কথা বলতে পারেন" else "Tap orb or mic to speak with Iris"
                },
                color = when {
                    isListening -> IrisAlertRed
                    isProcessing -> IrisVioletSecondary
                    isSpeaking -> IrisEmeraldAccent
                    handsFreeContinuous -> IrisCyanPrimary
                    else -> IrisTextMuted
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            // Audio Waveform Bar
            AudioWaveformBar(
                isActive = isListening,
                isSpeaking = isSpeaking,
                audioRmsDb = audioRmsDb,
                modifier = Modifier.padding(horizontal = 40.dp, vertical = 4.dp)
            )
        }

        // Active Working Card (Friendly & Clean, No DAG Syntax)
        AnimatedVisibility(visible = isProcessing) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = IrisSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(IrisBorder)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = IrisCyanPrimary
                    )
                    Text(
                        text = if (language == AppLanguage.BENGALI) "আপনার কাজটি দ্রুত ও সুন্দরভাবে প্রস্তুত করা হচ্ছে..." else "Taking care of your request...",
                        color = IrisTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Chat Conversation Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageItem(
                    message = msg,
                    onSpeak = {
                        viewModel.speechManager.speak(msg.text)
                    },
                    onCopy = {
                        viewModel.nativeBridge.copyToClipboard("IRIS Output", msg.text)
                    }
                )
            }
        }

        // Quick Suggestion Chips (Warm & Friendly)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = if (language == AppLanguage.BENGALI) listOf(
                "স্ক্রিনে কি আছে পড়ে শোনাও 📱",
                "হোম স্ক্রিনে ফিরে যাও 🏠",
                "নোটিফিকেশন প্যানেল নামাও 🔔",
                "নিচে স্ক্রল করো 📜",
                "একটি সুন্দর গান শোনাও 🎵",
                "ডিভাইসের যত্ন ও ব্যাটারি চেক 🔋",
                "একটি গুরুত্বপূর্ণ নোট মনে রাখো 📝",
                "কেমন আছো আইরিস? 👋"
            ) else listOf(
                "Read what's on my screen 📱",
                "Go to home screen 🏠",
                "Show notifications 🔔",
                "Scroll down 📜",
                "Play relaxing music 🎵",
                "Check device care & battery 🔋",
                "Save a quick note for me 📝",
                "How are you doing today? 👋"
            )
            items(suggestions) { chipText ->
                SuggestionChip(
                    onClick = { viewModel.submitPrompt(chipText) },
                    label = { Text(chipText, fontSize = 11.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = IrisSurfaceVariant,
                        labelColor = IrisCyanPrimary
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = IrisBorder
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Bottom Input Dock
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = IrisSurfaceVariant,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mic Button
                IconButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.toggleListening()
                        } else {
                            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isListening) IrisAlertRed else IrisSurfaceElevated)
                        .testTag("microphone_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) IrisTextPrimary else IrisCyanPrimary
                    )
                }

                // Text Input
                TextField(
                    value = inputText,
                    onValueChange = { viewModel.setInputText(it) },
                    placeholder = {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "আইরিসকে যেকোনো কথা বা কাজের অনুরোধ বলুন..." else "Ask or request anything from Iris...",
                            color = IrisTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("prompt_input_field"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = IrisSurfaceElevated,
                        unfocusedContainerColor = IrisSurfaceElevated,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = IrisTextPrimary,
                        unfocusedTextColor = IrisTextPrimary
                    ),
                    singleLine = true
                )

                // Send Button
                IconButton(
                    onClick = { viewModel.submitPrompt() },
                    enabled = inputText.isNotBlank() && !isProcessing,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isProcessing) IrisCyanPrimary else IrisSurfaceElevated)
                        .testTag("send_prompt_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Transmit",
                        tint = if (inputText.isNotBlank() && !isProcessing) IrisBackground else IrisTextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 3.dp)
            ) {
                val agentType = message.agentType ?: AgentType.ORCHESTRATOR
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(getAgentColor(agentType))
                )
                Text(
                    text = agentType.displayName.uppercase(),
                    color = getAgentColor(agentType),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                if (message.latencyMs != null) {
                    Text(
                        text = "• ${message.latencyMs}ms",
                        color = IrisTextMuted,
                        fontSize = 10.sp
                    )
                }
                if (message.emotionTone != null) {
                    Text(
                        text = "[${message.emotionTone}]",
                        color = IrisVioletSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) IrisCyanPrimary.copy(alpha = 0.5f) else IrisBorder,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                ),
            color = if (isUser) IrisSurfaceElevated else IrisSurface
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = IrisTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak Text",
                                tint = IrisCyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = IrisTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
