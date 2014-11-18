package de.kmindi.fbissueexport;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import edu.umd.cs.findbugs.BugInstance;

/**
 * Creates a new issue on the platform found in the git remotes configuration or by using the one defined in the project preferences.
 * The user must be signed in to the used platform.
 * 
 * @author Kai Mindermann
 * 
 */
public class Export {

	private static Logger logger = Logger.getLogger(Export.class);

	public Export(BugInstance bug, IProject project) {
		logger.debug("new Export instance created for Bug-ID: " + bug.getInstanceHash() + "in project " + project.getName());

		IScopeContext projectScope = new ProjectScope(project);
		IEclipsePreferences projectPreferences = projectScope.getNode("de.kmindi.fbissueexport");
		int threshold = projectPreferences.getInt("threshold.confidence", 2);
		// TODO store only if changed through an interface or if really non exisitent
		projectPreferences.putInt("threshold.confidence", threshold);
		try {
			projectPreferences.flush();
		} catch (BackingStoreException e) {
			logger.error(e.getMessage(), e);
		}
		// 1 is highest confidence
		if(bug.getPriority() >= threshold) {
			Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			MessageDialog dialog = new MessageDialog(
					activeShell, 
					"Confidence below threshold!", 
					null,
					"This bug is only of " + bug.getPriorityString() + " confidence! Are you sure you want to report it?",
					MessageDialog.QUESTION_WITH_CANCEL, 
					new String[]{
						IDialogConstants.YES_LABEL, 
						IDialogConstants.NO_LABEL},
						0);

			switch(dialog.open()) {
			case 1: logger.info("stopping export because user is not sure if confidence is high enough.");
			return;
			}
		}

		// TODO only search once for possible project issue tracker
		// save it in preferences and use that

		// get file location this bug is found in
		File file = ProjectUtils.getSourceFile(project, bug);
		logger.debug("searching for versioned directory, starting at: " + file);

		if(new RepositoryBuilder().findGitDir(file).getGitDir() != null) {
			logger.debug("found versioned directory" + file);
			try {
				// get remotes and check if it contains github(or other popular platforms)
				Repository repository = new RepositoryBuilder().findGitDir(file).build();
				//Config storedConfig = repository.getConfig();
				//Set<String> remotes = storedConfig.getSubsections("remote");

				// parse url for popular social coding platforms
				IPlatformExporter pe = PlatformExporterFactory.matchPlatform(repository.getConfig(), bug, project);
				if(pe != null) {  
					pe.exportBug();
				}

			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			logger.debug("no versioned directory found");
		}
	}
}
