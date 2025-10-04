# 🚀 Termux-Ultra Deployment Guide

Complete guide for building, testing, and deploying the Termux-Ultra Android application.

## 📋 Prerequisites

### Development Environment

- **Operating System**: Linux (Ubuntu 20.04+ recommended) or macOS
- **Java**: OpenJDK 17 (required for Android Gradle Plugin 8.3.2+)
- **Android SDK**: API Level 35 with Build Tools 35.0.0
- **NDK**: 25.1.8937393 (automatically installed during build)
- **CMake**: 3.22.1+ (for native library compilation)
- **Rust**: 1.70+ (for Bifrost core compilation)

### Hardware Requirements

- **RAM**: Minimum 8GB, recommended 16GB
- **Storage**: 10GB free space for SDK and build artifacts
- **CPU**: Multi-core processor recommended for faster builds

## 🛠️ Environment Setup

### 1. Install Java 17

#### Ubuntu/Debian:
```bash
sudo apt update
sudo apt install openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

#### Using SDKMAN (Recommended):
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.15-ms
sdk use java 17.0.15-ms
```

### 2. Setup Android SDK

Run the provided setup script:
```bash
chmod +x android-setup.sh
source android-setup.sh
```

Or manually:
```bash
# Download and install Android command line tools
cd /tmp
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
sudo mkdir -p /opt/android-sdk/cmdline-tools
sudo mv cmdline-tools /opt/android-sdk/cmdline-tools/latest

# Set environment variables
export ANDROID_HOME=/opt/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# Accept licenses and install required packages
yes | sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

### 3. Setup Rust (for Bifrost Core)

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
source $HOME/.cargo/env

# Add Android targets
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
rustup target add x86_64-linux-android
rustup target add i686-linux-android
```

### 4. Verify Environment

```bash
# Check Java version
java -version

# Check Android SDK
sdkmanager --list | grep "build-tools;35"

# Check Rust
rustc --version
```

## 🔨 Building the Application

### 1. Clone and Prepare

```bash
git clone https://github.com/helixexpress9119-max/Termux-Ultra.git
cd Termux-Ultra

# Make gradlew executable
chmod +x gradlew

# Setup environment for this session
source android-setup.sh
```

### 2. Build Rust Core (Optional - Auto-built during Android build)

```bash
cd rust-core
./build-android.sh
cd ..
```

### 3. Build Android Application

#### Debug Build:
```bash
./gradlew assembleDebug
```

#### Release Build:
```bash
./gradlew assembleRelease
```

#### Build with Specific Target:
```bash
# For specific architecture only
./gradlew assembleDebug -Pandroid.abi.split=arm64-v8a
```

### 4. Generated APK Locations

- **Debug**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release**: `app/build/outputs/apk/release/app-release.apk`

## 🧪 Testing

### 1. Unit Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew :app:testDebugUnitTest --tests="*ChatAgentTest*"

# Generate test report
./gradlew testDebugUnitTest
# Report location: app/build/reports/tests/testDebugUnitTest/index.html
```

### 2. Instrumentation Tests

```bash
# Run on connected device/emulator
./gradlew connectedDebugAndroidTest

# Run specific test
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.termuxultra.InstrumentedTest
```

### 3. Integration Testing

Use the provided test script:
```bash
chmod +x test_project.sh
./test_project.sh
```

## 📱 Device Deployment

### 1. Enable Developer Options

On your Android device:
1. Go to **Settings** → **About Phone**
2. Tap **Build Number** 7 times
3. Go back to **Settings** → **Developer Options**
4. Enable **USB Debugging**

### 2. Install via ADB

```bash
# Connect device and verify
adb devices

# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Install with replacement
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Uninstall if needed
adb uninstall com.example.termuxultra
```

### 3. Install via File Transfer

1. Copy APK to device storage
2. Open file manager on device
3. Tap APK file and follow installation prompts
4. Enable "Install from Unknown Sources" if prompted

## 🔧 Troubleshooting

### Common Build Issues

#### 1. Kotlin/Compose Compatibility Error
```
Error: This version (1.5.13) of the Compose Compiler requires Kotlin version 1.9.23
```

**Solution**: Update Compose compiler version in `app/build.gradle.kts`:
```kotlin
composeOptions {
    kotlinCompilerExtensionVersion = "1.5.14"
}
```

#### 2. Missing Rust Library
```
ninja: error: libtermux_ultra_bifrost.so missing
```

**Solution**: Build Rust core first:
```bash
cd rust-core
./build-android.sh
cd ..
```

#### 3. SDK Location Not Found
```
SDK location not found. Define a valid SDK location with ANDROID_HOME
```

**Solution**: Set environment variables:
```bash
export ANDROID_HOME=/opt/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
```

#### 4. Out of Memory During Build
```
OutOfMemoryError during build
```

**Solution**: Increase Gradle memory in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
```

