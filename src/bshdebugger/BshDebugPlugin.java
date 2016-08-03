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
