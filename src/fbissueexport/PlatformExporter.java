package fbissueexport;

import java.awt.Desktop;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugRankCategory;

/**
 * Base Class for PlatformExpoter.
 * @author Kai Mindermann
 *
 */
public abstract class PlatformExporter implements IPlatformExporter{
	
	private static Logger logger = Logger.getLogger(PlatformExporter.class);
	protected String ownerName = null;
	protected String repositoryName = null;
	protected BugInstance bugInstance = null;
	protected IProject project = null;
	
	/**
	 * Constructor which can only be used by subclasses. 
	 * 
	 * Initializes used variables.
	 * @param ownerName
	 * @param repositoryName
	 * @param bugInstance
	 * @param project
	 */
	protected PlatformExporter(String ownerName, String repositoryName, BugInstance bugInstance, IProject project) {
		this.ownerName = ownerName;
		this.repositoryName = repositoryName;
		this.bugInstance = bugInstance;
		this.project = project;
	}
	
	/**
	 * Performs a HTTP GET Request.
	 * @param url
	 * @return ResponseWithEntity(HTTPResponse, String entity)
	 */
	protected ResponseWithEntity httpGetRequest(String url) {
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
			HttpGet request = new HttpGet(url);
			request.addHeader("content-type", "application/json");
			logger.debug("request line:" + request.getRequestLine());
			HttpResponse result = httpClient.execute(request);
			logger.debug("request status: " + result.getStatusLine());
			return new ResponseWithEntity(result,EntityUtils.toString(result.getEntity(), "UTF-8"));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	/**
	 * Gets a title for the bug.
	 * @return
	 */
	protected String getBugTitle() {
		return bugInstance.getMessageWithoutPrefix();
	}

	/**
	 * Gets a Markdown formatted description of the bug.
	 * @return
	 */
	protected String getBugDescription() {
		String md = "";
		md += "# " + bugInstance.getAbridgedMessage() + "\n";
		md += "\n\n"
				+ bugInstance.getBugPattern().getDetailText() + "\n\n";

		md += "The problem occurs in `"
				+ bugInstance.getPrimarySourceLineAnnotation().getClassName()
				+ "` on line **" + bugInstance.getPrimarySourceLineAnnotation().getStartLine() + "**";
		if(bugInstance.getPrimaryMethod() != null) {
			md += "in method `" + bugInstance.getPrimaryMethod().getMethodName() + "`";
		} else if(bugInstance.getPrimaryField() != null) {
			md += "in field `" + bugInstance.getPrimaryField().getFieldName() + "`";
		} else if(bugInstance.getPrimaryLocalVariableAnnotation() != null) {
			md += "in local variable `" + bugInstance.getPrimaryLocalVariableAnnotation().getName() + "`";
		}
		md += ":\n\n";
				

		md += "```java\n";
		md += getSourceCodeFragment(ProjectUtils.getSourceFile(project, bugInstance), bugInstance.getPrimarySourceLineAnnotation().getStartLine() - 5, bugInstance.getPrimarySourceLineAnnotation().getEndLine() + 5 );
		md += "```\n\n";

		md += "We have **" + bugInstance.getPriorityString() + "** confidence for this **" + BugRankCategory.getRank(bugInstance.getBugRank()) + "** bug!";
		md +="\n\nThis bug was found by FindBugs and exported using kmindi's [FBIssueExport](https://github.com/kmindi/FBIssueExport). \n"
				+ "(FindBugs Bug-ID: "+ bugInstance.getInstanceHash() + ")";

		return md;
	}
	
	/**
	 * Gets a specified range of lines from a file.
	 * @param file
	 * @param start
	 * @param end
	 * @return
	 */
	protected String getSourceCodeFragment(File file, int start, int end) {

		if(start <= 1) {
			start = 1;
		}

		try (LineNumberReader rdr = new LineNumberReader(new FileReader(file))) {
			StringBuilder sb1 = new StringBuilder();
			for (String line = null; (line = rdr.readLine()) != null;) {
				if (rdr.getLineNumber() >= start && rdr.getLineNumber() <= end) {
					sb1.append(line).append("\n");
				}
			}
			return sb1.toString();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}
	
	
	
	/**
	 * Opens a new browser window or new tab if browser already open.
	 * @param uri
	 */
	protected static void openWebPage(URI uri) {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(uri);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				// TODO fallback to API use / show own GUI to create new issue
			}
		}
	}

	@Override
	public boolean exportBug() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public URI isBugAlreadyExported() {
		// TODO Auto-generated method stub
		return null;
	}
}
