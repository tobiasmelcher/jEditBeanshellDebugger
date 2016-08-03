package bshdebugger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Locale;

import javax.swing.JMenuItem;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.gui.DynamicContextMenuService;
import org.gjt.sp.jedit.textarea.JEditTextArea;

public class BshDebugPluginContextMenuService extends DynamicContextMenuService {

	@Override
	public JMenuItem[] createMenu(final JEditTextArea ta, MouseEvent evt) {
		if (ta!=null) {
			JEditBuffer buffer = ta.getBuffer();
			if (buffer instanceof Buffer) {
				String path = ((Buffer) buffer).getPath();
				if (path!=null && path.toLowerCase(Locale.ENGLISH).endsWith(".bsh")) {
					JMenuItem item = new JMenuItem("Toggle Beanshell Breakpoint");
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							DebuggerView.toggleBreakpoint(ta.getView());
						}
					});
					return new JMenuItem[]{item};
				}
			}
		}
		return null;
	}
}
