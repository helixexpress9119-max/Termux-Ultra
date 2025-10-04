# Termux-Ultra

An advanced terminal emulator for Android with integrated AI capabilities, multi-language agent system, and powerful automation features.

## 🚀 Features

### Core Components
- **Bifröst Engine**: Multi-language execution engine written in Rust supporting Python, Go, Java, and WebAssembly agents
- **AI Integration**: Built-in support for Llama and MLC-LLM inference engines
- **Advanced Terminal**: Full-featured terminal emulator with session management and command execution
- **System API**: Comprehensive system monitoring and package management capabilities
- **Cross-Platform Agents**: Execute code in multiple languages with inter-agent communication

### AI Capabilities
- **Local LLM Inference**: Run language models directly on device using Llama.cpp and MLC-LLM
- **Code Generation**: AI-powered code completion and generation
- **Natural Language Commands**: Convert natural language to terminal commands
- **Smart Automation**: AI-driven task automation and scripting

### Multi-Agent System
- **Python Agent**: Execute Python scripts with full standard library support
- **Go Agent**: High-performance Go code execution for system tasks
- **Java Agent**: JVM-based agent for enterprise integration
- **WASM Agent**: WebAssembly execution for portable cross-platform code
- **Agent Communication**: JSON-based inter-agent messaging and task orchestration

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Android Application                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐│
│  │   Compose UI    │  │  Terminal Core  │  │  API Service ││
│  └─────────────────┘  └─────────────────┘  └──────────────┘│
└─────────────────────────────┬───────────────────────────────┘
                             │ JNI
┌─────────────────────────────┴───────────────────────────────┐
│                    Bifröst Engine (Rust)                   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐│
│  │ Agent Manager   │  │  Python Runtime │  │ WASM Runtime ││
│  └─────────────────┘  └─────────────────┘  └──────────────┘│
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐│
│  │  Llama Engine   │  │   MLC Engine    │  │ Task Queue   ││
│  └─────────────────┘  └─────────────────┘  └──────────────┘│
└─────────────────────────────────────────────────────────────┘
                             │
┌─────────────────────────────┴───────────────────────────────┐
│                    External Agents                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐│
│  │   Go Agent      │  │   Java Agent    │  │  Python Env  ││
│  └─────────────────┘  └─────────────────┘  └──────────────┘│
└─────────────────────────────────────────────────────────────┘
```

## 🛠️ Building

### Prerequisites
- Android Studio Arctic Fox or later
- Android NDK 25+
- Rust toolchain with Android targets
- Java 17+
- Go 1.21+ (for agent development)

### Build Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/helixexpress9119-max/Termux-Ultra.git
   cd Termux-Ultra
   ```

2. **Install Rust Android targets**
   ```bash
   rustup target add aarch64-linux-android armv7-linux-androideabi
   ```

3. **Build Rust components**
   ```bash
   cd rust-core
   ./build-android.sh
   cd ..
   ```

4. **Build Android app**
   ```bash
   ./gradlew assembleDebug
   ```

### Development Setup

For development, you can use the provided container environment:
```bash
# Build in dev container
docker build -t termux-ultra-dev .
docker run -v $(pwd):/workspace termux-ultra-dev
```

## 📱 Usage

### Basic Terminal Operations

The terminal supports all standard commands plus enhanced features:

```bash
# Standard commands
ls -la
cd /sdcard
pwd
cat file.txt

# Built-in commands
help                    # Show available commands
clear                   # Clear terminal
history                 # Show command history
env                     # Show environment variables
export VAR=value        # Set environment variable

# Session management
sessions                # List active sessions
switch <session_id>     # Switch to different session
kill <session_id>       # Terminate session
```

### Agent System

Execute code in different languages:

```bash
# Python agent
python-agent '{"id":"task1","command":"print(\"Hello from Python\")"}'

# Go agent
go-agent '{"id":"task2","command":"echo","args":["Hello from Go"]}'

# Java agent
java-agent '{"id":"task3","command":"java","args":["-version"]}'
```

### AI Integration

Use built-in AI capabilities:

