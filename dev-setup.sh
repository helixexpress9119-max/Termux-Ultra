#!/bin/bash

# Development Environment Setup for Termux-Ultra
# Sets up all required tools for multi-language development

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
JAVA_VERSION="17"
RUST_VERSION="1.75.0"
GO_VERSION="1.21"
PYTHON_VERSION="3.11"
NODE_VERSION="18"
ANDROID_SDK_VERSION="34"

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

detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "linux"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macos"
    elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
        echo "windows"
    else
        echo "unknown"
    fi
}

install_java() {
    echo_info "Installing Java $JAVA_VERSION..."
    
    local os=$(detect_os)
    case $os in
        "linux")
            if command -v apt-get &> /dev/null; then
                sudo apt-get update
                sudo apt-get install -y openjdk-${JAVA_VERSION}-jdk
            elif command -v yum &> /dev/null; then
                sudo yum install -y java-${JAVA_VERSION}-openjdk-devel
            elif command -v dnf &> /dev/null; then
                sudo dnf install -y java-${JAVA_VERSION}-openjdk-devel
            else
                echo_warning "Package manager not supported. Please install Java $JAVA_VERSION manually"
                return 1
            fi
            ;;
        "macos")
            if command -v brew &> /dev/null; then
                brew install openjdk@${JAVA_VERSION}
                echo 'export PATH="/opt/homebrew/opt/openjdk@'${JAVA_VERSION}'/bin:$PATH"' >> ~/.zshrc
            else
                echo_warning "Homebrew not found. Please install Java $JAVA_VERSION manually"
                return 1
            fi
            ;;
        *)
            echo_warning "OS not supported for automatic Java installation"
            return 1
            ;;
    esac
    
    echo_success "Java $JAVA_VERSION installed"
}

install_rust() {
    echo_info "Installing Rust $RUST_VERSION..."
    
    if command -v rustc &> /dev/null; then
        echo_info "Rust already installed, updating..."
        rustup update
    else
        curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --default-toolchain "$RUST_VERSION"
        source ~/.cargo/env
    fi
    
    # Add required targets
    echo_info "Adding Rust targets..."
    rustup target add wasm32-unknown-unknown
    rustup target add aarch64-linux-android
    rustup target add armv7-linux-androideabi
    rustup target add i686-linux-android
    rustup target add x86_64-linux-android
    
    # Install useful tools
    echo_info "Installing Rust tools..."
    cargo install cargo-audit
    cargo install wasm-pack
    cargo install cargo-watch
    cargo install cargo-edit
    
    # Install formatting and linting tools
    rustup component add rustfmt
    rustup component add clippy
    
    echo_success "Rust $RUST_VERSION and tools installed"
}

install_go() {
    echo_info "Installing Go $GO_VERSION..."
    
    local os=$(detect_os)
    local arch=$(uname -m)
    
    case $arch in
        "x86_64")
            arch="amd64"
            ;;
        "aarch64"|"arm64")
            arch="arm64"
            ;;
        "armv7l")
            arch="armv6l"
            ;;
    esac
    
    case $os in
        "linux")
            local go_os="linux"
            ;;
        "macos")
            local go_os="darwin"
            ;;
        "windows")
            local go_os="windows"
            ;;
        *)
            echo_warning "OS not supported for automatic Go installation"
            return 1
            ;;
    esac
    
    local go_archive="go${GO_VERSION}.${go_os}-${arch}.tar.gz"
    local download_url="https://golang.org/dl/${go_archive}"
    
    echo_info "Downloading Go from $download_url..."
    curl -L "$download_url" -o "/tmp/$go_archive"
    
    echo_info "Installing Go..."
    sudo rm -rf /usr/local/go
    sudo tar -C /usr/local -xzf "/tmp/$go_archive"
    
    # Add to PATH
    echo 'export PATH=$PATH:/usr/local/go/bin' >> ~/.bashrc
    echo 'export PATH=$PATH:/usr/local/go/bin' >> ~/.zshrc
    export PATH=$PATH:/usr/local/go/bin
    
    rm "/tmp/$go_archive"
    
    echo_success "Go $GO_VERSION installed"
}

install_python() {
    echo_info "Installing Python $PYTHON_VERSION..."
    
    local os=$(detect_os)
    case $os in
        "linux")
            if command -v apt-get &> /dev/null; then
                sudo apt-get update
                sudo apt-get install -y python${PYTHON_VERSION} python${PYTHON_VERSION}-dev python3-pip python3-venv
            elif command -v yum &> /dev/null; then
                sudo yum install -y python${PYTHON_VERSION} python3-pip python3-devel
            elif command -v dnf &> /dev/null; then
                sudo dnf install -y python${PYTHON_VERSION} python3-pip python3-devel
            else
                echo_warning "Package manager not supported. Please install Python $PYTHON_VERSION manually"
                return 1
            fi
            ;;
        "macos")
            if command -v brew &> /dev/null; then
                brew install python@${PYTHON_VERSION}
            else
                echo_warning "Homebrew not found. Please install Python $PYTHON_VERSION manually"
                return 1
            fi
            ;;
        *)
            echo_warning "OS not supported for automatic Python installation"
            return 1
            ;;
    esac
    
    # Install useful Python tools
    echo_info "Installing Python tools..."
    pip3 install --upgrade pip
    pip3 install black flake8 pytest mypy
    
    echo_success "Python $PYTHON_VERSION and tools installed"
}

