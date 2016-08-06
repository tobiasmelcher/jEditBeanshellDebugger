package bshdebugger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.gjt.sp.jedit.BeanShell;
import org.gjt.sp.jedit.EditPlugin;

import bshdebugger.options.OptionPane;

public class BshDebugPlugin extends EditPlugin {

	@Override
	public void start() {
		super.start();
		if (OptionPane.isBshDebuggerEnabled()==false) {
			return;
		}
		replaceJeditBSHBlockClass(); // at this point in time BSHBlock class is
										// not yet loaded and therefore we can
										// replace it
		replaceInputHandler();
	}

	private void replaceInputHandler() {
		//TODO: check for which beanshell script exists a compiled java class
		//example how to replace current binding with a wrapper to be able to call generated code instead the beanshell
		/*
		Timer t = new Timer(10*1000, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {

					DefaultInputHandler dh = (DefaultInputHandler) jEdit.getActiveView().getInputHandler();
					Field f = AbstractInputHandler.class.getDeclaredField("currentBindings");
					f.setAccessible(true);
					Hashtable bindings = (Hashtable) f.get(dh);
					for (Object key:bindings.keySet()) {
						Object value = bindings.get(key);
						if ("tm/my_bsh_completion".equals(value)) {
							ActionSet set = jEdit.getActionContext().getActionSetForAction((String) value);
							set.addAction(new EditAction("tm/my_bsh_completion2") {
								
								@Override
								public void invoke(View view) {
									jEdit.getAction("tm/my_bsh_completion").invoke(jEdit.getActiveView());
								}
							});
							bindings.put(key, "tm/my_bsh_completion2");
							return;
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		t.setRepeats(false);
		t.start();
		*/
	}

	@Override
	public void stop() {
		super.stop();
	}

	private static void replaceJeditBSHBlockClass() {
		replaceClass("BSHBlock.pclass", "org.gjt.sp.jedit.bsh.BSHBlock", BeanShell.class.getClassLoader());
	}

	private static void replaceClass(String fileName, String fqn, ClassLoader loader) {
		try {
			InputStream is = DebuggerView.class.getResourceAsStream(fileName);
			byte[] buf = getBytesFromInputStream(is);
			Method m = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class,
					int.class);
			m.setAccessible(true);
			m.invoke(loader, fqn, buf, 0, buf.length);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | IOException e) {
			e.printStackTrace();
		}
	}

	public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream();) {
			byte[] buffer = new byte[0xFFFF];

			for (int len; (len = is.read(buffer)) != -1;)
				os.write(buffer, 0, len);

			os.flush();

			return os.toByteArray();
		}
	}
}
