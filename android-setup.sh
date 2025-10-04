#!/bin/bash
# Android SDK Setup Script for Termux-Ultra

# Set up environment variables
export JAVA_HOME=/usr/local/sdkman/candidates/java/17.0.15-ms
export ANDROID_HOME=/opt/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

echo "✅ Android SDK Environment Setup Complete"
echo "JAVA_HOME: $JAVA_HOME"
echo "ANDROID_HOME: $ANDROID_HOME"
echo ""
echo "Available tools:"
echo "- adb: $(which adb 2>/dev/null || echo 'not found')"
echo "- sdkmanager: $(which sdkmanager 2>/dev/null || echo 'not found')"
echo ""
echo "To use in new terminal sessions, run:"
echo "source android-setup.sh"