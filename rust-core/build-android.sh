#!/bin/bash

# Build script for Rust native library for Android

set -e

echo "Building Termux-Ultra Rust core for Android..."

# Define Android targets
TARGETS=("aarch64-linux-android" "armv7-linux-androideabi")

# Build for each target
for TARGET in "${TARGETS[@]}"; do
    echo "Building for target: $TARGET"
    
    # Add target if not already added
    rustup target add $TARGET
    
    # Build the library
    cargo build --target $TARGET --release
    
    # Create target directory in app
    mkdir -p ../app/src/main/jniLibs/${TARGET//-/_}/
    
    # Copy the built library
    if [ "$TARGET" = "aarch64-linux-android" ]; then
        ABI_DIR="arm64-v8a"
    elif [ "$TARGET" = "armv7-linux-androideabi" ]; then
        ABI_DIR="armeabi-v7a"
    else
        ABI_DIR="$TARGET"
    fi
    
    mkdir -p ../app/src/main/jniLibs/$ABI_DIR/
    cp target/$TARGET/release/libtermux_ultra_bifrost.so ../app/src/main/jniLibs/$ABI_DIR/
    
    echo "Built and copied library for $TARGET -> $ABI_DIR"
done

echo "Rust build complete!"

# Also create a version for desktop testing
echo "Building for host (testing)..."
cargo build --release

echo "All builds complete!"