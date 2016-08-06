package bshdebugger.compiler;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.textarea.JEditTextArea;

public class CompileBeanshell {

	public static void run() {
		JEditTextArea area = jEdit.getActiveView().getTextArea();
		BeanShellScriptToJavaSourceConverter.beanShellToJavaAndCompile(area);
	}
}
