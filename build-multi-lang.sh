#!/bin/bash

# Multi-Language Build and Test Script for Termux-Ultra
# Supports: Kotlin/Android, Rust-core, Java agents, Go agents, Python agents, WASM agents

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
JAVA_VERSION="${JAVA_VERSION:-17}"
RUST_VERSION="${RUST_VERSION:-1.75.0}"
GO_VERSION="${GO_VERSION:-1.21}"
PYTHON_VERSION="${PYTHON_VERSION:-3.11}"
NODE_VERSION="${NODE_VERSION:-18}"

# Default targets
BUILD_ANDROID="${BUILD_ANDROID:-true}"
BUILD_RUST="${BUILD_RUST:-true}"
BUILD_JAVA_AGENTS="${BUILD_JAVA_AGENTS:-true}"
BUILD_GO_AGENTS="${BUILD_GO_AGENTS:-true}"
BUILD_PYTHON_AGENTS="${BUILD_PYTHON_AGENTS:-true}"
BUILD_WASM_AGENTS="${BUILD_WASM_AGENTS:-true}"
RUN_TESTS="${RUN_TESTS:-true}"

echo_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

echo_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

echo_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_command() {
    if ! command -v "$1" &> /dev/null; then
        echo_error "$1 is required but not installed"
        return 1
    fi
}

install_rust() {
    if ! command -v rustc &> /dev/null; then
        echo_info "Installing Rust $RUST_VERSION..."
        curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --default-toolchain "$RUST_VERSION"
        source ~/.cargo/env
    fi
    
    # Add required targets
    rustup target add wasm32-unknown-unknown
    
    # Install useful tools
    cargo install cargo-audit || true
    cargo install wasm-pack || true
}

setup_environment() {
    echo_info "Setting up build environment..."
    
    # Check for required tools
    check_command "git"
    
    # Install Rust if needed
    if [ "$BUILD_RUST" = "true" ] || [ "$BUILD_WASM_AGENTS" = "true" ]; then
        install_rust
    fi
    
    # Setup Go if needed
    if [ "$BUILD_GO_AGENTS" = "true" ] && ! command -v go &> /dev/null; then
        echo_warning "Go not found. Please install Go $GO_VERSION"
    fi
    
    # Setup Python if needed
    if [ "$BUILD_PYTHON_AGENTS" = "true" ] && ! command -v python3 &> /dev/null; then
        echo_warning "Python not found. Please install Python $PYTHON_VERSION"
    fi
    
    # Setup Node.js if needed (for WASM tools)
    if [ "$BUILD_WASM_AGENTS" = "true" ] && ! command -v node &> /dev/null; then
        echo_warning "Node.js not found. Some WASM tools may not work"
    fi
}

build_rust_core() {
    if [ "$BUILD_RUST" != "true" ]; then
        return 0
    fi
    
    echo_info "Building Rust core..."
    cd rust-core
    
    # Format check
    echo_info "Checking Rust formatting..."
    cargo fmt --all -- --check || {
        echo_warning "Rust code is not formatted. Run 'cargo fmt' to fix."
    }
    
    # Lint check
    echo_info "Running Rust lints..."
    cargo clippy --all-targets --all-features -- -D warnings
    
    # Build
    echo_info "Building Rust core (debug)..."
    cargo build
    
    echo_info "Building Rust core (release)..."
    cargo build --release
    
    # Build for Android targets
    if [ "$BUILD_ANDROID" = "true" ]; then
        echo_info "Building Rust core for Android..."
        for target in aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android; do
            if rustup target list --installed | grep -q "$target"; then
                echo_info "Building for $target..."
                cargo build --target "$target" --release || echo_warning "Failed to build for $target"
            else
                echo_warning "Target $target not installed, skipping..."
            fi
        done
    fi
    
    # Tests
    if [ "$RUN_TESTS" = "true" ]; then
        echo_info "Running Rust tests..."
        cargo test --all
    fi
    
    # Security audit
    echo_info "Running security audit..."
    cargo audit || echo_warning "Security audit found issues"
    
    cd ..
    echo_success "Rust core build completed"
}

build_android_app() {
    if [ "$BUILD_ANDROID" != "true" ]; then
        return 0
    fi
    
    echo_info "Building Android application..."
    
    # Check Gradle wrapper
    if [ ! -f "./gradlew" ]; then
        echo_error "Gradle wrapper not found"
        return 1
    fi
    
    chmod +x ./gradlew
    
    # Clean build
    echo_info "Cleaning previous build..."
    ./gradlew clean
    
    # Lint check
    echo_info "Running Android lint..."
    ./gradlew lintDebug || echo_warning "Lint issues found"
    
    # Format check (if ktlint is configured)
    echo_info "Checking Kotlin formatting..."
    ./gradlew ktlintCheck || echo_warning "Kotlin formatting issues found"
    
    # Unit tests
    if [ "$RUN_TESTS" = "true" ]; then
        echo_info "Running unit tests..."
        ./gradlew testDebugUnitTest
    fi
    
    # Build debug APK
    echo_info "Building debug APK..."
    ./gradlew assembleDebug
    
    # Build release APK
    echo_info "Building release APK..."
    ./gradlew assembleRelease
    
    echo_success "Android build completed"
}

