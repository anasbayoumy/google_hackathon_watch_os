# AI Emergency Companion - Wear OS App

A voice-only Wear OS companion app for the AI Emergency Companion that allows users to record emergency descriptions and receive AI-generated SMS messages.

## 🎯 Features

### Current Implementation (Prototype)
- ✅ **Voice Recording**: Tap-to-record emergency descriptions
- ✅ **Modern UI**: Clean, emergency-focused Wear OS interface
- ✅ **Recording States**: Visual feedback for recording/processing
- ✅ **Permissions**: Proper audio recording permissions
- ✅ **Wear OS Optimized**: Designed for round and square watch faces

### Planned Features
- 🔄 **Phone Communication**: Data Layer API integration
- 🔄 **AI Processing**: Send voice to phone for AI analysis
- 🔄 **SMS Display**: Show generated emergency SMS
- 🔄 **Location Integration**: Include GPS coordinates
- 🔄 **Emergency Contacts**: Quick access to emergency services

## 🚀 Running the App

### Prerequisites
1. **Wear OS Emulator** running on `emulator-5554`
2. **Android SDK** with platform-tools in PATH
3. **Java 11+** for building

### Quick Start
```bash
# Navigate to watch app directory
cd AndroidStudioProjects/retat/ai_comp_wearableos

# Run the automated script (Windows)
.\run_watch_app.ps1

# Or manually:
.\gradlew assembleDebug
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5554 shell am start -n com.example.ai_comp_wearableos/.presentation.MainActivity
```

## 📱 User Interface

### Main Screen
- **Large Microphone Button**: Tap to start/stop recording
- **Status Text**: Shows current state (Ready/Recording)
- **Emergency Branding**: Clear AI Emergency title

### Recording Screen
- **Red Stop Button**: Stop recording when active
- **Recording Indicator**: Visual feedback during recording
- **Instructions**: "Describe your emergency"

### Processing Screen
- **Loading Spinner**: Shows AI analysis in progress
- **Status Text**: "Analyzing..." and "Generating emergency SMS"

### Results Screen (Planned)
- **SMS Display**: Shows generated emergency message
- **New Emergency Button**: Return to main screen

## 🔧 Technical Details

### Architecture
- **Platform**: Wear OS 3.0+ (API 30+)
- **UI Framework**: Jetpack Compose for Wear
- **Language**: Kotlin
- **Audio**: MediaRecorder for voice capture

### Key Components
```kotlin
MainActivity.kt          // Main activity with recording logic
MainRecordingScreen()    // Primary voice recording interface
ProcessingScreen()       // AI analysis loading state
SmsResultScreen()        // Emergency SMS display (planned)
```

### Permissions Required
- `RECORD_AUDIO`: Voice recording
- `WRITE_EXTERNAL_STORAGE`: Audio file storage
- `WAKE_LOCK`: Keep watch awake during emergency

## 🔄 Communication Flow (Planned)

```
Watch App → Data Layer API → Phone App → AI Processing → SMS Generation → Watch Display
```

1. **Voice Input**: User records emergency description
2. **Send to Phone**: Audio data transmitted via Wear Data Layer
3. **AI Processing**: Phone app processes with Gemma model
4. **SMS Generation**: AI creates emergency message with location
5. **Return to Watch**: SMS displayed on watch for confirmation

## 🎨 Design Principles

### Emergency-First Design
- **Large Touch Targets**: Easy to use in stress situations
- **Clear Visual Hierarchy**: Important actions prominently displayed
- **Minimal Steps**: Single tap to start emergency process
- **High Contrast**: Readable in various lighting conditions

### Wear OS Optimization
- **Round/Square Support**: Adaptive layouts for different watch shapes
- **Battery Efficient**: Minimal background processing
- **Quick Access**: Fast app launch and interaction
- **Gesture Friendly**: Optimized for small screen interaction

## 📋 Development Status

### ✅ Completed
- Basic Wear OS app structure
- Voice recording functionality
- Emergency-focused UI design
- Permission handling
- Build and deployment scripts

### 🔄 In Progress
- Data Layer API integration
- Phone app communication
- Voice-to-text processing

### 📅 Planned
- Real-time SMS generation
- Emergency contact integration
- Health sensor integration
- Offline emergency features

## 🚀 Next Steps

1. **Implement Data Layer API** for phone communication
2. **Add voice-to-text** processing capability
3. **Integrate with main phone app** AI service
4. **Test end-to-end** emergency workflow
5. **Add emergency contacts** and quick actions

This prototype demonstrates the core voice recording functionality and provides a foundation for the full emergency companion experience on Wear OS.