### Runtime Issues

#### 1. App Crashes on Start
- Check logcat: `adb logcat | grep TermuxUltra`
- Verify device architecture matches APK (arm64-v8a recommended)
- Ensure minimum Android API 26 (Android 8.0)

#### 2. ChatAgent Not Responding
- Check if LlamaEngine initialized properly
- Verify AI models are accessible
- Toggle to Simple Mode if AI fails

#### 3. File Permission Issues
- Ensure app has storage permissions
- Check if running on Android 11+ with scoped storage

## 📦 Release Preparation

### 1. Generate Signed APK

Create `keystore.properties` in project root:
```properties
storeFile=path/to/your/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

Update `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        release {
            storeFile file(keystoreProperties['storeFile'])
            storePassword keystoreProperties['storePassword']
            keyAlias keystoreProperties['keyAlias']
            keyPassword keystoreProperties['keyPassword']
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            isMinifyEnabled true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}
```

### 2. Generate Release Build

```bash
./gradlew assembleRelease
```

### 3. APK Optimization

```bash
# Install bundletool
curl -L -o bundletool.jar https://github.com/google/bundletool/releases/latest/download/bundletool-all-1.15.4.jar

# Generate AAB (Android App Bundle)
./gradlew bundleRelease

# Test APK set generation
java -jar bundletool.jar build-apks --bundle=app/build/outputs/bundle/release/app-release.aab --output=app.apks
```

## 🚀 Distribution

### Google Play Store

1. **Upload AAB**: Use Android App Bundle (.aab) format
2. **Target API**: Ensure targeting latest Android API
3. **Permissions**: Review and justify all permissions
4. **Content Rating**: Complete content questionnaire
5. **Testing**: Use internal testing track first

### Alternative Distribution

1. **GitHub Releases**: Upload signed APK
2. **F-Droid**: Submit for open-source distribution
3. **Direct Download**: Host APK on your website

## 📊 Performance Optimization

### Build Performance

```bash
# Enable parallel builds
echo "org.gradle.parallel=true" >> gradle.properties
echo "org.gradle.caching=true" >> gradle.properties

# Use daemon
echo "org.gradle.daemon=true" >> gradle.properties
```

### Runtime Performance

1. **ProGuard**: Enable for release builds
2. **APK Size**: Use APK splitting for different architectures
3. **Memory**: Monitor heap usage with Android Studio Profiler

## 🔐 Security Considerations

### Code Obfuscation

Enable ProGuard/R8 in release builds:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
    }
}
```

### Network Security

Add network security config in `app/src/main/res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">your-api-domain.com</domain>
    </domain-config>
</network-security-config>
```

## 📋 Deployment Checklist

### Pre-Deployment
- [ ] All tests passing
- [ ] Code review completed
- [ ] Version number updated
- [ ] Release notes prepared
- [ ] Signing key secured

### Build Verification
- [ ] Clean build successful
- [ ] APK size acceptable (<100MB)
- [ ] All architectures supported
- [ ] No debug information in release

### Testing
- [ ] Manual testing on target devices
- [ ] Performance benchmarks met
- [ ] Battery usage acceptable
- [ ] Memory leaks checked

### Distribution
- [ ] Store listing updated
- [ ] Screenshots current
- [ ] Metadata localized
- [ ] Privacy policy updated

## 📞 Support

### Getting Help

1. **Issues**: Report bugs on GitHub Issues
2. **Discussions**: Use GitHub Discussions for questions
3. **Documentation**: Check project README and wiki
4. **Community**: Join development Discord/Slack

### Contributing

1. Fork the repository
2. Create feature branch
3. Follow coding standards
4. Add tests for new features
5. Submit pull request

---

## 📈 Version History

- **v1.0.0**: Initial release with ChatAgent and AI integration
- **Future**: Planned features and improvements

## 🏆 Best Practices

1. **Test on Real Devices**: Emulators don't catch all issues
2. **Monitor Performance**: Use Android Vitals and crash reporting
3. **Update Dependencies**: Keep libraries current for security
4. **Document Changes**: Maintain detailed changelog
5. **Backup Keys**: Secure signing keys and passwords

---

**Happy Deploying! 🎉**

For more detailed information, see the individual documentation files in the project repository.