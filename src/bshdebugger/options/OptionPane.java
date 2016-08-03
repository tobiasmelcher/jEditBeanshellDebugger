package bshdebugger.options;

import javax.swing.JCheckBox;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;

public class OptionPane extends AbstractOptionPane {

	private static final long serialVersionUID = 1L;
	private JCheckBox enabledCheckbox;

	public OptionPane() {
		super("bshdebugger.general");
	}

	protected void _init() {
		enabledCheckbox = new JCheckBox();
		enabledCheckbox.setSelected(isBshDebuggerEnabled());
		addComponent(jEdit.getProperty("options.bshdebugger.general.enabled", "Beanshell Debugger enabled (influences beanshell script performance if enabled; option requires restart to take effect):"),
			enabledCheckbox);
	}

	protected void _save() {
		jEdit.setBooleanProperty("bshdebugger.enabled", enabledCheckbox.isSelected());
	}
	
	public static boolean isBshDebuggerEnabled() {
		String enabled = jEdit.getProperty("bshdebugger.enabled");
		if (enabled==null)
			return true;
		boolean result = jEdit.getBooleanProperty("bshdebugger.enabled");
		return result;
	}
}
