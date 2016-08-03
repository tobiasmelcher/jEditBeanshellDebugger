package bshdebugger;

import org.gjt.sp.jedit.bsh.CallStack;
import org.gjt.sp.jedit.bsh.Interpreter;
import org.gjt.sp.jedit.bsh.SimpleNode;

public class BshDebugCallback {
	public static IBeanShellDebugger debugger;

	public static void trace(String sourceFileInfo, SimpleNode node, Interpreter localInterpreter,
			CallStack callstack) {
		if (debugger != null) {
			debugger.trace(sourceFileInfo, node, localInterpreter, callstack);
		}
	}
}
