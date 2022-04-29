package liberty.mpls.tests.it;

import org.eclipse.lsp4mp.jdt.core.restclient.java.MicroProfileRestClientJavaDiagnosticsTest;
import org.eclipse.lsp4mp.jdt.internal.core.ls.JDTUtilsLSImpl;
import org.junit.BeforeClass;
import org.junit.Ignore;

/**
 * @author Red Hat Developers
 *
 */
//@Ignore(value = "Diff between JDT and JDT-LS")
public class LibertyMPLSJavaDiagnosticsMicroProfileRestClientTest extends MicroProfileRestClientJavaDiagnosticsTest {
	@BeforeClass
	public static void init() {
		setJDTUtils(JDTUtilsLSImpl.getInstance());
	}
}
