package bshdebugger;

import org.gjt.sp.jedit.bsh.CallStack;
import org.gjt.sp.jedit.bsh.Interpreter;
import org.gjt.sp.jedit.bsh.SimpleNode;

public interface IBeanShellDebugger {
	public void trace(String sourceFileInfo, SimpleNode node, Interpreter localInterpreter, CallStack callstack);
}
