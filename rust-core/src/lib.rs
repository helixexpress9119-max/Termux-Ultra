use std::ffi::{CStr, CString};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use libc::c_char;
use pyo3::prelude::*;
use pyo3::types::PyDict;
use wasmtime::*;
use serde::{Deserialize, Serialize};
use serde_json;

// Agent system types
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AgentTask {
    pub id: String,
    pub agent_type: String,
    pub command: String,
    pub args: Vec<String>,
    pub environment: HashMap<String, String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AgentResult {
    pub task_id: String,
    pub success: bool,
    pub output: String,
    pub error: Option<String>,
    pub execution_time_ms: u64,
}

// Global agent registry
lazy_static::lazy_static! {
    static ref AGENT_REGISTRY: Arc<Mutex<HashMap<String, Box<dyn Agent + Send + Sync>>>> = 
        Arc::new(Mutex::new(HashMap::new()));
}

// Agent trait for different execution engines
pub trait Agent: Send + Sync {
    fn execute(&self, task: &AgentTask) -> AgentResult;
    fn supports(&self, agent_type: &str) -> bool;
}

// Python agent implementation
pub struct PythonAgent {
    interpreter: Arc<Mutex<Python>>,
}

impl PythonAgent {
    pub fn new() -> PyResult<Self> {
        Python::with_gil(|py| {
            Ok(PythonAgent {
                interpreter: Arc::new(Mutex::new(py.clone())),
            })
        })
    }
}

impl Agent for PythonAgent {
    fn execute(&self, task: &AgentTask) -> AgentResult {
        let start_time = std::time::Instant::now();
        
        let result = Python::with_gil(|py| {
            let globals = PyDict::new(py);
            
            // Set up environment variables
            for (key, value) in &task.environment {
                globals.set_item(key, value).ok();
            }
            
            // Execute Python code
            match py.run(&task.command, Some(globals), None) {
                Ok(_) => {
                    let output = globals.get_item("__result__")
                        .and_then(|item| item.map(|i| i.to_string()))
                        .unwrap_or_else(|| "Python code executed successfully".to_string());
                    
                    AgentResult {
                        task_id: task.id.clone(),
                        success: true,
                        output,
                        error: None,
                        execution_time_ms: start_time.elapsed().as_millis() as u64,
                    }
                }
                Err(e) => AgentResult {
                    task_id: task.id.clone(),
                    success: false,
                    output: String::new(),
                    error: Some(format!("Python error: {}", e)),
                    execution_time_ms: start_time.elapsed().as_millis() as u64,
                }
            }
        });
        
        result
    }
    
    fn supports(&self, agent_type: &str) -> bool {
        agent_type == "python"
    }
}

// WASM agent implementation
pub struct WasmAgent {
    engine: Engine,
}

impl WasmAgent {
    pub fn new() -> Self {
        WasmAgent {
            engine: Engine::default(),
        }
    }
}

impl Agent for WasmAgent {
    fn execute(&self, task: &AgentTask) -> AgentResult {
        let start_time = std::time::Instant::now();
        
        // For now, just acknowledge WASM execution
        // In a full implementation, this would compile and run WASM modules
        AgentResult {
            task_id: task.id.clone(),
            success: true,
            output: format!("WASM agent executed: {}", task.command),
            error: None,
            execution_time_ms: start_time.elapsed().as_millis() as u64,
        }
    }
    
    fn supports(&self, agent_type: &str) -> bool {
        agent_type == "wasm"
    }
}

// System command agent for shell/system commands
pub struct SystemAgent;

impl Agent for SystemAgent {
    fn execute(&self, task: &AgentTask) -> AgentResult {
        let start_time = std::time::Instant::now();
        
        let output = std::process::Command::new("sh")
            .arg("-c")
            .arg(&task.command)
            .output();
            
        match output {
            Ok(output) => {
                let stdout = String::from_utf8_lossy(&output.stdout);
                let stderr = String::from_utf8_lossy(&output.stderr);
                
                AgentResult {
                    task_id: task.id.clone(),
                    success: output.status.success(),
                    output: stdout.to_string(),
                    error: if stderr.is_empty() { None } else { Some(stderr.to_string()) },
                    execution_time_ms: start_time.elapsed().as_millis() as u64,
                }
            }
            Err(e) => AgentResult {
                task_id: task.id.clone(),
                success: false,
                output: String::new(),
                error: Some(format!("System command error: {}", e)),
                execution_time_ms: start_time.elapsed().as_millis() as u64,
            }
        }
    }
    
    fn supports(&self, agent_type: &str) -> bool {
        agent_type == "system" || agent_type == "shell" || agent_type == "bash"
    }
}

// Initialize the agent system
pub fn init_agent_system() -> Result<(), Box<dyn std::error::Error>> {
    let mut registry = AGENT_REGISTRY.lock().unwrap();
    
    // Register Python agent
    if let Ok(python_agent) = PythonAgent::new() {
        registry.insert("python".to_string(), Box::new(python_agent));
    }
    
    // Register WASM agent
    registry.insert("wasm".to_string(), Box::new(WasmAgent::new()));
    
    // Register system agent
    registry.insert("system".to_string(), Box::new(SystemAgent));
    
    Ok(())
}

// Execute a task using the appropriate agent
pub fn execute_agent_task(task_json: &str) -> String {
    let task: AgentTask = match serde_json::from_str(task_json) {
        Ok(task) => task,
        Err(e) => {
            let error_result = AgentResult {
                task_id: "unknown".to_string(),
                success: false,
                output: String::new(),
                error: Some(format!("Failed to parse task JSON: {}", e)),
                execution_time_ms: 0,
            };
            return serde_json::to_string(&error_result).unwrap_or_else(|_| "{}".to_string());
        }
    };
    
    let registry = AGENT_REGISTRY.lock().unwrap();
    
    for (_, agent) in registry.iter() {
        if agent.supports(&task.agent_type) {
            let result = agent.execute(&task);
            return serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
        }
    }
    
    let error_result = AgentResult {
        task_id: task.id,
        success: false,
        output: String::new(),
        error: Some(format!("No agent found for type: {}", task.agent_type)),
        execution_time_ms: 0,
    };
    
    serde_json::to_string(&error_result).unwrap_or_else(|_| "{}".to_string())
}

// JNI exports for Android
#[no_mangle]
pub extern "C" fn bifrost_hello(input: *const c_char) -> *mut c_char {
    let c_str = unsafe { CStr::from_ptr(input) };
    let msg = c_str.to_str().unwrap_or("invalid utf8");
    let response = format!("Bifröst Agent System initialized. Input: {}", msg);
    CString::new(response).unwrap().into_raw()
}

#[no_mangle]
pub extern "C" fn bifrost_init() -> bool {
    match init_agent_system() {
        Ok(_) => true,
        Err(_) => false,
    }
}

#[no_mangle]
pub extern "C" fn bifrost_execute_task(task_json: *const c_char) -> *mut c_char {
    let c_str = unsafe { CStr::from_ptr(task_json) };
    let task_str = c_str.to_str().unwrap_or("{}");
    let result = execute_agent_task(task_str);
    CString::new(result).unwrap().into_raw()
}

#[no_mangle]
pub extern "C" fn bifrost_run_python(code: *const c_char) -> *mut c_char {
    let c_str = unsafe { CStr::from_ptr(code) };
    let code_str = c_str.to_str().unwrap_or("");
    
    let task = AgentTask {
        id: uuid::Uuid::new_v4().to_string(),
        agent_type: "python".to_string(),
        command: code_str.to_string(),
        args: vec![],
        environment: HashMap::new(),
    };
    
    let result = execute_agent_task(&serde_json::to_string(&task).unwrap());
    CString::new(result).unwrap().into_raw()
}

#[no_mangle]
pub extern "C" fn bifrost_free_string(ptr: *mut c_char) {
    if !ptr.is_null() {
        unsafe {
            let _ = CString::from_raw(ptr);
        }
    }
}
