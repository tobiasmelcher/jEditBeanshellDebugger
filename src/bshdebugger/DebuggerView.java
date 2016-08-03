package bshdebugger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditAction;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.bsh.CallStack;
import org.gjt.sp.jedit.bsh.EvalError;
import org.gjt.sp.jedit.bsh.Interpreter;
import org.gjt.sp.jedit.bsh.SimpleNode;
import org.gjt.sp.jedit.bsh.Token;
import org.gjt.sp.jedit.bsh.UtilEvalError;
import org.gjt.sp.jedit.textarea.Gutter;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.TextAreaExtension;

import bshdebugger.options.OptionPane;

public class DebuggerView extends JPanel {

	private static final long serialVersionUID = 1L;
	private static DefaultTableModel tableModel;

	public DebuggerView(View v) {
		setLayout(new BorderLayout());
		if (OptionPane.isBshDebuggerEnabled()==false) {
			JOptionPane.showMessageDialog(v, "Beanshell Debugger not enabled. Enable it in plugin options dialog and restart jEdit.");
			return;
		}
		// TODO: step over/step into/step return
		JButton step = new JButton("step");
		step.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				doStep();
			}
		});
		jEdit.getInputHandler().addKeyBinding("F6", new EditAction("bshdebugger.step") {
			@Override
			public void invoke(View view) {
				doStep();
			}
		});
		JButton cont = new JButton("continue");
		cont.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				doContinue();
			}
		});
		jEdit.getInputHandler().addKeyBinding("F8", new EditAction("bshdebugger.continue") {
			@Override
			public void invoke(View view) {
				doContinue();
			}
		});
		tableModel = new DefaultTableModel();
		tableModel.addColumn("Key");
		tableModel.addColumn("Value");
		tableModel.addRow(new Object[] { "", "" });
		JTable tab = new JTable(tableModel);
		tab.setShowGrid(true);
		tableModel.addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {
				if (e.getColumn() == 0 && lastInterpreter != null && lastCallStack != null) {
					try {
						int row = e.getFirstRow();
						Object expr = tableModel.getValueAt(row, e.getColumn());
						if (expr instanceof String && ((String) expr).length() == 0) {
							tableModel.removeRow(e.getFirstRow());
							return;
						}
						Object res = evalOnCurrentStack(lastInterpreter, lastCallStack, (String) expr);
						if (res != null) {
							tableModel.setValueAt(res, row, 1);
						}
						// add additional rows if necessary
						if (row == tableModel.getRowCount() - 1) {
							tableModel.addRow(new Object[] { "", "" });
						}
					} catch (EvalError | UtilEvalError e1) {
						e1.printStackTrace();
					}
				} else if (e.getColumn() == 0) {
					int row = e.getFirstRow();
					Object expr = tableModel.getValueAt(row, e.getColumn());
					if (expr instanceof String && ((String) expr).length() == 0) {
						tableModel.removeRow(e.getFirstRow());
						return;
					}
					if (row == tableModel.getRowCount() - 1) {
						tableModel.addRow(new Object[] { "", "" });
					}
				}
			}
		});

		JPanel north = new JPanel();
		north.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		// c.gridx = 0;
		// c.gridy = 0;
		// c.gridwidth = 2;
		// north.add(enableDebugger, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		north.add(step, c);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		north.add(cont, c);

		add(north, BorderLayout.NORTH);
		add(new JScrollPane(tab), BorderLayout.CENTER);
	}

	private static void ensureBreapointPainterRegistered(JEditTextArea textArea) {
		Gutter gutter = textArea.getGutter();
		TextAreaExtension[] extensions = gutter.getExtensions();
		for (TextAreaExtension ext : extensions) {
			if (ext instanceof BshBreakpointTextAreaExtension) {
				return;
			}
		}
		gutter.addExtension(new BshBreakpointTextAreaExtension());
	}

	private static class BshBreakpointTextAreaExtension extends TextAreaExtension {
		@Override
		public void paintValidLine(Graphics2D gfx, int screenLine, int physicalLine, int start, int end, int y) {
			View av = jEdit.getActiveView();
			String path = av.getBuffer().getPath();
			Set<Integer> lines = breakpoints.get(path);
			if (lines==null)
				return;
			if (lines.contains(physicalLine+1)) {
				JEditTextArea textArea = av.getTextArea();
				Point p = textArea.offsetToXY(textArea.getLineStartOffset(physicalLine));
				int nextLine = physicalLine + 1;
				if (nextLine < textArea.getLineCount()) {
					Point e = textArea.offsetToXY(textArea.getLineStartOffset(nextLine));
					if (e != null) {
						int restHeight = e.y - p.y - 10;
						if (restHeight > 0)
							p.y = p.y + (restHeight / 2);
					}
				}
				gfx.setColor(Color.blue);
				gfx.fillOval(p.x, p.y, 10, 10);
			}
		}
	}

	private static boolean doWaitForStep = true;
	private static boolean stepping = false;

	private static void enableBeanshellDebugger() {
		if (BshDebugCallback.debugger == null) {
			BshDebugCallback.debugger = new IBeanShellDebugger() {

				@Override
				public void trace(String sourceFileInfo, SimpleNode node, Interpreter localInterpreter,
						CallStack callstack) {
					if (sourceFileInfo == null) {
						return;
					}
					if (new File(sourceFileInfo).exists() == false) {
						return;
					}
					try {
						Token t = getFirstToken(node);
						if (t == null)
							return;
						if (stepping == true || breakpointHit(sourceFileInfo, t.beginLine)) {
							openFile(sourceFileInfo, t.beginLine);
							for (int i = 0; i < tableModel.getRowCount(); i++) {
								Object v = tableModel.getValueAt(i, 0);
								if (v instanceof String) {
									try {
										String expr = (String) v;
										if (expr.length() == 0)
											continue;
										Object res = evalOnCurrentStack(localInterpreter, callstack, (String) v);
										tableModel.setValueAt(res, i, 1);
									} catch (EvalError | UtilEvalError e) {
										e.printStackTrace();
									}
								}
							}
							doWaitForStep = true;
							while (doWaitForStep) {
								readAndDispatch();
							}
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			};
		}
	}

	private static boolean breakpointHit(String sourceFileInfo, int beginLine) {
		Set<Integer> bps = breakpoints.get(sourceFileInfo);
		if (bps!=null) {
			if (bps.contains(beginLine)) {
				return true;
			}
		}
		return false;
	}

	private static Token getFirstToken(SimpleNode node) throws IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = node.getClass();
		while (true) {
			Field[] fields = clazz.getDeclaredFields();
			for (Field f : fields) {
				String n = f.getName();
				if ("firstToken".equals(n)) {
					f.setAccessible(true);
					Token r = (Token) f.get(node);
					return r;
				}
			}
			clazz = clazz.getSuperclass();
			if (clazz == null)
				return null;
		}
	}

	private static Interpreter lastInterpreter;
	private static CallStack lastCallStack;

	private static Object evalOnCurrentStack(Interpreter localInterpreter, CallStack callstack, String evalExpr)
			throws EvalError, UtilEvalError {
		Object evalResult = null;
		if (callstack == null) {
			return "call stack not available";
		} else {
			evalResult = localInterpreter.eval(evalExpr, callstack.top());
			lastInterpreter = localInterpreter;
			lastCallStack = callstack;
		}
		if (evalResult == null && evalExpr.contains(".") == false && callstack != null) {
			// nothing found - search for variable in stack
			for (int i = 0; i < callstack.depth(); i++) {
				Object variable = callstack.get(i).getVariable(evalExpr, true);
				if (variable != null) {
					evalResult = variable;
					break;
				}
			}
		}
		return evalResult;
	}

	private static void openFile(String sourceFileInfo, int line) {
		View view = jEdit.getActiveView();
		jEdit.openFile(view, sourceFileInfo);
		JEditTextArea textArea = view.getTextArea();
		int offset = textArea.getLineStartOffset(line - 1);
		textArea.setCaretPosition(offset);
		textArea.selectLine();
		textArea.scrollToCaret(true);
	}

	private static Thread eventThread;
	private static Method pumpMethod;

	private static void readAndDispatch() {
		try {
			if (eventThread == null) {
				EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
				Field f = EventQueue.class.getDeclaredField("dispatchThread");
				f.setAccessible(true);
				eventThread = (Thread) f.get(queue);
				Method[] declaredMethods = eventThread.getClass().getDeclaredMethods();
				for (Method m : declaredMethods) {
					if ("pumpOneEventForFilters".equals(m.getName())) {
						m.setAccessible(true);
						pumpMethod = m;
						break;
					}
				}
			}
			pumpMethod.invoke(eventThread, -1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void set(View view) {
		view.getDockableWindowManager().showDockableWindow("bshdebugger.debugview");
	}

	static Map<String, Set<Integer>> breakpoints = new HashMap<String, Set<Integer>>();

	public static void toggleBreakpoint(View view) {
		if (OptionPane.isBshDebuggerEnabled()==false) {
			JOptionPane.showMessageDialog(view, "Beanshell Debugger not enabled. Enable it in plugin options dialog and restart jEdit.");
			return;
		}
		JEditTextArea area = view.getTextArea();
		ensureBreapointPainterRegistered(area);
		//TODO: check for valid position
		Buffer buffer = (Buffer) area.getBuffer();
		String path = buffer.getPath();
		Set<Integer> breakpointLines = breakpoints.get(path);
		if (breakpointLines == null) {
			breakpointLines = new HashSet<Integer>();
			breakpoints.put(path, breakpointLines);
		}
		int line = area.getCaretLine() + 1 /* first line starts with 1*/;
		if (breakpointLines.contains(line)) {
			breakpointLines.remove(line);
		} else {
			breakpointLines.add(line);
			enableBeanshellDebugger();
		}
		area.repaint();
	}

	private void doStep() {
		doWaitForStep = false;
		stepping = true;
	}

	private void doContinue() {
		doWaitForStep = false;
		stepping = false;
	}
}
