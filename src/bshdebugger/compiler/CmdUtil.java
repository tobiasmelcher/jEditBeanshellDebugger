package bshdebugger.compiler;

import java.io.*;
import java.util.*;

public class CmdUtil {

	public static String runCommand(List<String> commandList, File workingDir) throws Exception {
		ProcessBuilder builder = null;
		StringBuffer result = new StringBuffer();
		// the current executable
		builder = new ProcessBuilder(commandList);
		builder.directory(workingDir);
		Process p = builder.start();
		java.io.InputStream is = p.getInputStream();
		if (is.available() > 0) {
			java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(is));
			// And print each line
			String s = null;
			while ((s = reader.readLine()) != null) {
				result.append(s).append("\r\n"); //$NON-NLS-1$
			}
			is.close();
		}
		BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(p.getErrorStream()));
		// And print each line
		String s = null;
		while ((s = reader.readLine()) != null) {
			result.append(s).append("\r\n"); //$NON-NLS-1$
		}
		is.close();
		return result.toString();
	}
}