package fbissueexport;

import java.awt.Desktop;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URI;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.jgit.lib.Config;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugRankCategory;

/**
 * Factory class for platform exporters.
 * 
 * PlatformExporter-Subclasses should be created either by calling matchPlatform(...) or by createExporter(String platformIdentifier).
 * 
 * @author Kai Mindermann
 *
 */
public class PlatformExporterFactory {
	
	private static Logger logger = Logger.getLogger(PlatformExporterFactory.class);
	protected String ownerName = null;
	protected String repositoryName = null;
	protected BugInstance bugInstance = null;
	protected IProject project = null;
	
	/**
	 * Regular Expression to check for different kinds of platforms.
	 * It expects a naming convention like "owner/repositoryName" after the platform URL like "github.com".
	 * 
	 * Groups:
	 *  - Group 3: Platform URL
	 *  - Group 5: Owner
	 *  - Group 6: Repository
	 */
	private static Pattern platformUrlPattern = Pattern.compile("((git@|https://)([\\w\\.@]+)(/|:))([\\w,\\-,\\_]+)/([\\w,\\-,\\_]+)(.git){0,1}((/){0,1})");
	
	/**
	 * Parses existing entrys in a git configuration for matching remote urls and uses the first matching to create a new PlatformExporter.
	 * @param storedConfig
	 * @param bugInstance
	 * @param project
	 * @return null if no matching url was found or the corresponding PlatformExporter
	 */
	public static IPlatformExporter matchPlatform(Config storedConfig, BugInstance bugInstance, IProject project) {
		
		Set<String> remotes = storedConfig.getSubsections("remote");
		for (String remoteName : remotes) {
			String url = storedConfig.getString("remote", remoteName, "url");
			logger.debug("trying to match: " + url);
			// parse url for popular social coding platforms
			Matcher matcher = platformUrlPattern.matcher(url);
			if(matcher.matches()) {
				// found a plattform
				logger.debug("regex matched platform: " + matcher.group(3) + " owner: " + matcher.group(5) + " repo: " + matcher.group(6));
				
				String platformURLPart = matcher.group(3).toLowerCase();
				switch(platformURLPart) {
				case "github.com": return new GitHubExporter(matcher.group(5), matcher.group(6), bugInstance, project);
				case "bitbucket.org": return new BitbucketExporter(matcher.group(5), matcher.group(6), bugInstance, project);
				default: return null;
				}
				
				// TODO check if the issue tracker is used on this platform
				// TODO save used tracker in project preferences
			}
		}
		return null;
	}
	
	/**
	 * Creates an IPlatformExporter by using platformIdentifier to create the correct one.
	 * @param platformIdentifier
	 * @param owner
	 * @param repoName
	 * @param bugInstance
	 * @param project
	 * @return null if platformIdentifier does not match any platform, the corresponding PlatformExporter otherwise
	 */
	public static IPlatformExporter createExporter(String platformIdentifier, String owner, String repoName, BugInstance bugInstance, IProject project) {
		switch(platformIdentifier) {
		case "github.com": return new GitHubExporter(owner, repoName, bugInstance, project);
		case "bitbucket.org": return new BitbucketExporter(owner, repoName, bugInstance, project);
		case "sourceforge.net": return new SourceForgeExporter(owner, repoName, bugInstance, project);
		default: return null;
		}
	}
	
	/**
	 * Constructor which can only be used by subclasses. 
	 * 
	 * Initializes used variables.
	 * @param ownerName
	 * @param repositoryName
	 * @param bugInstance
	 * @param project
	 */
	protected PlatformExporterFactory(String ownerName, String repositoryName, BugInstance bugInstance, IProject project) {
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
			logger.error(e.getMessage() + "\n" + e.getStackTrace());
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
				+ "` on line **" + bugInstance.getPrimarySourceLineAnnotation().getStartLine() 
				+ "** in method `" + bugInstance.getPrimaryMethod().getMethodName() + "`:\n\n";

		md += "```java\n";
		md += getSourceCodeFragment(ProjectUtils.getSourceFile(project, bugInstance), bugInstance.getPrimarySourceLineAnnotation().getStartLine() - 5, bugInstance.getPrimarySourceLineAnnotation().getEndLine() + 5 );
		md += "```\n\n";

		md += "We have **" + bugInstance.getPriorityString() + "** confidence for this **" + BugRankCategory.getRank(bugInstance.getBugRank()) + "** bug!";
		md +="\n\nThis bug was found by FindBugs and exported using [FBIssueExport](https://github.com/kmindi/FBIssueExport) by kmindi. \n"
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
			logger.error(e.getMessage() + "\n" + e.getStackTrace());
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
				logger.error(e.getMessage() + "\n" + e.getStackTrace());
				// TODO fallback to API use / show own GUI to create new issue
			}
		}
	}

	
}
