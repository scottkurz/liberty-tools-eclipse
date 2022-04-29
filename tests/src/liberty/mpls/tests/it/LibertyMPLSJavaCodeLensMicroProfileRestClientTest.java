package liberty.mpls.tests.it;

import org.eclipse.lsp4mp.jdt.core.restclient.java.MicroProfileRestClientJavaCodeLensTest;
import org.eclipse.lsp4mp.jdt.internal.core.ls.JDTUtilsLSImpl;
import org.junit.BeforeClass;

/**
 * @author Red Hat Developers
 *
 */
public class LibertyMPLSJavaCodeLensMicroProfileRestClientTest extends MicroProfileRestClientJavaCodeLensTest {
	@BeforeClass
	public static void init() {
		setJDTUtils(JDTUtilsLSImpl.getInstance());
	}
}

