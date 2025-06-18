🧭 Blind Assistance Application
An AI-powered Android and Web-based solution designed to aid visually impaired users through object detection, voice-guided navigation, and complete voice control.
🚀 Features
🔍 Object Detection

Real-time object detection using ML Kit

Front & rear camera switch

Voice feedback for identified objects

Swipe gestures for control

🗺️ Navigation Assistance

Google Maps integration

Voice input for destination

Voice-guided turn-by-turn directions

"Back to Menu" button and swipe right gesture

🗣️ Full Voice Interaction

Voice commands for switching modes

Speech-to-text for user commands

Text-to-speech for system responses

Completely accessible UI

🛠️ Tech Stack
Android Studio (Java/Kotlin)

ML Kit – for object detection

Google Maps API – for navigation

Firebase – for database & analytics (optional)

Text-to-Speech (TTS) / Speech-to-Text (STT) – for voice communication

📱 Application Flow
Splash Screen – App logo and loading animation

Main Menu – Choose between Object Detection, Navigation, or Voice Mode

Object Detection Activity – Detect and announce surrounding objects

Navigation Activity – Provide guided navigation to a destination

Voice Access Activity – Control app using just your voice

📂 Project Structure
BlindAssistance/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/blindassistance/
│   │   │   │   ├── MainActivity.java
│   │   │   │   ├── ObjectDetectionActivity.java
│   │   │   │   ├── NavigationActivity.java
│   │   │   │   └── VoiceAccessActivity.java
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── drawable/
│   │   │   │   └── values/
│   ├── build.gradle
├── README.md
└──
