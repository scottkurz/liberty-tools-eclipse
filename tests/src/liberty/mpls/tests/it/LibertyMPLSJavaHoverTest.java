package liberty.mpls.tests.it;

import org.eclipse.lsp4mp.jdt.core.config.java.MicroProfileConfigJavaHoverTest;
import org.eclipse.lsp4mp.jdt.internal.core.ls.JDTUtilsLSImpl;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Red Hat Developers
 *
 */
public class LibertyMPLSJavaHoverTest extends MicroProfileConfigJavaHoverTest {
	@BeforeClass
	public static void init() {
		setJDTUtils(JDTUtilsLSImpl.getInstance());
	}
	
	@Override
	@Test
	@Ignore
	public void configPropertyNamePrecedence() throws Exception {
		
	}
	
	@Override
	@Test
	@Ignore
	public void configPropertyNameProfile() throws Exception {
	}
}
