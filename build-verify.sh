#!/bin/bash
# Build Verification Script for Termux-Ultra

set -e  # Exit on any error

echo "🔧 Termux-Ultra Build Verification"
echo "=================================="

# Check prerequisites
echo "📋 Checking Prerequisites..."

# Check Java version
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo "✅ Java: $JAVA_VERSION"
    
    # Verify Java 17+
    if [[ "$JAVA_VERSION" < "17" ]]; then
        echo "❌ Java 17+ required, found $JAVA_VERSION"
        exit 1
    fi
else
    echo "❌ Java not found"
    exit 1
fi

# Check Android SDK
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
    echo "✅ Android SDK: $ANDROID_HOME"
else
    echo "❌ ANDROID_HOME not set or invalid"
    echo "💡 Run: source android-setup.sh"
    exit 1
fi

# Check Rust (optional)
if command -v rustc &> /dev/null; then
    RUST_VERSION=$(rustc --version | cut -d' ' -f2)
    echo "✅ Rust: $RUST_VERSION"
else
    echo "⚠️ Rust not found - Bifrost core won't be rebuilt"
fi

# Check gradlew
if [ -f "./gradlew" ]; then
    echo "✅ Gradle wrapper found"
else
    echo "❌ gradlew not found in current directory"
    exit 1
fi

echo ""
echo "🏗️ Starting Build Process..."

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Build debug APK
echo "🔨 Building debug APK..."
./gradlew assembleDebug

# Verify APK was created
DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$DEBUG_APK" ]; then
    APK_SIZE=$(du -h "$DEBUG_APK" | cut -f1)
    echo "✅ Debug APK created: $APK_SIZE"
    echo "📍 Location: $DEBUG_APK"
else
    echo "❌ Debug APK not found"
    exit 1
fi

# Run tests
echo "🧪 Running unit tests..."
./gradlew test

# Check for common issues
echo "🔍 Checking for common issues..."

# Check APK size
APK_SIZE_BYTES=$(stat -f%z "$DEBUG_APK" 2>/dev/null || stat -c%s "$DEBUG_APK" 2>/dev/null)
if [ "$APK_SIZE_BYTES" -gt 100000000 ]; then  # 100MB
    echo "⚠️ APK size is large: $(($APK_SIZE_BYTES / 1024 / 1024))MB"
fi

# Check for native libraries
if unzip -l "$DEBUG_APK" | grep -q "lib/"; then
    echo "✅ Native libraries included"
    echo "   Architectures:"
    unzip -l "$DEBUG_APK" | grep "lib/" | grep "\.so$" | sed 's/.*lib\/\([^\/]*\)\/.*/   - \1/' | sort -u
else
    echo "⚠️ No native libraries found"
fi

echo ""
echo "🎉 Build Verification Complete!"
echo "================================"
echo "📱 Ready for deployment:"
echo "   • Install: adb install $DEBUG_APK"
echo "   • Or copy APK to device and install manually"
echo ""
echo "🔧 For release build:"
echo "   • Run: ./gradlew assembleRelease"
echo "   • Sign with your keystore"
echo ""
echo "📚 See DEPLOYMENT_GUIDE.md for detailed instructions"