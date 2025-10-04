(module
	(import "env" "cmd_ptr" (global $cmd_ptr (mut i32)))
	(import "env" "cmd_len" (global $cmd_len (mut i32)))
	(memory (export "memory") 1)
	(func (export "_start")
		;; For demonstration, just return
	)
)
