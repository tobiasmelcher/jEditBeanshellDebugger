package bshdebugger.compiler;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.gjt.sp.jedit.bsh.ParseException;
import org.gjt.sp.jedit.bsh.Parser;
import org.gjt.sp.jedit.bsh.SimpleNode;
public class BeanshellSourceUtils {

	public static List<Object> parseBeanshell(String source) {
		List<Object> result = new ArrayList<Object>();
		Parser parser = new Parser(new ByteArrayInputStream(source.getBytes()));
		try {
			while (!parser.Line()) {
				SimpleNode node = parser.popNode();
				// Macros.message(view,node.toString());
				if (node==null)continue;
				//node.dump("*");
				result.add(node);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return result;
	}
}
