package liberty.mpls.tests.it;

import org.eclipse.lsp4mp.jdt.core.health.java.MicroProfileHealthJavaDiagnosticsTest;
import org.eclipse.lsp4mp.jdt.internal.core.ls.JDTUtilsLSImpl;
import org.junit.BeforeClass;
import org.junit.Ignore;

/**
 * @author Red Hat Developers
 *
 */
//@Ignore("JDT and JDT-LS JavaDoc2HTML generators differs")
public class LibertyMPLSJavaDiagnosticsMicroProfileHealthTest extends MicroProfileHealthJavaDiagnosticsTest {
	@BeforeClass
	public static void init() {
		setJDTUtils(JDTUtilsLSImpl.getInstance());
	}
}