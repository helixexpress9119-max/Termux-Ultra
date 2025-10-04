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
        System.loadLibrary("bifrost")
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) { BifrostDemoUI() }
            }
        }
    }
    external fun bifrostHello(input: String): String
    external fun bifrostRunPython(code: String): String
    external fun bifrostInfer(prompt: String): String
}

@Composable
fun BifrostDemoUI() {
    var result by remember { mutableStateOf("Press button to call Rust") }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { result = MainActivity().bifrostHello("Nomad") }) {
            Text("Call Rust Hello")
        }
        Spacer(Modifier.height(12.dp))
        Text(result)
    }
}
