package liberty.mpls.tests.it;

import org.eclipse.lsp4mp.jdt.internal.core.ls.JDTUtilsLSImpl;
import org.junit.BeforeClass;

import org.eclipse.lsp4mp.jdt.core.config.java.MicroProfileConfigJavaDefinitionTest;

/**
 * @author Red Hat Developers
 *
 */
public class LibertyMPLSMicroProfileConfigPropertyTest extends MicroProfileConfigJavaDefinitionTest {
	@BeforeClass
	public static void init() {
		setJDTUtils(JDTUtilsLSImpl.getInstance());
	}
}