build_java_agents() {
    if [ "$BUILD_JAVA_AGENTS" != "true" ]; then
        return 0
    fi
    
    echo_info "Building Java agents..."
    cd agents/java
    
    if [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
        echo_info "Building with Gradle..."
        if [ -f "gradlew" ]; then
            chmod +x gradlew
            ./gradlew build
        else
            gradle build
        fi
    elif [ -f "pom.xml" ]; then
        echo_info "Building with Maven..."
        mvn clean compile
        if [ "$RUN_TESTS" = "true" ]; then
            mvn test
        fi
    else
        echo_info "Building Java files directly..."
        find . -name "*.java" -exec javac {} \;
        
        if [ "$RUN_TESTS" = "true" ]; then
            echo_info "Running Java agent test..."
            java Agent --test || echo_warning "Java agent test failed"
        fi
    fi
    
    cd ../..
    echo_success "Java agents build completed"
}

build_go_agents() {
    if [ "$BUILD_GO_AGENTS" != "true" ]; then
        return 0
    fi
    
    echo_info "Building Go agents..."
    cd agents/go
    
    if [ -f "go.mod" ]; then
        echo_info "Building Go module..."
        go mod tidy
        go build -o agent-go .
        
        if [ "$RUN_TESTS" = "true" ]; then
            echo_info "Running Go tests..."
            go test ./...
        fi
        
        # Format check
        echo_info "Checking Go formatting..."
        if [ "$(gofmt -l .)" != "" ]; then
            echo_warning "Go code is not formatted. Run 'go fmt ./...'"
        fi
        
        # Vet check
        echo_info "Running Go vet..."
        go vet ./...
        
    else
        echo_info "Building single Go file..."
        go build -o agent-go agent.go
        
        if [ "$RUN_TESTS" = "true" ]; then
            echo_info "Running Go agent test..."
            ./agent-go --test || echo_warning "Go agent test failed"
        fi
    fi
    
    cd ../..
    echo_success "Go agents build completed"
}

build_python_agents() {
    if [ "$BUILD_PYTHON_AGENTS" != "true" ]; then
        return 0
    fi
    
    echo_info "Building Python agents..."
    cd agents/python
    
    # Install dependencies
    if [ -f "requirements.txt" ]; then
        echo_info "Installing Python dependencies..."
        pip3 install -r requirements.txt
    fi
    
    # Format and lint check
    if command -v black &> /dev/null; then
        echo_info "Checking Python formatting..."
        black --check . || echo_warning "Python code is not formatted. Run 'black .'"
    fi
    
    if command -v flake8 &> /dev/null; then
        echo_info "Running Python linting..."
        flake8 . || echo_warning "Python linting issues found"
    fi
    
    # Compile Python files
    echo_info "Compiling Python files..."
    python3 -m py_compile *.py
    
    if [ "$RUN_TESTS" = "true" ]; then
        if command -v pytest &> /dev/null && [ -d "tests" ]; then
            echo_info "Running Python tests with pytest..."
            python3 -m pytest
        else
            echo_info "Running Python agent test..."
            python3 agent.py --test || echo_warning "Python agent test failed"
        fi
    fi
    
    cd ../..
    echo_success "Python agents build completed"
}

build_wasm_agents() {
    if [ "$BUILD_WASM_AGENTS" != "true" ]; then
        return 0
    fi
    
    echo_info "Building WASM agents..."
    cd agents/wasm
    
    if [ -f "Cargo.toml" ]; then
        echo_info "Building Rust-based WASM agent..."
        cargo build --target wasm32-unknown-unknown --release
        
        if command -v wasm-pack &> /dev/null; then
            echo_info "Building with wasm-pack..."
            wasm-pack build --target web --out-dir pkg
        fi
        
        if [ "$RUN_TESTS" = "true" ]; then
            echo_info "Running WASM tests..."
            cargo test
        fi
        
    elif [ -f "agent.wat" ]; then
        echo_info "Building WAT to WASM..."
        if command -v wat2wasm &> /dev/null; then
            wat2wasm agent.wat -o agent.wasm
            echo_success "WAT compiled to WASM"
        else
            echo_warning "wat2wasm not found. Install wabt tools to compile WAT files"
        fi
        
    else
        echo_warning "No WASM build configuration found"
    fi
    
    cd ../..
    echo_success "WASM agents build completed"
}

run_integration_tests() {
    if [ "$RUN_TESTS" != "true" ]; then
        return 0
    fi
    
    echo_info "Running integration tests..."
    
    # Test script runner
    if [ -f "test_project.sh" ]; then
        echo_info "Running project test script..."
        chmod +x test_project.sh
        ./test_project.sh || echo_warning "Project tests failed"
    fi
    
    # Test build verification
    if [ -f "build-verify.sh" ]; then
        echo_info "Running build verification..."
        chmod +x build-verify.sh
        ./build-verify.sh || echo_warning "Build verification failed"
    fi
    
    echo_success "Integration tests completed"
}

generate_artifacts() {
    echo_info "Generating build artifacts..."
    
    # Create artifacts directory
    mkdir -p build-artifacts
    
    # Copy Android APKs
    if [ "$BUILD_ANDROID" = "true" ] && [ -d "app/build/outputs/apk" ]; then
        echo_info "Copying Android APKs..."
        cp -r app/build/outputs/apk/* build-artifacts/ 2>/dev/null || true
    fi
    
    # Copy Rust binaries
    if [ "$BUILD_RUST" = "true" ] && [ -d "rust-core/target" ]; then
        echo_info "Copying Rust artifacts..."
        find rust-core/target -name "*.so" -o -name "*.a" -o -name "*.dylib" | while read -r file; do
            cp "$file" build-artifacts/ 2>/dev/null || true
        done
    fi
    
    # Copy agent binaries
    find agents -name "agent-*" -o -name "*.wasm" -o -name "*.jar" | while read -r file; do
        cp "$file" build-artifacts/ 2>/dev/null || true
    done
    
    # Generate build info
    cat > build-artifacts/build-info.json << EOF
{
    "build_date": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "git_commit": "$(git rev-parse HEAD 2>/dev/null || echo 'unknown')",
    "git_branch": "$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')",
    "targets": {
        "android": $BUILD_ANDROID,
        "rust_core": $BUILD_RUST,
        "java_agents": $BUILD_JAVA_AGENTS,
        "go_agents": $BUILD_GO_AGENTS,
        "python_agents": $BUILD_PYTHON_AGENTS,
        "wasm_agents": $BUILD_WASM_AGENTS
    },
    "languages": ["Kotlin", "Rust", "Java", "Go", "Python", "WASM"]
}
EOF
    
    echo_success "Build artifacts generated in build-artifacts/"
}

print_summary() {
    echo_info "Build Summary:"
    echo "=============="
    
    [ "$BUILD_ANDROID" = "true" ] && echo "✅ Android Application"
    [ "$BUILD_RUST" = "true" ] && echo "✅ Rust Core"
    [ "$BUILD_JAVA_AGENTS" = "true" ] && echo "✅ Java Agents"
    [ "$BUILD_GO_AGENTS" = "true" ] && echo "✅ Go Agents"
    [ "$BUILD_PYTHON_AGENTS" = "true" ] && echo "✅ Python Agents"
    [ "$BUILD_WASM_AGENTS" = "true" ] && echo "✅ WASM Agents"
    [ "$RUN_TESTS" = "true" ] && echo "✅ Tests Executed"
    
    echo ""
    echo_success "Multi-language build completed successfully!"
    echo_info "Check build-artifacts/ for generated files"
}

# Main execution
main() {
    echo_info "Starting Termux-Ultra multi-language build..."
    echo_info "Target languages: Kotlin, Rust, Java, Go, Python, WASM"
    
    setup_environment
    build_rust_core
    build_android_app
    build_java_agents
    build_go_agents
    build_python_agents
    build_wasm_agents
    run_integration_tests
    generate_artifacts
    print_summary
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --no-android)
            BUILD_ANDROID=false
            shift
            ;;
        --no-rust)
            BUILD_RUST=false
            shift
            ;;
        --no-java)
            BUILD_JAVA_AGENTS=false
            shift
            ;;
        --no-go)
            BUILD_GO_AGENTS=false
            shift
            ;;
        --no-python)
            BUILD_PYTHON_AGENTS=false
            shift
            ;;
        --no-wasm)
            BUILD_WASM_AGENTS=false
            shift
            ;;
        --no-tests)
            RUN_TESTS=false
            shift
            ;;
        --help)
            echo "Multi-Language Build Script for Termux-Ultra"
            echo ""
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --no-android    Skip Android build"
            echo "  --no-rust       Skip Rust core build"
            echo "  --no-java       Skip Java agents build"
            echo "  --no-go         Skip Go agents build"
            echo "  --no-python     Skip Python agents build"
            echo "  --no-wasm       Skip WASM agents build"
            echo "  --no-tests      Skip running tests"
            echo "  --help          Show this help message"
            echo ""
            echo "Environment variables:"
            echo "  JAVA_VERSION    Java version (default: 17)"
            echo "  RUST_VERSION    Rust version (default: 1.75.0)"
            echo "  GO_VERSION      Go version (default: 1.21)"
            echo "  PYTHON_VERSION  Python version (default: 3.11)"
            exit 0
            ;;
        *)
            echo_error "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Run main function
main