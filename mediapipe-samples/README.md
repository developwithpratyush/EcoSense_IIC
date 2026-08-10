# ECOSense — Context-Aware On-Device Audio Intelligence

> **Life-saving acoustic accessibility and autonomous emergency dispatch engineered for deaf, hard-of-hearing, and elderly individuals.**

---

## 📌 Overview

Standard native OS sound recognition systems rely on passive, low-priority text banners that fail to wake sleeping users or differentiate ambient noise from active crises. **ECOSense** transforms smartphone audio hardware into a context-aware safety guardian. Operating entirely offline, it detects environmental sound cues, learns custom household acoustics locally, synthesizes concurrent threat signals, and triggers full-screen visual takeovers, aggressive haptics, or automated GPS emergency dispatches.

---

## 🚀 Core Features

* **Active Sensory Overrides:** Overrides system UI with full-screen color-coded flashes (e.g., full Crimson alert for critical threats) and continuous custom haptic vibrations to wake sleeping or hard-of-hearing users instantly.
* **Context Fusion Reasoning:** Uses an 8-second rolling buffer to evaluate multi-signal acoustic events (e.g., combining `Smoke Alarm` + `Screaming` into a unified `Active Fire Emergency` rather than treating them as isolated pings).
* **Personalized On-Device Sound Training:** Employs few-shot vector cosine distance matching to capture custom household sounds (e.g., specific doorbells, baby cries, family voices) in seconds—without sending audio to the cloud.
* **Autonomous Emergency Dispatch:** Launches an interactive, 5-second cancellable countdown during unacknowledged threats before sending a direct SMS containing live GPS coordinates via cellular network.
* **100% Offline Privacy Sovereignty:** Processes live microphone streams in transient, volatile RAM slices (500ms buffers) that are immediately overwritten. Zero audio data is recorded, saved, or uploaded.

---

## 🏗️ System Architecture

```
[ 1. AUDIO CAPTURE LAYER ]
       │ Persistent 16kHz Audio Stream (Android Foreground Service)
       ▼
[ 2. ON-DEVICE ML ENGINE ]
       │ TFLite Neural Classifier (500+ Acoustic Classes)
       ├─────────────────────────┐
       ▼                         ▼
[ 3. MATCHING & FILTERING ]   [ 4. CONTEXT FUSION ENGINE ]
       │ Confidence Gate         │ 8s Rolling Time-Window Buffer
       │ Vector Distance Math    │ Multi-Signal Synthesis
       └─────────────────────────┼─────────────────────────┘
                                 ▼
                    [ 5. ACTION EXECUTION LAYER ]
                                 │
         ┌───────────────────────┼───────────────────────┐
         ▼                       ▼                       ▼
  Dynamic Screen UI       Custom Haptic Motor     Emergency Dispatch
(Color-Coded Takeover)    (Vibrator API Pulses)    (5s Auto-GPS SMS)

```

---

## 🛠️ Tech Stack Matrix

| Architectural Layer           | Technology Selected                           |                      Purpose & Execution                                   |
| --- -------------------       | --- ---------------------------------------   | ----------------------------------------------------                       |
| **Platform Core**             | Kotlin / Native Android SDK                   | Foreground Services, persistent audio capture lifecycle management.        |
| **Edge ML Engine**            | TensorFlow Lite Runtime                       | Low-latency, zero-network classification across 500+ acoustic classes.     |
| **Personalization**           | Local Embedding Vector Cosine Math            | Few-shot centroid matching for user-defined sound signatures.              |
| **Persistence**               | Room Database (SQLite)                        | Encrypted offline storage for timestamped event history and local vectors. |
| **Emergency Pipeline**        | `FusedLocationProviderClient` + `SmsManager`  | Offline GPS lock and direct cellular SMS transmission.                     |

---

## 🔐 Privacy & Security Architecture

1. **Volatile Buffer Memory:** Microphone input is buffered in temporary RAM slices (500ms) and discarded instantly after feature extraction.
2. **Zero Network Egress:** The core detection pipeline operates without internet permissions—eliminating cloud security attack vectors.
3. **Local Vector Storage:** Personalized audio fingerprints are stored as mathematical vector centroids locally inside an encrypted SQLite database.

---

## 💻 Getting Started

### Prerequisites

* Android Studio Jellyfish or newer
* Android SDK Version 26+ (Android 8.0 Oreo or higher)
* Physical Android device (Recommended for microphone and haptic motor testing)



---