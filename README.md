# MindArc

<p align="center">
  <strong>Earn your screen time through meaningful effort.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android" alt="Android"/>
  <img src="https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=flat&logo=kotlin" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=flat&logo=jetpackcompose" alt="Compose"/>
  <img src="https://img.shields.io/badge/minSdk-24%2B-green" alt="minSdk 24"/>
</p>

---

MindArc is a digital wellbeing Android app that helps you build healthier screen habits. **Restrict distracting apps** (social media, games) and **unlock them only after** completing physical or cognitive activities—pushups, squats, reading, Trace-to-Earn, Pong, or Speed-Dial. Screen time becomes a reward you earn.

---

## ✨ Features

### 🔒 App blocking & screen time

- **Restrict by app** — Select which apps to block (e.g. Instagram, WhatsApp, games).
- **Common daily limit** — Optional cap shared by all blocked apps; when exceeded, apps stay blocked until the next day.
- **Per-app limits** — Set daily usage limits per app; usage is tracked via `UsageStatsManager`.
- **Block overlay** — When you open a restricted app without an active unlock, a full-screen block is shown until you complete an activity.
- **Screen time insights** — Total device time and **social media–only** usage with tiers: Excellent (0–1h), Good (1–2h), Average (2–3h), Below average, Bad, Critical.

### 🏃 Activities that unlock time

| Activity | Description |
|----------|-------------|
| **Push-ups** | ML Kit pose detection + 4-phase state machine; real-time rep count + TTS (“One”, “Two”, …). |
| **Squats** | Pose-based squat counting (vertical displacement, EMA smoothing, hold-at-bottom). |
| **Reading** | App-provided articles with quizzes, or user-provided material with a reflection prompt. |
| **Trace-to-Earn** | 30s trace challenge; accuracy from path distance → **5 / 1 / 0** minutes unlock. |
| **Pong** | Mini game; complete to earn unlock time. |
| **Speed-Dial** | Challenge screen; complete to earn unlock time. |

### 📊 Progress & motivation

- **Progress screen** — Activity history, points earned, daily streaks, unlock sessions.
- **Achievements** — Badges and milestones (e.g. streaks, total points).
- **Unlock sessions** — Completing an activity starts a timed session during which restricted apps are accessible.

---

## 🛠️ Tech stack

- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose), Kotlin
- **Architecture:** ViewModel → Repository → Data (Room, DataStore, `UsageStatsManager`)
- **DI:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Data:** [Room](https://developer.android.com/jetpack/androidx/releases/room), [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Camera & ML:** [CameraX](https://developer.android.com/training/camerax), [ML Kit Pose Detection](https://developers.google.com/ml-kit/vision/pose-detection)
- **Blocking:** [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) + full-screen `BlockActivity`
- **Navigation:** [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)

---

## 🚀 Setup & run

1. **Clone**
   ```bash
   git clone https://github.com/RICH132/MINDARC_.git
   cd MindArc
   ```
2. **Open in Android Studio** — File → Open → select the project; let Gradle sync.
3. **Build** — Build → Make Project (minSdk 24, targetSdk 35).
4. **Enable accessibility** — For blocking to work, enable MindArc in **Settings → Accessibility → Installed apps → MindArc**.

---

## 📁 Project structure (high level)

```
app/src/main/java/com/example/mindarc/
├── data/          # models, repository, DB, DataStore, pose processor
├── di/             # Hilt modules
├── domain/         # PoseAnalyzer, ScreenTimeManager
├── service/        # AppBlockingService, NotificationManager, blocking logic
├── ui/             # Compose screens, ViewModels, navigation, theme
└── viewmodel/      # ProgressViewModel, ReadingViewModel
```

---

## 📄 Documentation

A technical feature reference (architecture, code snippets, blocking and screen-time logic, pose detection, Trace-to-Earn, TTS) is in **`docs/MindArc_Features_Technical_Reference.tex`**. Build the PDF with your favourite LaTeX toolchain (e.g. `pdflatex`).

---

## 📜 License

See repository license file.

---

<p align="center">
  <em>MindArc — turn screen time into a reward.</em>
</p>
