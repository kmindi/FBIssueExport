package de.kmindi.fbissueexport;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import org.apache.http.ParseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;

import edu.umd.cs.findbugs.BugInstance;

/**
 * PlatformExporter for SourceForge
 * @author Kai Mindermann
 *
 */
public class SourceForgeExporter extends PlatformExporter implements IPlatformExporter {

	private static Logger logger = Logger.getLogger(SourceForgeExporter.class);
	
	/**
	 * Regular Expression to check for sourceforge git urls
	 * 
	 * http://sourceforge.net/p/forge/documentation/Git/
	 * SF git urls look like this: 
	 * - read-only-access:
	 * 	- git://git.code.sf.net/p/PROJECTNAME/MOUNTPOINT/
	 * 	- http://git.code.sf.net/p/PROJECTNAME/MOUNTPOINT/
	 * - developer-read-write-access:
	 * 	- ssh://USERNAME@git.code.sf.net/p/PROJECTNAME/MOUNTPOINT
	 *  - https://USERNAME@git.code.sf.net/p/PROJECTNAME/MOUNTPOINT
	 *  
	 *  Where MOUNTPOINT usually is "code"
	 *  
	 *  Groups:
	 *  TODO
	 */
	private static Pattern platformURLPattern = Pattern.compile("((git@|https://)([\\w\\.@]+)(/|:))([\\w,\\-,\\_]+)/([\\w,\\-,\\_]+)(.git){0,1}((/){0,1})");
	
	
	protected SourceForgeExporter(String ownerName, String repositoryName,
			BugInstance bugInstance, IProject project) {
		super(ownerName, repositoryName, bugInstance, project);
	}
	
	public boolean exportBug() {
		final String newIssueURL = "https://sourceforge.net/p/" + repositoryName + "/bugs/new/";
		try {
			// TODO use isBugAlreadyReported to check if the bug was already reported

			// if the bug is not filed yet create a new issue
			//https://sourceforge.net/p/<REPOSITORY>/bugs/new/?summary=<TITLE>&description=<DESCRIPTION>
			URIBuilder uriBuilder = new URIBuilder(newIssueURL);
			uriBuilder.addParameter("summary", getBugTitle());
			uriBuilder.addParameter("description", getBugDescription());
			openWebPage(uriBuilder.build());
			return true;

		} catch (ParseException | URISyntaxException e) {
			logger.error(e.getMessage(), e);
		};

		return false;
	}
	
	/**
	 * Check all issues of the project if a given bug was already exported/reported.
	 * @todo not implemented yet
	 * @todo cache result and used cache version to check for further queries
	 * @param bug
	 * @return Boolean if the bug has been reported before
	 */
	public URI isBugAlreadyExported() {
		return null;
	}
	
	public static Pattern getplatformURLPattern() {
		return platformURLPattern;
	}

}
