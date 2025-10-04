import sys
import json
import subprocess
import time

def main():
	for line in sys.stdin:
		try:
			task = json.loads(line)
			cmd = [task.get('command', '')] + task.get('args', [])
			start = time.time()
			proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
			out, err = proc.communicate()
			exec_time = int((time.time() - start) * 1000)
			result = {
				'task_id': task.get('id', ''),
				'success': proc.returncode == 0,
				'output': out.decode().strip(),
				'error': err.decode().strip() if proc.returncode != 0 else '',
				'execution_time_ms': exec_time
			}
		except Exception as e:
			result = {'success': False, 'error': str(e)}
		print(json.dumps(result))
		sys.stdout.flush()

if __name__ == '__main__':
	main()
