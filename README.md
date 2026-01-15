# MindArc

**Earn your screen time through meaningful effort.**

---

MindArc is a digital wellbeing application for Android designed to help students and anyone struggling with screen addiction to build healthier digital habits. It transforms access to distracting apps into a reward that must be earned through positive physical or cognitive activities.

## ✨ Core Features

- **App Blocking & Control**: Users can select specific apps (e.g., social media, games) to restrict. These apps are blocked until a productive activity is completed.
- **Activity-Based Unlocking**: To unlock apps, users can choose from:
  - **Physical Activity (Push-ups)**: Uses the device camera and ML Kit Pose Detection to count push-ups in real-time.
  - **Cognitive Activity (Reading)**: Offers both app-provided articles with quizzes and a mode for reading user-provided material with a reflection prompt.
- **Unlock Rewards**: Completing an activity unlocks the restricted apps for a limited duration. The more effort you put in, the longer the access you get.
- **Progress & Analytics**: A dedicated screen tracks all completed activities, points earned, daily streaks, and unlock sessions, providing visible progress and motivation.
- **Achievements**: Gamified achievements for reaching milestones like maintaining a streak or earning points.

## 🛠️ Tech Stack & Key Libraries

- **UI**: 100% Kotlin with [Jetpack Compose](https://developer.android.com/jetpack/compose) for a modern, declarative UI.
- **Architecture**: Follows recommended Android architecture patterns (UI Layer -> ViewModel -> Repository -> Data Source).
- **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) for managing background threads and asynchronous tasks.
- **Database**: [Room](https://developer.android.com/jetpack/androidx/releases/room) for local persistence of user data, blocked apps, and activity history.
- **Navigation**: [Jetpack Navigation for Compose](https://developer.android.com/jetpack/compose/navigation) for handling all in-app navigation.
- **Camera & Machine Learning**:
  - [CameraX](https://developer.android.com/training/camerax) for a robust and easy-to-use camera preview.
  - [Google ML Kit Pose Detection](https://developers.google.com/ml-kit/vision/pose-detection) for real-time analysis of body poses to count push-ups.
- **App Blocking**: [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) to monitor foreground apps and enforce restrictions.

## 🚀 Setup & Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/MindArc.git
    ```
2.  **Open in Android Studio:**
    - Open Android Studio and select `File > Open`.
    - Navigate to the cloned repository and select it.
3.  **Build the project:**
    - Let Android Studio sync the Gradle files.
    - Build the project by selecting `Build > Make Project`.
4.  **Enable Accessibility Service:**
    - For the app blocking to function, you must manually enable the MindArc accessibility service in your device's settings after installation.
    - Go to `Settings > Accessibility > Installed apps > MindArc` and turn it on.

## 🔮 Future Enhancements

- Additional exercise options (e.g., squats, jumping jacks).
- Smartwatch integration for activity tracking.
- Social challenges and leaderboards.
- Adaptive difficulty based on user behavior.

---
