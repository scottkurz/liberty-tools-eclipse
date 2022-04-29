package liberty.mpls.tests.it;

import org.eclipse.lsp4mp.jdt.core.config.java.MicroProfileConfigJavaHoverTest;
import org.eclipse.lsp4mp.jdt.internal.core.ls.JDTUtilsLSImpl;
import org.junit.BeforeClass;

/**
 * @author Red Hat Developers
 *
 */
public class LibertyMPLSJavaHoverTest extends MicroProfileConfigJavaHoverTest {
	@BeforeClass
	public static void init() {
		setJDTUtils(JDTUtilsLSImpl.getInstance());
	}
}