install_nodejs() {
    echo_info "Installing Node.js $NODE_VERSION..."
    
    # Install using Node Version Manager (nvm)
    if ! command -v nvm &> /dev/null; then
        echo_info "Installing nvm..."
        curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
        export NVM_DIR="$HOME/.nvm"
        [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
    fi
    
    nvm install "$NODE_VERSION"
    nvm use "$NODE_VERSION"
    nvm alias default "$NODE_VERSION"
    
    # Install useful tools
    echo_info "Installing Node.js tools..."
    npm install -g @wasmtime/wasmtime-cli
    npm install -g wabt
    
    echo_success "Node.js $NODE_VERSION and tools installed"
}

install_android_sdk() {
    echo_info "Installing Android SDK..."
    
    local os=$(detect_os)
    local sdk_tools_url=""
    
    case $os in
        "linux")
            sdk_tools_url="https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip"
            ;;
        "macos")
            sdk_tools_url="https://dl.google.com/android/repository/commandlinetools-mac-9477386_latest.zip"
            ;;
        "windows")
            sdk_tools_url="https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip"
            ;;
        *)
            echo_warning "OS not supported for automatic Android SDK installation"
            return 1
            ;;
    esac
    
    # Set Android SDK path
    export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
    export ANDROID_HOME="$ANDROID_SDK_ROOT"
    export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
    
    # Create directories
    mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
    
    # Download and extract command line tools
    echo_info "Downloading Android command line tools..."
    curl -L "$sdk_tools_url" -o "/tmp/cmdline-tools.zip"
    unzip "/tmp/cmdline-tools.zip" -d "/tmp/"
    mv "/tmp/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
    rm "/tmp/cmdline-tools.zip"
    
    # Install SDK components
    echo_info "Installing Android SDK components..."
    yes | sdkmanager --licenses
    sdkmanager "platform-tools" "platforms;android-${ANDROID_SDK_VERSION}" "build-tools;${ANDROID_SDK_VERSION}.0.0"
    sdkmanager "ndk-bundle" "cmake;3.22.1"
    
    # Add to shell profiles
    echo 'export ANDROID_SDK_ROOT="$HOME/Android/Sdk"' >> ~/.bashrc
    echo 'export ANDROID_HOME="$ANDROID_SDK_ROOT"' >> ~/.bashrc
    echo 'export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"' >> ~/.bashrc
    
    echo 'export ANDROID_SDK_ROOT="$HOME/Android/Sdk"' >> ~/.zshrc
    echo 'export ANDROID_HOME="$ANDROID_SDK_ROOT"' >> ~/.zshrc
    echo 'export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"' >> ~/.zshrc
    
    echo_success "Android SDK installed"
}

install_additional_tools() {
    echo_info "Installing additional development tools..."
    
    local os=$(detect_os)
    case $os in
        "linux")
            if command -v apt-get &> /dev/null; then
                sudo apt-get install -y git curl wget unzip build-essential pkg-config libssl-dev
            elif command -v yum &> /dev/null; then
                sudo yum groupinstall -y "Development Tools"
                sudo yum install -y git curl wget unzip openssl-devel
            elif command -v dnf &> /dev/null; then
                sudo dnf groupinstall -y "Development Tools"
                sudo dnf install -y git curl wget unzip openssl-devel
            fi
            ;;
        "macos")
            if command -v brew &> /dev/null; then
                brew install git curl wget unzip
                xcode-select --install 2>/dev/null || true
            fi
            ;;
    esac
    
    # Install ktlint for Kotlin formatting
    echo_info "Installing ktlint..."
    curl -sSLO https://github.com/pinterest/ktlint/releases/download/0.50.0/ktlint
    chmod a+x ktlint
    sudo mv ktlint /usr/local/bin/
    
    echo_success "Additional tools installed"
}

