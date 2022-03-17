package liberty.tools;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

import liberty.tools.utils.Dialog;
import liberty.tools.utils.Project;

/**
 * Provides the implementation of all supported dev mode operations.
 */
public class DevModeOperations {
    // TODO: Dashboard display: Handle the case where the project is configured to be built/run by both
    // Gradle and Maven at the same time.

    // TODO: Establish a Maven/Gradle command precedence (i.e. gradlew -> gradle configured ->
    // gradle_home).
	
	private IConsole currentConsole;
	
	private void initConsole(String name) throws Exception {
		IProject project = Project.getSelected();
		String projectName = project.getName();
		
		if (currentConsole == null) {
			// Open Console
	    	MessageConsole console = new MessageConsole(name, null);
	        ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { console });
	        ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
	        currentConsole = console;
		} else {
			throw new Exception("A process is already running for application: " + projectName);
		}
	}
	
	private IConsole findConsole(String name) {
		
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++) {
            if (existing[i].getName().startsWith(name)) {
                return existing[i];
            }
        }
        
        return null;
   }

    /**
     * Starts the server in development mode.
     * 
     * @return An error message or null if the command was processed successfully.
     */
    public void start() {
    	IProject project = Project.getSelected();
		String projectName = project.getName();
        String projectPath = Project.getPath(project);
        
        if (projectPath == null) {
            Dialog.displayErrorMessage("Unable to find the home path to the selected project: " + projectName);
            return;
        }
        
        try {
            if (Project.isMaven(project)) {
                if (!Project.isMavenBuildFileValid(project)) {
                    System.out.println("Maven build file on project" + projectName + " is not valid..");
                }               
                runMavenCommand("liberty:dev");
                
            } else if (Project.isGradle(project)) {
                if (!Project.isGradleBuildFileValid(project)) {
                    System.out.println("Build file on project" + projectName + " is not valid.");
                }
                initConsole(projectName);
                String cmd = "gradle libertyDev -p=" + projectPath;
                cmd = Paths.get(getGradleInstallHome(), "bin", cmd).toString();
                runCommand(cmd);
            } else {
                Dialog.displayErrorMessage("Project" + projectName + "is not a Gradle or Maven project.");

                return;
            }
        } catch (Exception e) {
            Dialog.displayErrorMessageWithDetails("An error was detected while performing the start action on project " + projectName, e);
            return;
        }

    }

    /**
     * Starts the server in development mode.
     * 
     * @return An error message or null if the command was processed successfully.
     */
    public void startWithParms(String userParms) {
    	IProject project = Project.getSelected();
		String projectName = project.getName();
        String projectPath = Project.getPath(project);
        
        if (projectPath == null) {
            Dialog.displayErrorMessage("Unable to find the home path to the selected project: " + projectName);
            return;
        }

        try {
            String cmd = "";
            if (Project.isMaven(project)) {
                runMavenCommand("liberty:dev " + userParms);
                
            } else if (Project.isGradle(project)) {
            	initConsole(projectName);
                cmd += "gradle libertyDev " + userParms + " -p=" + projectPath;
                cmd = Paths.get(getGradleInstallHome(), "bin", cmd).toString();
                runCommand(cmd);
            } else {
                Dialog.displayErrorMessage("Project" + projectName + "is not a Gradle or Maven project.");
                return;
            }
        } catch (Exception e) {
            Dialog.displayErrorMessageWithDetails("An error was detected while performing the start... action on project " + projectName, e);
            return;
        }
    }

    /**
     * Starts the server in development mode.
     * 
     * @return An error message or null if the command was processed successfully.
     */
    public void startInContainer() {
    	IProject project = Project.getSelected();
		String projectName = project.getName();
        String projectPath = Project.getPath(project);
        
        if (projectPath == null) {
            Dialog.displayErrorMessage("Unable to find the home path to the selected project: " + projectName);
            return;
        }
        
        try {
            String cmd = "";
            if (Project.isMaven(project)) {
                runMavenCommand("liberty:devc");
                
            } else if (Project.isGradle(project)) {
            	initConsole(projectName);
                cmd += "gradle libertyDevc -p=" + projectPath;
                cmd = Paths.get(getGradleInstallHome(), "bin", cmd).toString();
                runCommand(cmd);
            } else {
                Dialog.displayErrorMessage("Project" + projectName + "is not a Gradle or Maven project.");
            }
        } catch (Exception e) {
            Dialog.displayErrorMessageWithDetails(
                    "An error was detected while performing the start in container action on project " + projectName, e);
            return;
        }
    }

    /**
     * Starts the server in development mode.
     * 
     * @return An error message or null if the command was processed successfully.
     */
    public void stop() {
    	if (currentConsole != null) {
    		try {
                runCommand("q");
                currentConsole = null;
            } catch (Exception e) {
                Dialog.displayErrorMessageWithDetails("An error was detected while performing the stop action.", e);
            }
    	} else {
    		Dialog.displayWarningMessage("The application is not currently running");
    	}
        
    }

    /**
     * Runs the tests provided by the application.
     * 
     * @return An error message or null if the command was processed successfully.
     */
    public void runTests() {
        try {
            runCommand(" ");
        } catch (Exception e) {
            Dialog.displayErrorMessageWithDetails("An error was detected while performing the run tests action.", e);
            return;
        }
    }

    /**
     * Open Maven integration test report.
     */
    public void openMavenIntegrationTestReport() {
        IProject project = Project.getSelected();
        String projName = project.getName();
        String projectPath = Project.getPath(project);
        if (projectPath == null) {
            Dialog.displayErrorMessage("Unable to find the home path to the selected project: " + projName);
            return;
        }

        try {
            String browserId = "maven.failsafe.integration.test.results";
            String name = "Maven Failsafe integration test results";
            Path path = Paths.get(projectPath, "target", "site", "failsafe-report.html");
            if (!path.toFile().exists()) {
                Dialog.displayWarningMessage("Integration test results are not available. Be sure to run the tests first.");
                return;
            }

            openTestReport(project.getName(), path, browserId, name, name);
        } catch (Exception e) {
            Dialog.displayErrorMessageWithDetails("An error was detected while opening integration test report for project " + projName, e);
            return;
        }
    }

    /**
     * Open Maven unit test report.
     */
    public void openMavenUnitTestReport() {
        IProject project = Project.getSelected();
        String projName = project.getName();
        String projectPath = Project.getPath(project);
        if (projectPath == null) {
            Dialog.displayErrorMessage("Unable to find the home path to the selected project: " + projName);
            return;
        }

        try {
            String browserId = "maven.project.surefire.unit.test.results";
            String name = "Maven Surefire unit test results";
            Path path = Paths.get(projectPath, "target", "site", "surefire-report.html");
            if (!path.toFile().exists()) {
                Dialog.displayWarningMessage("Unit test results are not available. Be sure to run the tests first.");
                return;
            }

            openTestReport(project.getName(), path, browserId, name, name);
        } catch (Exception e) {
            Dialog.displayErrorMessageWithDetails("An error was detected while opening unit test report for project " + projName, e);
            return;
        }
    }

    /**
     * Open Gradle test report.
     */
    public void openGradleTestReport() {
        IProject project = Project.getSelected();
        String projName = project.getName();
        String projectPath = Project.getPath(project);
        if (projectPath == null) {
            Dialog.displayErrorMessage("Unable to find the home path to the selected project: " + project.getName());
            return;
        }

        try {
            String browserId = "gradle.project.test.results";
            String name = "Gradle project test results";
            Path path = getGradleTestReportPath(project, projectPath);
            if (!path.toFile().exists()) {
                Dialog.displayWarningMessage("Test results are not available. Be sure to run the tests first.");
                return;
            }

            openTestReport(projName, path, browserId, name, name);
        } catch (Exception e) {
            Dialog.displayErrorMessageWithDetails("An error was detected while opening test report for project " + projName, e);
            return;
        }
    }

    /**
     * Opens the specified report in a browser.
     *
     * @param projName The application project name.
     * @param path The path to the HTML report file.
     * @param browserId The Id to use for the browser display.
     * @param name The name to use for the browser display.
     * @param toolTip The tool tip to use for the browser display.
     * 
     * @throws Exception If an error occurs while displaying the test report.
     */
    public void openTestReport(String projName, Path path, String browserId, String name, String toolTip) throws Exception {
        URL url = path.toUri().toURL();
        IWorkbenchBrowserSupport bSupport = PlatformUI.getWorkbench().getBrowserSupport();
        IWebBrowser browser = null;
        if (bSupport.isInternalWebBrowserAvailable()) {
            browser = bSupport.createBrowser(IWorkbenchBrowserSupport.AS_EDITOR | IWorkbenchBrowserSupport.LOCATION_BAR
                    | IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.STATUS, browserId, name, toolTip);
        } else {
            browser = bSupport.createBrowser(browserId);
        }

        browser.openURL(url);
    }
    
    /**
     * Use the m2e libraries to run the specified Maven command.
     * 
     * @param cmd The command to run.
     * 
     * @throws Exception If an error occurs while running the specified command.
     */
    public void runMavenCommand(String goal) throws Exception {
    	IProject project = Project.getSelected();
		String projectName = project.getName();
    	IPath workingDir = project.getLocation();
    	
        NullProgressMonitor monitor = new NullProgressMonitor();
    	
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType("org.eclipse.m2e.Maven2LaunchConfigurationType");

        // Note - project.getName() will be appended to the console name. 
        // We will use this to lookup the console for this processin the future.
        ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance((IContainer) null, projectName);

        workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, workingDir.toOSString());
        workingCopy.setAttribute("M2_GOALS", goal);

        ILaunchConfiguration launchConfig = workingCopy.doSave();

        ILaunch launch = launchConfig.launch("run", monitor, false, true); 
        
        // Wait so console has a chance to start up (better way to do this??)
        Thread.sleep(500);
        
        IConsole console = findConsole(projectName);
        
        if (console != null) {
        	currentConsole = console;
        } else {
        	throw new Exception("Something went wrong... No console was found");
        }
    }

    /**
     * Runs the specified command in the current console.
     * 
     * @param cmd The command to run.
     * 
     * @throws Exception If an error occurs while running the specified command.
     */
    public void runCommand(String cmd) throws Exception {
    	// At this point we assume we have a console open already - either
    	// from the Maven process that m2e kicked off or from us initializing a console
    	// for a Gradle build. If no console is found display an error for now.
    	
    	// TODO - this isnt working....... need to "run" a program somehow....
    	if (currentConsole != null) {
    		if (currentConsole instanceof MessageConsole) {
    			( (MessageConsole) currentConsole).newMessageStream().println(cmd);
    		} else {
    			( (ProcessConsole) currentConsole).newOutputStream().write(cmd);
    		}
    	} else {
    		throw new Exception("Something went wrong... No console was found");
    	}
    	
        
    }

    /**
     * Returns the home path to the Java installation.
     * 
     * @return The home path to the Java installation.
     */
    private String getJavaInstallHome() {
        String javaHome = null;
        // TODO: 1. Find the eclipse->java configured install path

        // 2. Check for associated system properties.
        if (javaHome == null) {
            javaHome = System.getProperty("java.home");
        }

        // 3. Check for associated environment property.
        if (javaHome == null) {
            javaHome = System.getenv("JAVA_HOME");
        }

        return javaHome;
    }

    /**
     * Returns the home path to the Maven installation.
     * 
     * @return The home path to the Maven installation.
     */
    private String getMavenInstallHome() {
        String mvnInstall = null;
        // TODO: 1. Find the eclipse->maven configured install path

        // 2. Check for associated environment property.
        if (mvnInstall == null) {
            mvnInstall = System.getenv("MAVEN_HOME");

            if (mvnInstall == null) {
                mvnInstall = System.getenv("M2_MAVEN");
            }
        }

        return mvnInstall;
    }

    /**
     * Returns the home path to the Gradle installation.
     * 
     * @return The home path to the Gradle installation.
     */
    private String getGradleInstallHome() {
        // TODO: 1. Find the eclipse->gradle configured install path.

        // 2. Check for associated environment property.
        String gradleInstall = System.getenv("GRADLE_HOME");

        return gradleInstall;
    }

    /**
     * Returns the home path to the HTML test report.
     * 
     * @return The HTML default located in the configured in the build file or the default location.
     */
    private Path getGradleTestReportPath(IProject project, String projectPath) {
        // TODO: Look for custom dir entry in build.gradle:
        // "test.reports.html.destination". Need to handle a value like this:
        // reports.html.destination = file("$buildDir/edsTestReports/teststuff")
        // Notice the use of a variable: $buildDir.

        // If a custom path was not defined, use default value.
        Path path = Paths.get(projectPath, "build", "reports", "tests", "test", "index.html");

        return path;
    }
}