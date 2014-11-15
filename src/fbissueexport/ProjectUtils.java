package fbissueexport;

import java.io.File;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import edu.umd.cs.findbugs.BugInstance;

/**
 * Useful methods for working with resources of eclipse projects
 * @author Kai Mindermann
 *
 */
public class ProjectUtils {
	
	private static Logger logger = Logger.getLogger(ProjectUtils.class);
	
	/**
	 * Gets a resource from a project specific location.
	 * 
	 * @see http://stackoverflow.com/a/7727264/455578
	 * @param project
	 * @param folderPath
	 * @param fileName
	 * @return
	 */
	public static IResource getResource(IProject project, String folderPath, String fileName) {
		IJavaProject javaProject = JavaCore.create(project);
		try {
			for (IPackageFragmentRoot root : javaProject.getAllPackageFragmentRoots()) {
				IPackageFragment folderFragment = root.getPackageFragment(folderPath);
				IResource folder = folderFragment.getResource();
				if (folder == null || ! folder.exists() || !(folder instanceof IContainer)) {
					continue;
				}

				IResource resource = ((IContainer) folder).findMember(fileName);
				if (resource.exists()) {
					return resource;
				}
			}
		} catch (JavaModelException e) {
			logger.error(e.getMessage() + "\n" + e.getStackTrace());
		}
		// file not found in any source path
		return null;
	} 
	
	/**
	 * Gets the primary source file referenced by the BugInstance from the corresponding project.
	 * @see getResource
	 * @return 
	 */
	public static File getSourceFile(IProject project, BugInstance bugInstance) {
		IPath path = project.getFile(bugInstance.getPrimarySourceLineAnnotation().getSourcePath()).getProjectRelativePath();
		IResource res = getResource(project, path.removeLastSegments(1).toString(), path.lastSegment());
		return res.getLocation().toFile();
	}
}
