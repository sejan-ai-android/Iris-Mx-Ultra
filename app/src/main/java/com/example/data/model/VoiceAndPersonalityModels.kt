package com.example.data.model

enum class GeminiVoicePreset(
    val voiceName: String,
    val pitch: Float,
    val speed: Float,
    val toneDescription: String,
    val genderNote: String
) {
    PUCK("Puck", 1.05f, 1.12f, "Energetic, natural, lively & conversational (Default)", "Dynamic & Expressive"),
    CHARON("Charon", 0.82f, 1.00f, "Deep, resonant, authoritative & executive", "Baritone Executive"),
    KORE("Kore", 1.15f, 1.08f, "Warm, gentle, melodic & supportive", "Warm Soprano"),
    FENRIR("Fenrir", 0.78f, 1.05f, "Bold, crisp, strong & decisive", "Deep Resonant"),
    AOEDE("Aoede", 1.10f, 1.05f, "Articulate, clear, sophisticated & polished", "Clear & Cultured"),
    LEDA("Leda", 1.18f, 1.10f, "Bright, enthusiastic, cheerful & friendly", "Bright Alto"),
    ORUS("Orus", 0.88f, 1.02f, "Calm, steady, reassuring & distinguished", "Steady Neutral"),
    ZEPHYR("Zephyr", 1.00f, 1.15f, "Swift, breezy, modern & ultra-crisp", "Agile & Crisp");

    companion object {
        val DEFAULT = PUCK
    }
}

enum class AddressingMode(
    val label: String,
    val defaultName: String,
    val bengaliGreeting: String,
    val hinglishGreeting: String
) {
    BOSS("Boss", "Boss", "বস", "Boss"),
    SIR("Sir", "Sir", "স্যার", "Sir"),
    FRIEND("Friend", "Friend", "বন্ধু", "Dost / Friend"),
    CUSTOM("Custom Name", "Chief", "", "");

    companion object {
        val DEFAULT = BOSS
    }
}

enum class AudioEngineMode(
    val title: String,
    val modelName: String,
    val description: String,
    val isLiveStreaming: Boolean
) {
    GEMINI_3_1_FLASH_LIVE(
        title = "Gemini 3.1 Flash Live (Ultra-Low Latency)",
        modelName = "gemini-3.1-flash-live-preview",
        description = "WebSocket Bidirectional Streaming for sub-second real-time live interaction",
        isLiveStreaming = true
    ),
    GEMINI_3_5_FLASH(
        title = "Gemini 3.5 Flash",
        modelName = "gemini-3.5-flash",
        description = "Standard REST request-response pipeline",
        isLiveStreaming = false
    );

    companion object {
        val DEFAULT = GEMINI_3_1_FLASH_LIVE
    }
}

enum class PersonalityMode(
    val title: String,
    val description: String
) {
    ASSISTANT_NATURAL_BLEND(
        title = "Assistant Mode",
        description = "Friendly, helpful, natural conversational blend of Hinglish, English, and Bengali"
    ),
    EXECUTIVE_CHIEF_OF_STAFF(
        title = "Executive Chief of Staff",
        description = "Direct, crisp, high-efficiency executive posture"
    ),
    EMPATHETIC_CARE(
        title = "Empathetic Companion",
        description = "Warm, caring, supportive, and patient"
    );

    companion object {
        val DEFAULT = ASSISTANT_NATURAL_BLEND
    }
}
