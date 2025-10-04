# CI/CD Pipeline Documentation

This document describes the comprehensive CI/CD pipeline for Termux-Ultra, supporting multi-language development with Kotlin, Rust, Java, Go, Python, and WASM.

## 🏗️ Pipeline Overview

### Main CI/CD Pipeline (`.github/workflows/ci-cd.yml`)

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main`
- Release publications

**Jobs:**
1. **Lint and Format Check** - Multi-language code quality checks
2. **Rust Build & Test** - Core engine compilation and testing
3. **Android Build & Test Matrix** - Multi-API level Android testing
4. **Agent Tests** - Individual language agent validation
5. **Security Analysis** - Vulnerability and dependency scanning
6. **Release Build** - Production APK generation and deployment

### Nightly Builds (`.github/workflows/nightly.yml`)

**Triggers:**
- Daily at 2 AM UTC
- Manual workflow dispatch

**Features:**
- Automatic change detection
- Nightly APK generation
- Auto-cleanup of old releases
- Development preview releases

### Security Scanning (`.github/workflows/security.yml`)

**Triggers:**
- Push to main branches
- Weekly scheduled scans
- Pull requests

**Security Checks:**
- OWASP dependency analysis
- Rust security audit
- Go vulnerability scanning
- Python safety checks
- Secret detection
- License compliance

## 🛠️ Supported Languages & Tools

### 🎯 Primary Languages
- **Kotlin/Android UI** - Main application interface
- **Rust Core** - High-performance engine and native components

### 🤖 Agent Languages
- **Java** - Enterprise integration agents
- **Go** - System-level and networking agents
- **Python** - AI/ML and scripting agents
- **WASM** - Cross-platform portable agents

### 🔧 Build Tools
- **Gradle** - Android/Kotlin/Java build system
- **Cargo** - Rust package manager and build tool
- **Go Modules** - Go dependency management
- **pip/venv** - Python package management
- **wasm-pack** - WebAssembly build tool

## 📁 Project Structure

```
Termux-Ultra/
├── .github/
│   ├── workflows/
│   │   ├── ci-cd.yml           # Main CI/CD pipeline
│   │   ├── nightly.yml         # Nightly builds
│   │   └── security.yml        # Security scanning
│   └── security-config.yml     # Security configuration
├── agents/
│   ├── java/                   # Java agents
│   ├── go/                     # Go agents
│   ├── python/                 # Python agents
│   └── wasm/                   # WASM agents
├── rust-core/                  # Rust core engine
├── app/                        # Android application
├── build-multi-lang.sh         # Multi-language build script
├── dev-setup.sh               # Development environment setup
└── CI-CD-README.md            # This file
```

## 🚀 Getting Started

### Development Environment Setup

1. **Automatic Setup** (Recommended):
   ```bash
   ./dev-setup.sh
   ```

2. **Manual Setup**:
   - Java 17+
   - Rust 1.75.0+
   - Go 1.21+
   - Python 3.11+
   - Node.js 18+ (for WASM tools)
   - Android SDK

### Building the Project

1. **Full Multi-Language Build**:
   ```bash
   ./build-multi-lang.sh
   ```

2. **Selective Building**:
   ```bash
   # Skip specific languages
   ./build-multi-lang.sh --no-go --no-python
   
   # Build without tests
   ./build-multi-lang.sh --no-tests
   ```

3. **Individual Components**:
   ```bash
   # Android only
   ./gradlew assembleDebug
   
   # Rust core only
   cd rust-core && cargo build --release
   
   # Go agents only
   cd agents/go && go build
   ```

## 🔄 CI/CD Workflow Details

### 1. Code Quality Checks

**Kotlin/Java:**
- ktlint formatting
- Android lint
- Gradle dependency analysis

**Rust:**
- `cargo fmt` formatting
- `clippy` linting
- Security audit with `cargo audit`

**Go:**
- `gofmt` formatting
- `go vet` static analysis
- Vulnerability checking with `govulncheck`

**Python:**
- Black formatting
- Flake8 linting
- Safety dependency scanning
- Bandit security analysis

### 2. Testing Strategy

**Unit Tests:**
- Kotlin: Android unit tests with JUnit
- Rust: Native Cargo tests
- Go: Built-in testing framework
- Python: pytest framework

**Integration Tests:**
- Android instrumented tests on multiple API levels
- Cross-language agent communication tests
- End-to-end workflow validation

**Matrix Testing:**
- Android API levels: 26, 28, 30, 33, 34
- Multiple architecture targets
- Different OS environments

### 3. Security Pipeline

**Vulnerability Scanning:**
- OWASP Dependency Check for Java/Android
- Cargo Audit for Rust dependencies
- Nancy/govulncheck for Go
- Safety for Python packages

**Code Analysis:**
- Semgrep SAST scanning
- Secret detection with TruffleHog
- License compatibility checks
- Container security (if applicable)

**Automated Response:**
- High-severity issue notifications
- Automatic security issue creation
- Weekly security reports

### 4. Release Process

**Automated Versioning:**
- Nightly builds: `nightly-YYYYMMDD-{commit}`
- Release builds: Git tag-based versioning

**Artifact Generation:**
- Debug and release APKs
- Native libraries for all targets
- Agent binaries and WASM modules

**Distribution:**
- GitHub Releases for tagged versions
- Nightly prereleases for development
- Artifact retention and cleanup

## 🔧 Configuration

### Environment Variables

**Build Configuration:**
```bash
JAVA_VERSION=17
RUST_VERSION=1.75.0
GO_VERSION=1.21
PYTHON_VERSION=3.11
ANDROID_API_LEVEL=34
```

**Build Targets:**
```bash
BUILD_ANDROID=true
BUILD_RUST=true
BUILD_JAVA_AGENTS=true
BUILD_GO_AGENTS=true
BUILD_PYTHON_AGENTS=true
BUILD_WASM_AGENTS=true
RUN_TESTS=true
```

### Secrets Configuration

Required GitHub secrets:
- `SIGNING_KEY` - Android APK signing key
- `ALIAS` - Keystore alias
- `KEY_STORE_PASSWORD` - Keystore password
- `KEY_PASSWORD` - Key password
- `DISCORD_WEBHOOK` - Discord notifications (optional)
- `SEMGREP_APP_TOKEN` - Semgrep scanning (optional)

## 📊 Monitoring & Notifications

### Build Status
- Real-time build status on GitHub
- Artifact generation and storage
- Test result reporting

### Security Monitoring
- Weekly security scans
- Immediate alerts for high-severity issues
- License compliance tracking

### Discord Integration
- Build success/failure notifications
- Nightly build announcements
- Security alert forwarding

## 🐛 Troubleshooting

### Common Issues

**Build Failures:**
1. Check language version compatibility
2. Verify all dependencies are installed
3. Clear caches and retry build

**Test Failures:**
1. Review test logs in CI artifacts
2. Run tests locally to reproduce
3. Check for environment-specific issues

**Security Alerts:**
1. Review vulnerability details
2. Update affected dependencies
3. Re-run security scan to verify fix

### Debug Commands

```bash
# Full diagnostic build
./build-multi-lang.sh --verbose

# Check environment setup
./dev-setup.sh --verify

# Individual component testing
cd rust-core && cargo test --verbose
./gradlew testDebugUnitTest --info
```

## 📈 Performance Optimization

### Build Speed
- Gradle daemon and parallel builds
- Cargo incremental compilation
- Dependency caching across runs
- Matrix job parallelization

### Resource Usage
- Efficient memory allocation
- Optimized Docker images
- Strategic artifact retention

## 🔮 Future Enhancements

### Planned Features
- Cross-platform builds (iOS, desktop)
- Performance regression testing
- Automated benchmarking
- Enhanced monitoring and metrics

### Scalability
- Build farm integration
- Distributed testing
- Advanced caching strategies
- Multi-region deployments

## 📚 Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android CI/CD Best Practices](https://developer.android.com/studio/build)
- [Rust CI/CD Guide](https://doc.rust-lang.org/cargo/guide/continuous-integration.html)
- [Multi-Language Project Management](https://github.com/github/super-linter)

---

**Last Updated:** October 2025  
**Pipeline Version:** 1.0  
**Supported Languages:** Kotlin, Rust, Java, Go, Python, WASM