package bshdebugger.options;

import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.jEdit;

public class OptionPane extends AbstractOptionPane {

	private static final long serialVersionUID = 1L;
	private JCheckBox enabledCheckbox;
	private JTextField javacLocation;

	public OptionPane() {
		super("bshdebugger.general");
	}

	protected void _init() {
		enabledCheckbox = new JCheckBox();
		enabledCheckbox.setSelected(isBshDebuggerEnabled());
		addComponent(
				jEdit.getProperty("options.bshdebugger.general.enabled",
						"Beanshell Debugger enabled (influences beanshell script performance if enabled; option requires restart to take effect):"),
				enabledCheckbox);

		javacLocation = new JTextField();
		javacLocation.setText(getJavacLocation());
		addComponent(jEdit.getProperty("options.bshdebugger.general.javac_location", "javac location:"), javacLocation);
	}

	protected void _save() {
		jEdit.setBooleanProperty("bshdebugger.enabled", enabledCheckbox.isSelected());
		jEdit.setProperty("bshdebugger.javac_location", javacLocation.getText());
	}

	public static boolean isBshDebuggerEnabled() {
		String enabled = jEdit.getProperty("bshdebugger.enabled");
		if (enabled == null)
			return true;
		boolean result = jEdit.getBooleanProperty("bshdebugger.enabled");
		return result;
	}
	
	public static String getJavacLocation() {
		String enabled = jEdit.getProperty("bshdebugger.javac_location");
		return enabled;
	}
}