setup_project() {
    echo_info "Setting up project configuration..."
    
    # Make scripts executable
    chmod +x gradlew 2>/dev/null || true
    chmod +x build-verify.sh 2>/dev/null || true
    chmod +x test_project.sh 2>/dev/null || true
    chmod +x android-setup.sh 2>/dev/null || true
    chmod +x build-multi-lang.sh 2>/dev/null || true
    
    # Create local.properties for Android
    if [ ! -f "local.properties" ] && [ -n "${ANDROID_SDK_ROOT:-}" ]; then
        echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
        echo_info "Created local.properties"
    fi
    
    # Setup Git hooks
    if [ -d ".git" ]; then
        echo_info "Setting up Git hooks..."
        
        cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
# Pre-commit hook for multi-language formatting

echo "Running pre-commit checks..."

# Rust formatting
if [ -d "rust-core" ]; then
    cd rust-core
    cargo fmt --all -- --check || {
        echo "Rust code is not formatted. Run 'cargo fmt' to fix."
        exit 1
    }
    cd ..
fi

# Kotlin formatting
if [ -f "gradlew" ]; then
    ./gradlew ktlintCheck || {
        echo "Kotlin code is not formatted. Run './gradlew ktlintFormat' to fix."
        exit 1
    }
fi

# Go formatting
if [ -d "agents/go" ]; then
    cd agents/go
    if [ "$(gofmt -l .)" != "" ]; then
        echo "Go code is not formatted. Run 'go fmt ./...'"
        exit 1
    fi
    cd ../..
fi

# Python formatting
if [ -d "agents/python" ] && command -v black &> /dev/null; then
    black --check agents/python/ || {
        echo "Python code is not formatted. Run 'black agents/python/'"
        exit 1
    }
fi

echo "All pre-commit checks passed!"
EOF
        chmod +x .git/hooks/pre-commit
        echo_success "Git hooks setup"
    fi
    
    echo_success "Project configuration completed"
}

verify_installation() {
    echo_info "Verifying installation..."
    
    local errors=0
    
    # Check Java
    if command -v java &> /dev/null; then
        local java_ver=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$java_ver" = "$JAVA_VERSION" ]; then
            echo_success "Java $JAVA_VERSION ✓"
        else
            echo_warning "Java version mismatch: expected $JAVA_VERSION, found $java_ver"
        fi
    else
        echo_error "Java not found ✗"
        ((errors++))
    fi
    
    # Check Rust
    if command -v rustc &> /dev/null; then
        local rust_ver=$(rustc --version | cut -d' ' -f2)
        echo_success "Rust $rust_ver ✓"
    else
        echo_error "Rust not found ✗"
        ((errors++))
    fi
    
    # Check Go
    if command -v go &> /dev/null; then
        local go_ver=$(go version | cut -d' ' -f3 | sed 's/go//')
        echo_success "Go $go_ver ✓"
    else
        echo_error "Go not found ✗"
        ((errors++))
    fi
    
    # Check Python
    if command -v python3 &> /dev/null; then
        local python_ver=$(python3 --version | cut -d' ' -f2)
        echo_success "Python $python_ver ✓"
    else
        echo_error "Python not found ✗"
        ((errors++))
    fi
    
    # Check Node.js
    if command -v node &> /dev/null; then
        local node_ver=$(node --version | sed 's/v//')
        echo_success "Node.js $node_ver ✓"
    else
        echo_warning "Node.js not found (optional)"
    fi
    
    # Check Android SDK
    if [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
        echo_success "Android SDK ✓"
    else
        echo_warning "Android SDK not found"
    fi
    
    if [ $errors -eq 0 ]; then
        echo_success "All required tools are installed!"
    else
        echo_error "$errors error(s) found. Please fix before continuing."
        return 1
    fi
}

print_next_steps() {
    echo_info "Setup completed! Next steps:"
    echo "=========================="
    echo ""
    echo "1. Restart your terminal or run: source ~/.bashrc"
    echo "2. Test the build: ./build-multi-lang.sh"
    echo "3. Start development:"
    echo "   - Android: ./gradlew assembleDebug"
    echo "   - Rust: cd rust-core && cargo build"
    echo "   - Go: cd agents/go && go build"
    echo "   - Python: cd agents/python && python3 agent.py"
    echo ""
    echo "Available languages:"
    echo "✅ Kotlin/Android UI"
    echo "✅ Rust Core Engine"
    echo "✅ Java Agents"
    echo "✅ Go Agents"
    echo "✅ Python Agents"
    echo "✅ WASM Agents"
    echo ""
    echo_success "Happy coding! 🚀"
}

# Main execution
main() {
    echo_info "Starting Termux-Ultra development environment setup..."
    echo_info "This will install: Java $JAVA_VERSION, Rust $RUST_VERSION, Go $GO_VERSION, Python $PYTHON_VERSION, Node.js $NODE_VERSION"
    echo ""
    
    read -p "Continue with installation? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Installation cancelled."
        exit 0
    fi
    
    install_java || echo_warning "Java installation failed"
    install_rust || echo_warning "Rust installation failed"
    install_go || echo_warning "Go installation failed"
    install_python || echo_warning "Python installation failed"
    install_nodejs || echo_warning "Node.js installation failed"
    install_android_sdk || echo_warning "Android SDK installation failed"
    install_additional_tools || echo_warning "Additional tools installation failed"
    setup_project
    verify_installation
    print_next_steps
}

# Parse command line arguments
case "${1:-}" in
    --help)
        echo "Development Environment Setup for Termux-Ultra"
        echo ""
        echo "Usage: $0 [--help]"
        echo ""
        echo "This script installs all required tools for multi-language development:"
        echo "- Java $JAVA_VERSION"
        echo "- Rust $RUST_VERSION"
        echo "- Go $GO_VERSION"
        echo "- Python $PYTHON_VERSION"
        echo "- Node.js $NODE_VERSION"
        echo "- Android SDK"
        echo "- Development tools and utilities"
        exit 0
        ;;
esac

# Run main function
main