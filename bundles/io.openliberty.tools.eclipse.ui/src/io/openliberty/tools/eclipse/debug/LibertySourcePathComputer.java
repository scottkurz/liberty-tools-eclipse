/*******************************************************************************
* Copyright (c) 2023 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation - initial implementation
*******************************************************************************/
package io.openliberty.tools.eclipse.debug;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.GradleModuleVersion;
import org.gradle.tooling.model.eclipse.EclipseProject;

import io.openliberty.tools.eclipse.DevModeOperations;
import io.openliberty.tools.eclipse.Project;
import io.openliberty.tools.eclipse.ui.launch.StartTab;

public class LibertySourcePathComputer implements ISourcePathComputerDelegate {

    ArrayList<IRuntimeClasspathEntry> unresolvedClasspathEntries;

    @Override
    public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {

        unresolvedClasspathEntries = new ArrayList<IRuntimeClasspathEntry>();

        /*
         * This method computes the defeault source lookup paths for a particular launch configuration. We are doing this in two ways:
         * .
         * 1. We are first computing the runtime classpath entries. This is in-line with what Remote Java Application does.
         * .
         * 2. We are finding any project dependencies that are also present in the current Eclipse workspace and adding those projects.
         * . For this step we are using m2e and gradle/buildship APIs to get lists of the dependency artifacts and then checking if those
         * . artifact coordinates map to any existing projects in the workspace. At the moment, the dependency projects must be Maven
         * . projects. M2e offers APIs to lookup Maven projects in the workspace based on artifact coordinates,
         * . but Gradle/Buildship does not offer similar capabilities for Gradle projects.
         */

        // Get current project
        String mainProjectName = configuration.getAttribute(StartTab.PROJECT_NAME, (String) null);
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject launchProject = root.getProject(mainProjectName);

        Project project = DevModeOperations.getInstance().getProjectModel().getProject(mainProjectName);
        if (project.getBuildType() == Project.BuildType.MAVEN) {
            List<IProject> allProjects = new ArrayList<IProject>();

            // Add the launch project
            allProjects.add(launchProject);

            // Add any child modules projects.
            allProjects.addAll(getChildModuleProjects(launchProject));

            // Loop through each project we have in our list
            for (IProject moduleProject : allProjects) {

                addRuntimeDependencies(moduleProject);

                // Get project dependencies that are open in the same workspace
                List<IProject> projectDependencies = new ArrayList<IProject>();

                MavenProject mavenModuleProject = MavenPlugin.getMavenModelManager().readMavenProject(moduleProject.getFile("pom.xml"),
                        new NullProgressMonitor());
                Set<Artifact> artifacts = mavenModuleProject.getArtifacts();

                for (Artifact artifact : artifacts) {

                    IProject localProject = getLocalProject(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                    if (localProject != null) {
                        projectDependencies.add(localProject);
                    }
                }

                // Create the classpath entry for the project dependencies found
                for (IProject dependencyProject : projectDependencies) {

                    if (dependencyProject.isNatureEnabled(JavaCore.NATURE_ID)) {
                        IJavaProject dependencyJavaProject = JavaCore.create(dependencyProject);
                        unresolvedClasspathEntries.add(JavaRuntime.newDefaultProjectClasspathEntry(dependencyJavaProject));
                    }
                }
            }

        } else {

            addRuntimeDependencies(launchProject);

            // Get project dependencies that are open in the same workspace
            List<IProject> projectDependencies = new ArrayList<IProject>();

            GradleConnector connector = GradleConnector.newConnector();
            connector.forProjectDirectory(launchProject.getLocation().toFile());
            ProjectConnection connection = connector.connect();

            try {
                EclipseProject eclipseProject = connection.getModel(EclipseProject.class);
                for (ExternalDependency externalDependency : eclipseProject.getClasspath()) {

                    GradleModuleVersion gradleModuleVersion = externalDependency.getGradleModuleVersion();

                    IProject localProject = getLocalProject(gradleModuleVersion.getGroup(), gradleModuleVersion.getName(),
                            gradleModuleVersion.getVersion());
                    if (localProject != null) {
                        projectDependencies.add(localProject);
                    }
                }
            } finally {
                connection.close();
            }

            // Create the classpath entry for the project dependencies found
            for (IProject dependencyProject : projectDependencies) {
                IJavaProject dependencyJavaProject = JavaCore.create(dependencyProject);
                unresolvedClasspathEntries.add(JavaRuntime.newDefaultProjectClasspathEntry(dependencyJavaProject));
            }

        }

        // Resolve all classpath entries
        IRuntimeClasspathEntry[] resolvedClasspathDependencies = JavaRuntime.resolveSourceLookupPath(
                unresolvedClasspathEntries.toArray(new IRuntimeClasspathEntry[unresolvedClasspathEntries.size()]), configuration);

        // Get all source containers
        ArrayList<ISourceContainer> containersList = new ArrayList<ISourceContainer>();

        containersList.addAll(Arrays.asList(JavaRuntime.getSourceContainers(resolvedClasspathDependencies)));

        ISourceContainer[] containers = new ISourceContainer[containersList.size()];
        containersList.toArray(containers);

        return containers;
    }

    /**
     * Get project if found in local workspace based on artifact coordinates
     * 
     * @param groupId
     * @param artifactId
     * @param version
     * 
     * @return
     */
    private IProject getLocalProject(String groupId, String artifactId, String version) {

        // Check Maven projects
        IMavenProjectRegistry mavenProjectRegistry = MavenPlugin.getMavenProjectRegistry();

        IMavenProjectFacade mavenProjectFacade = mavenProjectRegistry.getMavenProject(groupId, artifactId, version);

        if (mavenProjectFacade != null) {
            return mavenProjectFacade.getProject();
        }

        // TODO: Check Gradle projects

        return null;
    }

    /**
     * Returns a list of projects in the workspace that map to the child modules of the given parent project
     * 
     * @param parentProject
     * 
     * @return
     * 
     * @throws CoreException
     */
    private List<IProject> getChildModuleProjects(IProject parentProject) throws CoreException {

        List<IProject> childModuleProjects = new ArrayList<IProject>();

        MavenProject mavenParentProject = MavenPlugin.getMavenModelManager().readMavenProject(parentProject.getFile("pom.xml"),
                new NullProgressMonitor());
        List<String> childModuleNames = mavenParentProject.getModules();

        // At this point, childModuleNames is just a list of paths relative to the parent module project.
        // There may be a better way to do this, but for now, we build the full path of the child module, and if
        // that path matches the path of a project in the workspace, that project is added as the child module project.
        for (String childModuleName : childModuleNames) {

            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            for (IProject workspaceProject : Arrays.asList(root.getProjects())) {

                String workspaceProjectPath = workspaceProject.getLocation().toOSString();
                Path childModulePath = Paths.get(parentProject.getLocation().toOSString(), childModuleName);

                // Module names can include the pom.xml file of the child module (e.g. module1/pom.xml). If a file exists at the end, remove
                // it.
                if (!childModulePath.toFile().isDirectory()) {
                    childModulePath = childModulePath.subpath(0, childModulePath.getNameCount() - 1);
                }

                if (workspaceProjectPath.equals(childModulePath.toString())) {
                    childModuleProjects.add(workspaceProject);
                }
            }
        }

        return childModuleProjects;
    }

    /**
     * Adds classpath entries for runtime dependencies to the unresolved classpath entries list
     * 
     * @param project
     * 
     * @throws CoreException
     */
    private void addRuntimeDependencies(IProject project) throws CoreException {

        // If the project is a java project, get classpath entries for runtime dependencies
        if (project.isNatureEnabled(JavaCore.NATURE_ID)) {
            List<IRuntimeClasspathEntry> runtimeDependencies = Arrays
                    .asList(JavaRuntime.computeUnresolvedRuntimeDependencies(JavaCore.create(project)));
            for (IRuntimeClasspathEntry runtimeDependency : runtimeDependencies) {
                if (!unresolvedClasspathEntries.contains(runtimeDependency)) {
                    unresolvedClasspathEntries.add(runtimeDependency);
                }
            }
        }
    }
}