/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
/**
 * This class is a copy/paste of jbosstools-quarkus language server plugin
 * https://github.com/jbosstools/jbosstools-quarkus/blob/main/plugins/org.jboss.tools.quarkus.lsp4e/src/org/jboss/tools/quarkus/lsp4e/QuarkusLanguageClient.java
 * with modifications made for the Liberty Tools Microprofile LS plugin
 *
 */
package io.openliberty.tools.eclipse.liberty.languageserver;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.swt.widgets.Display;

/**
 * Liberty Config language client.
 * 
 * @author
 */
public class LibertyLSClientImpl extends LanguageClientImpl {

    public LibertyLSClientImpl() {
        super();
        IWorkspace iWorkspace = ResourcesPlugin.getWorkspace();
        LCLSListener resourceChangeListener = new LCLSListener();
        iWorkspace.addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.PRE_BUILD);

    }

    public void fireUpdate(String uri) {
        // List<FileEvent> fileEvents = uris.stream()
        // .map(uri -> new FileEvent(uri, FileChangeType.Changed)).toList();
        List<FileEvent> fileEvents = new ArrayList<FileEvent>();
        fileEvents.add(new FileEvent(uri, FileChangeType.Changed));
        DidChangeWatchedFilesParams params = new DidChangeWatchedFilesParams();
        params.setChanges(fileEvents);

        getLanguageServer().getWorkspaceService().didChangeWatchedFiles(params);
    }

    public class LCLSListener implements IResourceChangeListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void resourceChanged(IResourceChangeEvent event) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {

                    IResourceDelta delta = event.getDelta();
                    if (delta == null) {
                        return;
                    }

                    // On entry the resource type is the root workspace. Find the child resources affected.
                    IResourceDelta[] resourcesChanged = delta.getAffectedChildren();

                    List<IProject> projectsChanged = new ArrayList<IProject>();

                    boolean refreshNeeded = false;

                    // Iterate over the affected resources.
                    for (IResourceDelta resourceChanged : resourcesChanged) {
                        IResource iResource = resourceChanged.getResource();
                        if (iResource.getType() != IResource.PROJECT) {
                            continue;
                        }
                        IProject iProject = (IProject) iResource;
                        projectsChanged.add(iProject);

                        int updateFlag = resourceChanged.getFlags();

                        switch (resourceChanged.getKind()) {
                        // Project opened/closed.
                        // Flag OPEN (16384): "Change constant (bit mask) indicating that the resource was opened or closed"
                        // Flag 147456: Although IResourceDelta does not have a predefined constant, this flag value is used to
                        // denote open/close actions.
                        case IResourceDelta.CHANGED:
                            // if (iResource.getName().contains("liberty-plugin-config.xml")) {
                            refreshNeeded = true;
                            // }
                            break;
                        // Project created/imported.
                        // Flag OPEN (16384): "This flag is ... set when the project did not exist in the "before" state."
                        // Flag 147456: Although IResourceDelta does not have a predefined constant, this flag
                        // value is set when a project, that previously did not exist, is created.
                        case IResourceDelta.ADDED:
                            break;
                        // Project deleted.
                        // Flag NO_CHANGE (0).
                        // Flag MARKERS (130172).
                        case IResourceDelta.REMOVED:

                            break;
                        default:
                            break;
                        }
                        if (refreshNeeded) {
                            fireUpdate(iResource.getLocationURI().toString() + "/target/liberty-plugin-config.xml");
                        }
                    }

                }
            });
        }
    }

}
