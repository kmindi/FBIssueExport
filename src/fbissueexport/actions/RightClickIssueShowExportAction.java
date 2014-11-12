package fbissueexport.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.reporter.MarkerUtil;
import fbissueexport.Export;

public class RightClickIssueShowExportAction implements IObjectActionDelegate{

	private ISelection selection;
	private IWorkbenchPart targetPart;
	private Shell shell;

	@Override
	public void run(IAction action) {
		if (targetPart == null) {
            return;
        }
        try {
            if (!selection.isEmpty() && (selection instanceof IStructuredSelection)) {
                IStructuredSelection ssel = (IStructuredSelection) selection;
                Object element = ssel.getFirstElement();
                IMarker marker = (IMarker) element;
                
                MessageDialog.openInformation(
        				shell,
        				"FindBugsGitComments",
        				"New Action was executed on " + marker.getResource().getClass() + 
        				" which is from project " + getSelectedProject(marker.getResource()) + ".");
                if (MarkerUtil.isFindBugsMarker(marker)) {
                	new Export(MarkerUtil.findBugInstanceForMarker(marker));
                }
            }
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            targetPart = null;
        }
		
	}
	
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
		this.shell = targetPart.getSite().getShell();
	}
	
}
