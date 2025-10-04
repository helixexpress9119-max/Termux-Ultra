package com.example.termuxultra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
                ) { BifrostDemoUI() }
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
