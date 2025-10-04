# 🛠️ Termux-Ultra Troubleshooting Guide

Quick fixes for common issues when building and running Termux-Ultra.

## 🚨 Build Issues

### Issue: Java Version Error
```
Android Gradle plugin requires Java 17 to run. You are currently using Java 11.
```

**Fix:**
```bash
# Install Java 17
sudo apt install openjdk-17-jdk

# Or use SDKMAN
sdk install java 17.0.15-ms
sdk use java 17.0.15-ms

# Verify
java -version
```

### Issue: Android SDK Not Found
```
SDK location not found. Define a valid SDK location with ANDROID_HOME
```

**Fix:**
```bash
# Setup Android SDK
source android-setup.sh

# Or manually set
export ANDROID_HOME=/opt/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
```

### Issue: Kotlin/Compose Version Mismatch
```
This version (1.5.13) of the Compose Compiler requires Kotlin version 1.9.23
```

**Fix:** Update `app/build.gradle.kts`:
```kotlin
composeOptions {
    kotlinCompilerExtensionVersion = "1.5.14"  // Update this
}
```

### Issue: Missing Rust Library
```
ninja: error: libtermux_ultra_bifrost.so missing
```

**Fix:**
```bash
cd rust-core
./build-android.sh
cd ..
./gradlew assembleDebug
```

### Issue: Out of Memory
```
OutOfMemoryError during build
```

**Fix:** Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
org.gradle.parallel=true
org.gradle.caching=true
```

## 📱 Runtime Issues

### Issue: App Crashes on Launch
**Diagnosis:**
```bash
adb logcat | grep -E "(TermuxUltra|AndroidRuntime)"
```

**Common Fixes:**
- Ensure device runs Android 8.0+ (API 26)
- Check architecture compatibility (arm64-v8a recommended)
- Grant storage permissions in Settings

### Issue: ChatAgent Not Working
**Symptoms:** Commands not recognized, AI responses empty

**Fix:**
1. Toggle AI mode in app: Tap "AI" button
2. Check status: Tap "Status" button
3. Try simple commands first: "help", "list files"

### Issue: File Operations Fail
**Symptoms:** "Permission denied" errors

**Fix:**
1. Grant storage permissions in Android Settings
2. Use app's internal directory for testing
3. Check current directory with "where am i"

## 🔧 Development Issues

### Issue: IDE Can't Find Classes
**Fix:**
```bash
./gradlew clean
./gradlew assembleDebug
```
Then refresh/reimport in your IDE.

### Issue: Git Issues
```bash
# Reset to clean state
git clean -fd
git reset --hard HEAD

# Or start fresh
git stash
git pull origin main
```

### Issue: Gradle Daemon Problems
```bash
./gradlew --stop
./gradlew clean assembleDebug
```

## 📊 Performance Issues

### Issue: Slow Build Times
**Fix:**
```bash
# Add to gradle.properties
echo "org.gradle.parallel=true" >> gradle.properties
echo "org.gradle.caching=true" >> gradle.properties
echo "org.gradle.daemon=true" >> gradle.properties

# Use specific targets
./gradlew assembleDebug -Pandroid.abi.split=arm64-v8a
```

### Issue: Large APK Size
**Analysis:**
```bash
# Check APK contents
unzip -l app/build/outputs/apk/debug/app-debug.apk | head -20

# Enable APK splitting in app/build.gradle.kts
android {
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
        }
    }
}
```

## 🐛 Debugging Tips

### Enable Verbose Logging
Add to `app/src/main/AndroidManifest.xml`:
```xml
<application android:debuggable="true">
```

### Check Device Logs
```bash
# General logs
adb logcat

# App-specific logs
adb logcat | grep com.example.termuxultra

# Clear logs first
adb logcat -c
```

### Test on Different Devices
```bash
# List connected devices
adb devices

# Install on specific device
adb -s DEVICE_ID install app.apk
```

## 🔄 Quick Reset

### Complete Clean Build
```bash
# Clean everything
./gradlew clean
rm -rf app/build
rm -rf rust-core/target

# Rebuild from scratch
source android-setup.sh
./gradlew assembleDebug
```

### Reset App State
```bash
# Uninstall completely
adb uninstall com.example.termuxultra

# Clear data (if installed)
adb shell pm clear com.example.termuxultra

# Reinstall
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📞 Getting Help

### Collect Debug Information
```bash
# System info
uname -a
java -version
./gradlew --version

# Build info
./gradlew assembleDebug --info > build.log 2>&1

# Runtime info
adb logcat > runtime.log &
# Run app, reproduce issue, then:
# kill %1
```

### Useful Commands
```bash
# Check environment
./build-verify.sh

# Test specific component
./test_project.sh

# Clean slate
git clean -fd && git reset --hard HEAD
```

## 📚 Additional Resources

- **Main Documentation**: README.md
- **Deployment Guide**: DEPLOYMENT_GUIDE.md
- **Chat Integration**: CHATGENT_INTEGRATION.md
- **Testing Guide**: TESTING.md

---

**Remember**: When reporting issues, include:
1. Error messages (exact text)
2. Build logs (`./gradlew assembleDebug --info`)
3. Device/environment information
4. Steps to reproduce

Most issues can be resolved by:
1. Checking environment setup
2. Cleaning and rebuilding
3. Verifying permissions
4. Testing on different devices