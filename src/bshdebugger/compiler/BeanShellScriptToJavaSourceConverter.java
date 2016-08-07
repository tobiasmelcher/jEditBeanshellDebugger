package bshdebugger.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.swing.JOptionPane;

import org.gjt.sp.jedit.ActionSet;
import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.Macros.Macro;
import org.gjt.sp.jedit.PluginJAR;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.bsh.Token;
import org.gjt.sp.jedit.gui.DefaultInputHandler;
import org.gjt.sp.jedit.gui.TextAreaDialog;
import org.gjt.sp.jedit.input.AbstractInputHandler;
import org.gjt.sp.jedit.textarea.JEditTextArea;

import bshdebugger.options.OptionPane;

public class BeanShellScriptToJavaSourceConverter {

	private static String beanshellToJava(JEditTextArea area, String className) {
		try {
			String source = area.getText();
			List<Object> nodes = BeanshellSourceUtils.parseBeanshell(source);
			StringBuilder importSource = new StringBuilder(
					"import org.gjt.sp.jedit.jEdit;\nimport org.gjt.sp.jedit.*;\nimport org.gjt.sp.jedit.buffer.*;\nimport org.gjt.sp.jedit.textarea.*;\n");
			StringBuilder mainMethodSource = new StringBuilder();
			StringBuilder allMethodImpls = new StringBuilder();
			for (Object node : nodes) {
				Field f = getTokenField("firstToken", node.getClass());
				Token first = (Token) f.get(node);
				int startLine = first.beginLine;
				int startColumn = first.beginColumn;
				f = getTokenField("lastToken", node.getClass());
				Token last = (Token) f.get(node);
				int endLine = last.endLine;
				int endColumn = last.endColumn;
				int startOffset = area.getLineStartOffset(startLine - 1) + startColumn - 1;
				int endOffset = area.getLineStartOffset(endLine - 1) + endColumn;
				String sourceForNode = area.getText(startOffset, endOffset - startOffset);
				String simpleName = node.getClass().getSimpleName();
				if ("BSHClassDeclaration".equals(simpleName)) {
					sourceForNode = addLineNumbersAtEndOfLine(sourceForNode, startLine);
					allMethodImpls.append(sourceForNode).append("\n");
					continue;
				} else if ("BSHTypedVariableDeclaration".equals(simpleName) || "BSHPrimaryExpression".equals(simpleName)
						|| "BSHAssignment".equals(simpleName)) {
					sourceForNode += ";";
				}
				if ("BSHImportDeclaration".equals(simpleName)) {// if import add
					importSource.append(sourceForNode).append("\n");
				} else if ("BSHMethodDeclaration".equals(simpleName)) { // if
																		// method
					// simply
					// take over
					// as
					// separate
					// method
					sourceForNode = addLineNumbersAtEndOfLine(sourceForNode, startLine);
					allMethodImpls.append(sourceForNode).append("\n");
				} else { // rest - put to main
					sourceForNode = addLineNumbersAtEndOfLine(sourceForNode, startLine);
					mainMethodSource.append(sourceForNode).append("\n");
				}
			}
			StringBuilder javaSource = new StringBuilder();
			javaSource.append(importSource.toString()).append("\n");
			javaSource.append("public class " + className
					+ " {\norg.gjt.sp.jedit.Buffer buffer=(Buffer)jEdit.getActiveView().getTextArea().getBuffer();\norg.gjt.sp.jedit.View view=jEdit.getActiveView();\norg.gjt.sp.jedit.textarea.JEditTextArea textArea=jEdit.getActiveView().getTextArea();\n");
			javaSource.append(allMethodImpls.toString()).append("\n");
			javaSource.append("public void m1() throws Exception {\n");
			javaSource.append(mainMethodSource.toString()).append("\n");
			javaSource.append("}\n}\n");
			return javaSource.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private static Field getTokenField(String fieldName, Class<?> clazz) {
		try {
			Field f = clazz.getDeclaredField(fieldName);
			if (f != null) {
				f.setAccessible(true);
				return f;
			}
		} catch (NoSuchFieldException | SecurityException e) {
		}
		Class<?> sc = clazz.getSuperclass();
		if (sc != null) {
			return getTokenField(fieldName, sc);
		}
		return null;
	}

	private static String addLineNumbersAtEndOfLine(String source, int startLine) {
		StringBuffer result = new StringBuffer(source);
		int numberOfLines = source.length() - source.replace("\n", "").length();
		if (numberOfLines == 0) {
			result.append(" //" + startLine);
			return result.toString();
		}
		result.append(" //" + (startLine + numberOfLines));
		numberOfLines--;
		int i = source.length();
		while (true) {
			i = result.lastIndexOf("\n", i);
			if (i < 0)
				break;
			result.insert(i, " //" + (startLine + numberOfLines));
			numberOfLines--;
			i--;
		}
		return result.toString();
	}

	private static String compileJava(String path) {
		try {
			File folder = getTempBshDebuggerFolder();
			// get jar files
			StringBuilder cp = new StringBuilder();
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			URL[] urls = ((URLClassLoader) cl).getURLs();
			boolean first = true;
			for (URL url : urls) {
				if (first == false) {
					cp.append(";");
				}
				cp.append(url.getFile());
				first = false;
			}
			PluginJAR[] pluginJARs = jEdit.getPluginJARs();
			if (pluginJARs != null) {
				for (PluginJAR jar : pluginJARs) {
					String p = jar.getPath();
					cp.append(";").append(p);
				}
			}
			String javacLocation = OptionPane.getJavacLocation();
			if (javacLocation == null || javacLocation.length() == 0 || new File(javacLocation).exists() == false) {
				JOptionPane.showMessageDialog(jEdit.getActiveView(),
						"Location to javac compiler not configured in options.");
				return "";
			}
			String res = CmdUtil.runCommand(
					Arrays.asList(new String[] { javacLocation, "-classpath", cp.toString(), path }), folder);
			return res;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static File getTempBshDebuggerFolder() {
		String dir = System.getProperty("java.io.tmpdir");
		File f = new File(dir + "/bshdebugger");
		if (f.exists() == false) {
			f.mkdir();
		}
		return f;
	}

	public static String getCompleteMacroName(String path) {
		String lower = path.toLowerCase(Locale.ENGLISH);
		int idx = lower.indexOf("macros");
		if (idx < 0) {
			return null;
		}
		String result = path.substring(idx + "macros".length() + 1);
		result = result.replace("\\", "_");
		result = result.replace("/", "_");
		idx = result.lastIndexOf(".");
		if (idx > 0) { // remove file extension
			result = result.substring(0, idx);
		}
		return result;
	}

	public static void beanShellToJavaAndCompile(JEditTextArea area) {
		Buffer buffer = (Buffer) area.getBuffer();
		String path = buffer.getPath();
		String macroName = getCompleteMacroName(path);
		if (macroName == null)
			return;
		String source = beanshellToJava(area, macroName);
		File folder = getTempBshDebuggerFolder();
		String fileName = folder.getAbsolutePath() + "/" + macroName + ".java";
		try {
			PrintWriter pw = new PrintWriter(new File(fileName));
			pw.write(source);
			pw.flush();
			pw.close();
			long before = System.currentTimeMillis();
			String output = compileJava(fileName);
			if (output != null && output.length() > 0) {
				File classFileName = new File(folder.getAbsolutePath() + "/" + macroName + ".class");
				View view = jEdit.getActiveView();
				if (classFileName.exists() && classFileName.lastModified() > before) {
					new TextAreaDialog(view, "Compilation Result", "Compilation succesful!", null, output);
					replaceAction(path, macroName, classFileName);
				} else {
					jEdit.openFile(view, fileName);
					new TextAreaDialog(view, "Compilation Result", "Compilation Result", null, output);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static void replaceAction(final String macroFilePath, final String macroName, final File classFileName) {
		try {
			DefaultInputHandler dh = (DefaultInputHandler) jEdit.getActiveView().getInputHandler();
			Field f = AbstractInputHandler.class.getDeclaredField("currentBindings");
			f.setAccessible(true);
			Hashtable<?,?> bindings = (Hashtable<?,?>) f.get(dh);
			for (Object key : bindings.keySet()) {
				Object value = bindings.get(key);
				if (value instanceof String) {
					String v = (String) value;
					int idx = v.indexOf("°");
					if (idx > 0) {
						v = v.substring(0, idx);
					}
					final String originMacroName = v;
					v = v.replace("\\", "_");
					v = v.replace("/", "_");
					if (v.equals(macroName)) {
						ActionSet set = jEdit.getActionContext().getActionSetForAction(originMacroName);
						EditAction action = set.getAction(originMacroName);
						if (action instanceof Macro) {
							final EditAction originAction = action;
							set.removeAction(originMacroName);
							set.addAction(new EditAction(originMacroName) {

								@Override
								public void invoke(View view) {
									long bshModified = new File(macroFilePath).lastModified();
									long classModified = classFileName.lastModified();
									if (classModified >= bshModified) {
										String classFolder = getTempBshDebuggerFolder().getAbsolutePath();
										classFolder = classFolder.replace("\\", "\\\\");
										StringBuilder source = new StringBuilder();
										source.append("if (getClass(\"" + macroName + "\")==null) {\n");
										source.append("	addClassPath(\"" + classFolder + "\");\n");
										source.append("	reloadClasses(\"" + macroName + "\");\n");
										source.append("}\n");
										source.append("new " + macroName + "().m1();");
										BeanShell.eval(jEdit.getActiveView(), BeanShell.getNameSpace(),
												source.toString());
									} else {
										originAction.invoke(jEdit.getActiveView());
									}
								}
							});
						}
						return;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
