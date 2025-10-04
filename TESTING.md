# Testing Termux-Ultra

This document describes the testing strategy and procedures for Termux-Ultra.

## Test Structure

### Unit Tests (`app/src/test/`)
- **BifrostEngineTest**: Tests for the Rust engine integration
- **AgentCommunicationTest**: Tests for inter-agent communication protocols
- **UtilityTest**: Tests for utility functions and command parsing

### Instrumented Tests (`app/src/androidTest/`)
- **TerminalCoreTest**: Tests for terminal functionality on Android
- **ApiServiceTest**: Tests for system API integration
- **IntegrationTest**: End-to-end integration tests

### Rust Tests (`rust-core/src/`)
```bash
cd rust-core
cargo test
```

### Agent Tests
Each agent can be tested independently:

#### Python Agent
```bash
echo '{"id":"test","command":"python3","args":["-c","print(\"hello\")"]}' | python agents/python/agent.py
```

#### Go Agent
```bash
echo '{"id":"test","command":"echo","args":["hello"]}' | go run agents/go/agent.go
```

#### Java Agent
```bash
echo '{"id":"test","command":"echo","args":["hello"]}' | java agents/java/Agent.java
```

## Running Tests

### Android Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# Generate test report
./gradlew testDebugUnitTest --continue
```

### Rust Tests
```bash
cd rust-core
cargo test --all-features
cargo test --release
```

### Integration Testing
```bash
# Build and test everything
./gradlew clean build test connectedAndroidTest
```

## Test Coverage

The test suite covers:
- ✅ Terminal session management
- ✅ Command execution
- ✅ Agent communication protocols
- ✅ System API functionality
- ✅ JSON serialization/deserialization
- ✅ Error handling
- ✅ Memory management
- ✅ File operations

## Continuous Integration

The project includes GitHub Actions CI configuration in `.github/workflows/ci.yml`:

```yaml
name: Termux-Ultra CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: "21", distribution: "temurin" }
      - uses: dtolnay/rust-toolchain@stable
      - uses: android-actions/setup-android@v3
      - run: cd rust-core && cargo build --release --target aarch64-linux-android
      - run: ./gradlew assembleDebug
      - run: ./gradlew test
```

## Performance Testing

### Benchmarks
- Agent response time: < 100ms for simple commands
- Memory usage: < 50MB baseline
- Battery impact: Minimal when idle
- Storage footprint: < 200MB including models

### Load Testing
- Multiple concurrent terminal sessions
- High-frequency command execution
- Large file operations
- Extended AI inference sessions

## Manual Testing Checklist

### Basic Functionality
- [ ] App launches successfully
- [ ] Terminal displays correctly
- [ ] Commands execute and show output
- [ ] Session switching works
- [ ] Settings are persistent

### AI Features
- [ ] Llama engine loads models
- [ ] MLC engine performs inference
- [ ] AI responses are coherent
- [ ] Model switching works
- [ ] Memory usage is reasonable

### Agent System
- [ ] Python agent executes code
- [ ] Go agent handles commands
- [ ] Java agent processes tasks
- [ ] Inter-agent communication works
- [ ] Error handling is graceful

### System Integration
- [ ] Battery monitoring works
- [ ] Process listing is accurate
- [ ] Package information is correct
- [ ] Network interfaces are detected
- [ ] File operations succeed

## Debugging

### Logging
```bash
# View Android logs
adb logcat | grep -E "(BifrostJNI|TerminalCore|LlamaJNI|MLC4J)"

# Rust debugging
export RUST_LOG=debug
export RUST_BACKTRACE=1
```

### Common Issues
1. **Native library loading failures**
   - Check ABI compatibility
   - Verify library paths
   - Ensure NDK version compatibility

2. **Agent communication timeouts**
   - Check JSON format
   - Verify agent binaries
   - Monitor process execution

3. **AI model loading errors**
   - Verify model file integrity
   - Check available memory
   - Validate model format

### Performance Profiling
```bash
# Profile Android app
./gradlew :app:assembleDebug
adb shell am start -n com.example.termuxultra/.MainActivity
adb shell dumpsys meminfo com.example.termuxultra

# Profile Rust components
cd rust-core
cargo bench
```

## Test Data

### Sample Commands
```bash
ls -la
cd /sdcard
cat /proc/version
ps aux
echo "Hello World"
python3 -c "print('AI test')"
```

### Sample Agent Tasks
```json
{
  "id": "python-test",
  "agent_type": "python",
  "command": "import sys; print(sys.version)",
  "args": [],
  "environment": {}
}
```

### Expected Outputs
- Command execution should return JSON with success status
- Agent responses should include timing information
- Error cases should be handled gracefully
- System information should be current and accurate