package de.kmindi.fbissueexport.actions;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.kmindi.fbissueexport.Export;
import de.tobject.findbugs.reporter.MarkerUtil;

/**
 * Action that is called for right click on FindBugs Bugs and provides an export.
 * @see Export
 * @author Kai Mindermann
 */
public class RightClickIssueShowExportAction implements IObjectActionDelegate{

	private ISelection selection;
	private IWorkbenchPart targetPart;
	private static Logger logger = Logger.getLogger(Export.class);
	
	@Override
	public void run(IAction action) {
		if (targetPart == null) {
            return;
        }
        try {
            // get the selected elment (the one the right click was done on)
        	// and call Export
        	
        	if (!selection.isEmpty() && (selection instanceof IStructuredSelection)) {
                IStructuredSelection ssel = (IStructuredSelection) selection;
                Object element = ssel.getFirstElement();
                IMarker marker = (IMarker) element;
                logger.debug("new action executed on " + marker.getResource().getClass() + 
        				" which is from project " + getSelectedProject(marker.getResource()) + ".");
                if (MarkerUtil.isFindBugsMarker(marker)) {
                	IProject project = getSelectedProject(marker.getResource());
                	if(project != null) {
                		new Export(MarkerUtil.findBugInstanceForMarker(marker), project);
                	} else {
                		logger.info("project not found for marker");
                	}
                }
            }
        } catch (Exception e) {
        	logger.error(e.getMessage(), e);
		} finally {
            targetPart = null;
        }
	}
	
	/**
	 * Returns the project for a given Object if it belongs to a project.
	 * @param obj
	 * @return Project this object belongs to
	 * @throws Exception
	 */
	public static IProject getSelectedProject(Object obj) throws Exception {
        if (obj == null) {
            return null;
        }
        if (obj instanceof IResource) {
            return ((IResource) obj).getProject();
        } else if (obj instanceof IStructuredSelection) {
            return getSelectedProject(((IStructuredSelection) obj).getFirstElement());
        } else if(obj instanceof IAdaptable)  {
        	IResource res = (IResource)(((IAdaptable)obj).getAdapter(IResource.class));
            if (res != null) {
                return res.getProject();
            }
        }
        return null;
    }

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
		
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}
	
}
