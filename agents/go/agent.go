package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"strings"
	"time"
)

type AgentTask struct {
	ID      string   `json:"id"`
	Command string   `json:"command"`
	Args    []string `json:"args"`
}

type AgentResult struct {
	TaskID     string `json:"task_id"`
	Success    bool   `json:"success"`
	Output     string `json:"output"`
	Error      string `json:"error,omitempty"`
	ExecTimeMs int64  `json:"execution_time_ms"`
}

func main() {
	scanner := bufio.NewScanner(os.Stdin)
	for scanner.Scan() {
		var task AgentTask
		if err := json.Unmarshal(scanner.Bytes(), &task); err != nil {
			fmt.Println(`{"success":false,"error":"Invalid JSON input"}`)
			continue
		}
		start := time.Now()
		cmd := exec.Command(task.Command, task.Args...)
		output, err := cmd.CombinedOutput()
		execTime := time.Since(start).Milliseconds()
		result := AgentResult{
			TaskID:     task.ID,
			Success:    err == nil,
			Output:     strings.TrimSpace(string(output)),
			ExecTimeMs: execTime,
		}
		if err != nil {
			result.Error = err.Error()
		}
		resJSON, _ := json.Marshal(result)
		fmt.Println(string(resJSON))
	}
}
