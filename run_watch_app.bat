@echo off
echo Building and running AI Emergency Watch App...
echo.

echo Step 1: Building the watch app...
call gradlew assembleDebug
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo Step 2: Installing on emulator-5554...
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% neq 0 (
    echo Installation failed! Make sure emulator-5554 is running.
    pause
    exit /b 1
)

echo.
echo Step 3: Launching the app...
adb -s emulator-5554 shell am start -n com.example.ai_comp_wearableos/.presentation.MainActivity
if %ERRORLEVEL% neq 0 (
    echo Launch failed!
    pause
    exit /b 1
)

echo.
echo ✅ AI Emergency Watch App launched successfully on emulator-5554!
echo.
echo The app should now be running on your Wear OS emulator.
echo You can:
echo - Tap the microphone button to start recording
echo - Speak your emergency description
echo - Tap stop to end recording
echo - See the processing screen (currently simulated)
echo.
pause
