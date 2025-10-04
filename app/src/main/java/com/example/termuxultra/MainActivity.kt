package com.example.termuxultra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.termuxultra.agent.ChatAgent
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            System.loadLibrary("bifrost")
            initBifrost()
        } catch (e: UnsatisfiedLinkError) {
            // Handle library loading error gracefully
        }
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) { 
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    ) {
                        BifrostDemoUI()
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                        ChatAgentUI()
                    }
                }
            }
        }
    }
    
    // Native function declarations
    external fun bifrostHello(input: String): String
    external fun bifrostRunPython(code: String): String
    external fun bifrostInfer(prompt: String): String
    external fun initBifrost(): Boolean
    
    companion object {
        init {
            try {
                System.loadLibrary("bifrost")
            } catch (e: UnsatisfiedLinkError) {
                // Library not available - app will use fallback mode
            }
        }
    }
}

@Composable
fun BifrostDemoUI() {
    var result by remember { mutableStateOf("Press button to call Rust") }
    var isLoading by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { 
                isLoading = true
                try {
                    result = MainActivity().bifrostHello("Nomad")
                } catch (e: Exception) {
                    result = "Error: ${e.message}"
                }
                isLoading = false
            },
            enabled = !isLoading
        ) {
            Text("Call Rust Hello")
        }
        
        Spacer(Modifier.height(12.dp))
        
        Button(
            onClick = { 
                isLoading = true
                try {
                    result = MainActivity().bifrostRunPython("print('Hello from Python agent')")
                } catch (e: Exception) {
                    result = "Error: ${e.message}"
                }
                isLoading = false
            },
            enabled = !isLoading
        ) {
            Text("Run Python Code")
        }
        
        Spacer(Modifier.height(12.dp))
        
        Button(
            onClick = { 
                isLoading = true
                try {
                    result = MainActivity().bifrostInfer("What is Termux-Ultra?")
                } catch (e: Exception) {
                    result = "Error: ${e.message}"
                }
                isLoading = false
            },
            enabled = !isLoading
        ) {
            Text("AI Inference")
        }
        
        Spacer(Modifier.height(20.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Text(
                text = result,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ChatAgentUI() {
    val context = LocalContext.current
    val agent = remember { ChatAgent(context) }
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Chat Agent",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        output = agent.toggleAIMode()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("AI", style = MaterialTheme.typography.bodySmall)
            }
            
            Button(
                onClick = {
                    scope.launch {
                        output = agent.getStatus()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Status", style = MaterialTheme.typography.bodySmall)
            }
            
            Button(
                onClick = {
                    scope.launch {
                        output = agent.getHistory()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("History", style = MaterialTheme.typography.bodySmall)
            }
            
            Button(
                onClick = {
                    scope.launch {
                        output = agent.clearHistory()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Clear", style = MaterialTheme.typography.bodySmall)
            }
        }
        
        TextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Talk to Termux-Ultra") },
            placeholder = { Text("Try: 'list files', 'build app', 'help'") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        Button(
            onClick = {
                isLoading = true
                scope.launch {
                    try {
                        output = agent.handleInput(input)
                        input = "" // Clear input after successful execution
                    } catch (e: Exception) {
                        output = "Error: ${e.message}"
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading && input.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send")
        }
        
        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Processing...", style = MaterialTheme.typography.bodySmall)
            }
        }
        
        if (output.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = output,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
