@echo off
echo ========================================
echo   AI Emergency Companion - Watch App
echo   Build and Install Script
echo ========================================
echo.

echo Step 1: Cleaning previous build...
call gradlew clean
if %ERRORLEVEL% neq 0 (
    echo ❌ Clean failed!
    pause
    exit /b 1
)

echo.
echo Step 2: Building the watch app...
call gradlew assembleDebug
if %ERRORLEVEL% neq 0 (
    echo ❌ Build failed!
    pause
    exit /b 1
)

echo.
echo Step 3: Installing on emulator-5554...
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% neq 0 (
    echo ❌ Installation failed! Make sure emulator-5554 is running.
    echo.
    echo Troubleshooting:
    echo - Check if Wear OS emulator is running: adb devices
    echo - Start emulator if needed: emulator -avd Wear_OS_Large_Round_API_30
    pause
    exit /b 1
)

echo.
echo Step 4: Launching the app...
adb -s emulator-5554 shell am start -n com.example.myapp/.presentation.MainActivity
if %ERRORLEVEL% neq 0 (
    echo ❌ Launch failed!
    pause
    exit /b 1
)

echo.
echo ✅ SUCCESS: AI Emergency Watch App installed and launched!
echo.
echo 📱 The app should now be running on your Wear OS emulator.
echo.
echo 🎯 Testing Instructions:
echo 1. Tap the microphone button to start recording
echo 2. Speak your emergency description (e.g., "help me i got kidnapped")
echo 3. Tap stop to end recording
echo 4. Watch will send audio to phone for AI processing
echo 5. Phone will generate emergency SMS and send back to watch
echo 6. Watch will display the generated SMS
echo.
echo 🔍 Debugging:
echo - Watch logs: adb -s emulator-5554 logcat -s WearApp
echo - Phone logs: adb -s 9HG6E6MNRWKRAAR4 logcat -s WearDataListener
echo.
pause
