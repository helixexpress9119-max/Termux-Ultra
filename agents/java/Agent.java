import java.io.*;
import java.util.*;
import com.google.gson.*;

class AgentTask {
	String id;
	String command;
	List<String> args;
}

class AgentResult {
	String task_id;
	boolean success;
	String output;
	String error;
	long execution_time_ms;
}

public class Agent {
	public static void main(String[] args) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		Gson gson = new Gson();
		String line;
		while ((line = reader.readLine()) != null) {
			AgentResult result = new AgentResult();
			try {
				AgentTask task = gson.fromJson(line, AgentTask.class);
				result.task_id = task.id;
				long start = System.currentTimeMillis();
				List<String> cmd = new ArrayList<>();
				cmd.add(task.command);
				if (task.args != null) cmd.addAll(task.args);
				ProcessBuilder pb = new ProcessBuilder(cmd);
				Process proc = pb.start();
				String output = new String(proc.getInputStream().readAllBytes());
				String error = new String(proc.getErrorStream().readAllBytes());
				int exit = proc.waitFor();
				result.success = exit == 0;
				result.output = output.trim();
				result.error = exit == 0 ? "" : error.trim();
				result.execution_time_ms = System.currentTimeMillis() - start;
			} catch (Exception e) {
				result.success = false;
				result.error = e.getMessage();
			}
			System.out.println(gson.toJson(result));
			System.out.flush();
		}
	}
}