```bash
# Llama inference
llama-infer "Explain what is machine learning"

# MLC chat
mlc-chat "Write a Python function to calculate fibonacci"

# AI command generation
ai-cmd "list all files larger than 1MB"
```

### System Monitoring

Access comprehensive system information:

```bash
# Battery status
battery-status

# System information
system-info

# Process list
ps-extended

# Package information
pkg-list
```

## 🔧 Configuration

### Environment Variables

```bash
# Bifröst configuration
export BIFROST_LOG_LEVEL=info
export BIFROST_AGENT_TIMEOUT=30
export BIFROST_MAX_CONCURRENT_TASKS=10

# AI model paths
export LLAMA_MODEL_PATH=/sdcard/models/llama-7b.gguf
export MLC_MODEL_PATH=/sdcard/models/phi-3-mini

# Terminal settings
export TERM=xterm-256color
export SHELL=/data/data/com.example.termuxultra/files/usr/bin/bash
```

### AI Model Setup

1. **Download models** (examples):
   ```bash
   # Llama models (GGUF format)
   wget https://huggingface.co/models/llama-3.2-3b-instruct.gguf
   
   # MLC models
   git clone https://huggingface.co/mlc-ai/Phi-3.5-mini-instruct-q4f16_1-MLC
   ```

2. **Place in models directory**:
   ```bash
   mkdir -p /sdcard/termux-ultra/models
   mv *.gguf /sdcard/termux-ultra/models/
   ```

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew connectedAndroidTest
```

### Rust Tests
```bash
cd rust-core
cargo test
```

### Agent Tests
```bash
# Test Python agent
echo '{"id":"test","command":"print(\"test\")","args":[]}' | python agents/python/agent.py

# Test Go agent
echo '{"id":"test","command":"echo","args":["test"]}' | go run agents/go/agent.go

# Test Java agent
echo '{"id":"test","command":"echo","args":["test"]}' | java agents/java/Agent.java
```

## 📋 API Reference

### Bifröst Engine

```kotlin
// Initialize engine
BifrostEngine.initialize()

// Execute task
val task = AgentTask(
    id = "unique-id",
    agentType = "python",
    command = "print('Hello')",
    args = listOf(),
    environment = mapOf()
)
val result = BifrostEngine.executeTask(task)
```

### Terminal Core

```kotlin
// Create session
val session = TerminalCore.createSession("session-1")

// Execute command
val result = TerminalCore.runCommand("ls -la")

// Monitor output
TerminalCore.terminalOutput.collect { output ->
    println(output)
}
```

### AI Engines

```kotlin
// Llama inference
val result = LlamaEngine.infer(
    InferenceRequest(
        prompt = "Hello, how are you?",
        maxTokens = 512,
        temperature = 0.7f
    )
)

// MLC chat
val response = MLCEngine.sendMessage("What is the weather?")
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Kotlin coding conventions
- Write tests for new features
- Update documentation
- Ensure cross-platform compatibility
- Test on multiple Android versions

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Termux](https://termux.dev/) - Original terminal emulator inspiration
- [Llama.cpp](https://github.com/ggerganov/llama.cpp) - High-performance LLM inference
- [MLC-LLM](https://mlc.ai/) - Mobile-optimized language models
- [Rust Android Gradle Plugin](https://github.com/mozilla/rust-android-gradle) - Rust integration
- [Android NDK](https://developer.android.com/ndk) - Native development toolkit

## 🐛 Known Issues

- WebAssembly agent requires additional runtime setup
- Some AI models may require significant storage space
- Performance varies by device capabilities
- Network-dependent features require internet connectivity

## 🗺️ Roadmap

- [ ] WebAssembly runtime integration
- [ ] Cloud model synchronization
- [ ] Plugin system for custom agents
- [ ] Enhanced UI themes and customization
- [ ] Termux package compatibility layer
- [ ] Real-time collaboration features
- [ ] Advanced debugging tools
- [ ] Performance profiling integration

---

**Termux-Ultra** - Empowering mobile development with AI-enhanced terminal capabilities.
