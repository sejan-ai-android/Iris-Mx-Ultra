# IRIS-MX Ultra — Neural Execution System

<div align="center">

```
  ___ ____  ___ ____        __  ____  __   _   _ _   _                  
 |_ _|  _ \|_ _/ ___|      |  \/  \ \/ /  | | | | | | |_ __ __ _       
  | || |_) || |\___ \ _____| |\/| |\  /   | | | | | | | '__/ _` |      
  | ||  _ < | | ___) |_____| |  | |/  \   | |_| | |_| | | | (_| |  _   
 |___|_| \_\___|____/      |_|  |_/_/\_\   \___/ \___/|_|  \__,_| (_)  
                                                                       
```

**"Your Neural Execution System"**  
*Voice-first • Sub-second latency • Local-first privacy • Multi-agent swarm orchestration*

[![Platform](https://img.shields.io/badge/Platform-Android%20Native-00F0FF?style=for-the-badge&logo=android)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin%20%7C%20Compose-A855F7?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Architecture](https://img.shields.io/badge/Architecture-7--Agent%20Swarm-10B981?style=for-the-badge)](https://github.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-F59E0B?style=for-the-badge)](LICENSE)

</div>

---

## ⚡ Overview

**IRIS-MX Ultra** is an executive-tier Android voice-first, multi-agent AI execution engine designed to function as an autonomous Chief of Staff on Android devices. Operating under a sub-second latency budget, it translates natural voice or text directives into directed acyclic graph (DAG) plans, validates security posture via safety interceptors, persists memories locally, and executes actions across native Android bridges and terminal interfaces.

---

## 🧠 7-Agent Autonomous Swarm Architecture

IRIS-MX Ultra coordinates execution across a shared internal event bus using seven specialized, concurrent agents:

```
                      ┌──────────────────────┐
                      │     ORCHESTRATOR     │  (Central Router & Latency Budget)
                      └──────────┬───────────┘
         ┌───────────────────────┼───────────────────────┐
         ▼                       ▼                       ▼
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│   PLANNER AGENT  │    │  EXECUTOR AGENT  │    │   MEMORY AGENT   │
│  (Goal DAG Gen)  │    │ (Native Bridges) │    │  (Vector / Room) │
└──────────────────┘    └──────────────────┘    └──────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│   CRITIC AGENT   │    │ PROACTIVE AGENT  │    │   EMOTION AGENT  │
│(Safety Intercept)│    │(Device Telemetry)│    │(Tone Modulation) │
└──────────────────┘    └──────────────────┘    └──────────────────┘
```

| Agent | Icon | Role & Responsibilities |
|---|---|---|
| **Orchestrator** | ⚡ | Central router. Evaluates intent, delegates sub-tasks, and enforces sub-second latency budgets. |
| **PlannerAgent** | 🌲 | Decomposes goals into step execution DAGs with explicit tool bindings. |
| **ExecutorAgent** | ⚙️ | Dispatches native Android bridges, package managers, audio streams, and AnyClaw CLI calls. |
| **MemoryAgent** | 💾 | Manages short-term reactive states, long-term encrypted SQLite records (Room), and cosine vector search. |
| **CriticAgent** | 🛡️ | **Mandatory Level-4 Safety Interceptor.** Intercepts destructive actions, wipes, and financial calls. Requires explicit Commander authorization. |
| **ProactiveAgent**| 📡 | Monitors battery, audio session, and network telemetry to deliver timely proactive suggestions. |
| **EmotionAgent** | 🎭 | Assesses input sentiment and modulates voice/text output between ultra-concise executive and supportive tones. |

---

## 🛠️ Function Calling Tools Schema

The swarm executes native actions through structured JSON tool bindings:

```json
[
  {
    "name": "control_app",
    "description": "Launch, close, or query app state on Android.",
    "parameters": {
      "type": "OBJECT",
      "properties": {
        "packageName": { "type": "STRING", "description": "e.g. com.whatsapp, com.spotify.music" },
        "action": { "type": "STRING", "enum": ["OPEN", "CLOSE", "GET_STATE"] }
      },
      "required": ["action"]
    }
  },
  {
    "name": "control_media",
    "description": "Control playback and system audio sessions.",
    "parameters": {
      "type": "OBJECT",
      "properties": {
        "command": { "type": "STRING", "enum": ["PLAY", "PAUSE", "SKIP", "PREVIOUS", "SET_VOLUME"] },
        "volumeLevel": { "type": "INTEGER", "description": "Volume percentage (0-100)" }
      },
      "required": ["command"]
    }
  },
  {
    "name": "manage_automation",
    "description": "Create, trigger, or schedule an automated device workflow.",
    "parameters": {
      "type": "OBJECT",
      "properties": {
        "triggerType": { "type": "STRING", "enum": ["TIME", "LOCATION", "BATTERY", "APP_LAUNCH", "VOICE"] },
        "actionDescription": { "type": "STRING", "description": "Detailed description of the action" }
      },
      "required": ["triggerType", "actionDescription"]
    }
  },
  {
    "name": "access_memory",
    "description": "Persist or search user knowledge graph and vector memories.",
    "parameters": {
      "type": "OBJECT",
      "properties": {
        "operation": { "type": "STRING", "enum": ["STORE", "RETRIEVE", "DELETE"] },
        "key": { "type": "STRING" },
        "value": { "type": "STRING" }
      },
      "required": ["operation", "key"]
    }
  },
  {
    "name": "run_anyclaw_command",
    "description": "Execute system shell or bridge tools through AnyClaw interface.",
    "parameters": {
      "type": "OBJECT",
      "properties": {
        "command": { "type": "STRING", "description": "Command string to pass to the AnyClaw CLI" }
      },
      "required": ["command"]
    }
  }
]
```

---

## ✨ Key Screens & Features

### 1. Swarm Status Monitor (`এজেন্ট টিম` - Swarm Matrix)
- **Interactive Radial Mesh Graph:** Real-time visual network of all 7 agents with glowing dynamic synapse conduits.
- **Live Status Badges:** Reflects `IDLE`, `ANALYZING`, `PLANNING`, `EXECUTING`, `INTERCEPTING`, and `SUCCESS`.
- **Diagnostic Pulse Self-Test:** One-tap health sweep dispatching diagnostic signals across the shared event bus.
- **Hardware Telemetry Breakdown:** Real-time CPU allocation %, allocated memory cache (KB), queue depth, and status indicators.

### 2. Warm & Natural Voice Assistant (`সহকারী` - Voice HUD)
- **Holographic Neural Orb:** Multi-layer animated cybernetic iris reacting dynamically to speech recognition (`SpeechRecognizer`), reasoning state, and TTS audio RMS levels.
- **Reactive Audio Waveform:** Real-time animated audio spectrum visualizer.
- **Conversational Chief of Staff:** Warm, friendly, supportive human companion tone in both Bengali and English. Zero robotic jargon, code syntax, or raw DAG dumps in standard responses.
- **Bilingual Voice Engine:** Executive English and Bengali (`বাংলা`) with automatic TextToSpeech voice synthesis.

### 3. Memory & Knowledge Graph (`নোট ও মেমরি` - Knowledge)
- Local-first encrypted Room Database (`iris_mx_ultra.db`).
- Vector cosine similarity search over stored user notes and knowledge nodes.
- Pre-seeded root entities, operational postures, and clearance policies.

### 4. Hardware Telemetry & Automations (`ডিভাইস যত্ন` - Telemetry)
- Live system battery tracking, charging state, and battery-triggered routine dispatch.
- Audio volume session controls and network link profiling.
- Scheduled automations engine with time, battery, and location triggers.

### 5. Safety & Care (`নিরাপত্তা নিশ্চিতকরণ` - CriticAgent)
- **CriticAgent Safety Care Modal:** Intercepts high-risk operations (e.g. data wipes, system config alterations) with interactive, polite authorize/abort confirmation dialogs.
- **Secrets & API Management:** Built with Secrets Gradle Plugin; supports Gemini 3.5 Flash with automatic offline neural fallback.

---

## 🚀 Building & Generating the APK

### Prerequisites
- Android SDK 36 (minSdk 24, targetSdk 36)
- JDK 17 or higher
- Gradle 8.x

### Build Debug APK via Command Line
Run the following Gradle command in the root project directory:

```bash
gradle :app:assembleDebug
```

The generated APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK
```bash
gradle :app:assembleRelease
```

### Running Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### Exporting via Google AI Studio
1. Open the **Settings** or hamburger menu in the AI Studio streaming interface.
2. Select **"Download APK"** or **"Export Project as ZIP"**.
3. You can also push directly to your linked GitHub repository with a single click.

---

## 📂 Project Structure

```
├── app/
│   ├── build.gradle.kts                # App dependencies, secrets, & Room KSP
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml     # Permissions, queries, & adaptive icon
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt     # Main entry point & Scaffold navigation
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/          # AgentModels, MemoryEntities, DAG structs
│   │   │   │   │   ├── local/          # AppDatabase, Room DAOs
│   │   │   │   │   ├── remote/         # GeminiApiClient (Gemini 3.5 Flash)
│   │   │   │   │   └── repository/     # IrisAgentRepository & Swarm Event Bus
│   │   │   │   ├── service/            # NativeDeviceBridge, AnyClawBridge, SpeechManager
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/     # NeuralOrbVisualizer, AudioWaveform, AgentSwarmBar, CriticDialog
│   │   │   │   │   ├── screens/        # SwarmStatusMonitor, Orchestration, CodeGen, MemoryGraph, Telemetry, Settings
│   │   │   │   │   └── theme/          # Cybernetic Dark Obsidian & Cyan Palette
│   │   │   │   └── viewmodel/          # IrisViewModel
│   │   │   └── res/
│   │   │       ├── drawable/           # ic_launcher adaptive foreground/background
│   │   │       ├── mipmap-*/           # High-resolution raster launcher icons
│   │   │       └── values/             # strings.xml, themes.xml, colors.xml
│   │   └── test/java/com/example/      # Robolectric & JUnit test suite
├── gradle/
│   └── libs.versions.toml              # Centralized version catalog
├── metadata.json                       # Platform configuration & name synchronization
└── README.md                           # Documentation
```

---

## 🔒 Security & Privacy Posture

- **Local-First Privacy:** All memories, automations, and knowledge nodes remain strictly on-device in encrypted SQLite Room storage (`iris_mx_ultra.db`).
- **No Cloud Leakage:** Hardware telemetry and sensory data are evaluated locally at sub-second speeds.
- **Fail-Safe Critic:** Destructive actions cannot execute without explicit Commander override.

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
Copyright © 2026 IRIS-MX Ultra Development Team.
