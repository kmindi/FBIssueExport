package fbissueexport;

import java.awt.Desktop;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugRankCategory;

public class Export {
	
	private static Logger logger = Logger.getLogger(Export.class);
	
	private BugInstance bug;

	private IProject project;
	
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
	
	public Export(BugInstance bug, IProject project) {
		logger.debug("new Export instance created for Bug-ID: " + bug.getInstanceHash() + "in project " + project.getName());
		
		this.bug = bug;
		this.project = project;
		
		// TODO read threshold from project preferences
		// 1 is highest confidence
		if(bug.getPriority() > 2) {
			Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		    MessageDialog dialog = new MessageDialog(
		    		activeShell, 
		    		"Confidence below threshold!", 
		    		null,
		    		"This bug is only of " + bug.getPriorityString() + "! Are you sure you want to report it?",
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
		// save it in prefrences and use that
		
		// get file location this bug is found in
		File file = getSourceFile();
		logger.debug("searching for versioned directory, starting at: " + file);
		
		if(new RepositoryBuilder().findGitDir(file).getGitDir() != null) {
    		logger.debug("found versioned directory" + file);
			try {
				// get remotes and check if it contains github(or other popular platforms)
				Repository repository = new RepositoryBuilder().findGitDir(file).build();
				Config storedConfig = repository.getConfig();
				Set<String> remotes = storedConfig.getSubsections("remote");
				
				for (String remoteName : remotes) {
			      String url = storedConfig.getString("remote", remoteName, "url");
			      logger.debug("found remote: " + remoteName + " " + url);
			      
			      // parse url for popular social coding platforms
			      Matcher matcher = platformUrlPattern.matcher(url);
			      if(matcher.matches()) {
			    	  // found a plattform
			    	  logger.debug("regex matched platform: " + matcher.group(3) + " owner: " + matcher.group(5) + " repo: " + matcher.group(6));
			    	  // call general export
			    	  exportToPlatformTracker(matcher.group(3), matcher.group(5), matcher.group(6));
			    	  // only export to first found platform so break loop
			    	  // TODO check if the issue tracker is used on this platform
			    	  // TODO save used tracker in project preferences
			    	  break;
			      }
			    }
			} catch (IOException e) {
				logger.error(e.getMessage() + "\n" + e.getStackTrace());
			}
		} else {
			logger.debug("no versioned directory found");
		}
	}
	
	/**
	 * Gets the primary source file referenced by the BugInstance from the corresponding project.
	 * @see getResource
	 * @return 
	 */
	protected File getSourceFile() {
		IPath path = project.getFile(bug.getPrimarySourceLineAnnotation().getSourcePath()).getProjectRelativePath();
		IResource res = getResource(project, path.removeLastSegments(1).toString(), path.lastSegment());
		return res.getLocation().toFile();
	}
	
	/**
	 * Gets a resource from a project specific location.
	 * 
	 * @see http://stackoverflow.com/a/7727264/455578
	 * @param project
	 * @param folderPath
	 * @param fileName
	 * @return
	 */
	IResource getResource(IProject project, String folderPath, String fileName) {
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
	 * Calls the appropriate method for the corresponding platform
	 * @param platformURLPart
	 * @param owner
	 * @param repoName
	 * @return
	 */
	protected boolean exportToPlatformTracker(String platformURLPart, String owner, String repoName) {
		platformURLPart = platformURLPart.toLowerCase();
		
		switch(platformURLPart) {
		case "github.com": return exportToGitHubTracker(owner, repoName);
		case "bitbucket.org": return exportToBitbucketTracker(owner, repoName);
		default: return false;
		}
	}
	
	/**
	 * Trys to export to Bitbucket
	 * 
	 * @todo untested!!
	 * 
	 * @param owner
	 * @param repoName
	 * @return
	 */
	protected boolean exportToBitbucketTracker(String owner, String repoName) {
		
		final String apiRepoUrl = "https://api.bitbucket.org/2.0/repositories/";
		String issueRepo = owner + "/" + repoName;
		
		try {
			ResponseWithEntity response = httpGetRequest(apiRepoUrl + issueRepo);
			if(response == null) {
				logger.warn("no response");
				return false;
			}
			
			// for Bitbucket
			// check per API if the project is a fork 
			// check for parent and if parent exists for full_name
			logger.debug("checking if this repo (" + issueRepo + ") is a fork?");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> repoData = mapper.readValue(response.getEntity(), Map.class);
			Object parentData = repoData.get("parent");
			
			if(parentData != null) {
				Map<String,String> parentD = (Map<String, String>) parentData;
				issueRepo = parentD.get("full_name");
				logger.debug("is forked from: " + issueRepo);
			}
			
			// TODO use isBugAlreadyReported to check if the bug was already reported
			
			// if the bug is not filed yet create a new issue
			// https://github.com/<OWNER>/<REPOSITORY>/issues/new?title=<TITLE>&body=<DESCRIPTION>
			
			// TODO provide GUI to edit the issue before reporting
			
			HttpPost request = new HttpPost("https://bitbucket.org/api/1.0/repositories/" + issueRepo + "/issues");
			
			List<NameValuePair> params = new ArrayList<NameValuePair>(2);
			params.add(new BasicNameValuePair("title", getBugTitle()));
			params.add(new BasicNameValuePair("content", getBugDescription()));
			request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			
			logger.debug("request line:" + request.getRequestLine());
			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse result = httpClient.execute(request);
            
            // TODO open issue in webbrowser
            
			return true;
			
		} catch (ParseException | IOException e) {
			logger.error(e.getMessage() + "\n" + e.getStackTrace());
		};
		
		return false;
	}
	
	/**
	 * Creates a new issue for the bug on GitHub.
	 * @param owner
	 * @param repoName
	 * @return
	 */
	protected boolean exportToGitHubTracker(String owner, String repoName) {
		final String apiRepoUrl = "https://api.github.com/repos/";
		String issueRepo = owner + "/" + repoName;
		
		try {
			ResponseWithEntity response = httpGetRequest(apiRepoUrl + issueRepo);
			if(response == null) {
				logger.warn("no response");
				return false;
			}
			
			// for GitHub
			// check per API if the project is a fork 
			// via https://api.github.com/repos/<OWNER>/<REPOSITORY> 
			// check for parent and if parent exists for full_name
			logger.debug("checking if this repo (" + issueRepo + ") is a fork?");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> repoData = mapper.readValue(response.getEntity(), Map.class);
			Object parentData = repoData.get("parent");
			
			if(parentData != null) {
				Map<String,String> parentD = (Map<String, String>) parentData;
				issueRepo = parentD.get("full_name");
				logger.debug("is forked from: " + issueRepo);
			}
			
			// TODO use isBugAlreadyReported to check if the bug was already reported
			
			// if the bug is not filed yet create a new issue
			// https://github.com/<OWNER>/<REPOSITORY>/issues/new?title=<TITLE>&body=<DESCRIPTION>
			URIBuilder uriBuilder = new URIBuilder("https://github.com/" + issueRepo + "/issues/new");
			uriBuilder.addParameter("title", getBugTitle());
			uriBuilder.addParameter("body", getBugDescription());
			openWebPage(uriBuilder.build());
			return true;
			
		} catch (ParseException | IOException | URISyntaxException e) {
			logger.error(e.getMessage() + "\n" + e.getStackTrace());
		};
		
		return false;
	}
	
	/**
	 * Gets a title for the bug.
	 * @return
	 */
	protected String getBugTitle() {
		return bug.getMessageWithoutPrefix();
	}
	
	/**
	 * Gets a Markdown formatted description of the bug.
	 * @return
	 */
	protected String getBugDescription() {
		String md = "";
		md += "# " + bug.getAbridgedMessage() + "\n";
		md += "\n\n"
			+ bug.getBugPattern().getDetailText() + "\n\n";
		
		md += "The problem occurs in `"
				+ bug.getPrimarySourceLineAnnotation().getClassName()
				+ "` on line **" + bug.getPrimarySourceLineAnnotation().getStartLine() 
				+ "** in method `" + bug.getPrimaryMethod().getMethodName() + "`:\n\n";
		
		md += "```java\n";
		md += getSourceCodeFragment(getSourceFile(), bug.getPrimarySourceLineAnnotation().getStartLine() - 5, bug.getPrimarySourceLineAnnotation().getEndLine() + 5 );
		md += "```\n\n";
		
		md += "We have **" + bug.getPriorityString() + "** confidence for this **" + BugRankCategory.getRank(bug.getBugRank()) + "** bug!";
		md +="\n\nThis bug was found by FindBugs and exported using [FBIssueExport](https://github.com/kmindi/FBIssueExport) by kmindi. \n"
				+ "(FindBugs Bug-ID: "+ bug.getInstanceHash() + ")";
		
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
	public static void openWebPage(URI uri) {
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

	
	/**
	 * Check all issues of the project if a given bug was already reported.
	 * @todo not implemented yet
	 * @todo cache result and used cache version to check for further queries
	 * @see https://api.github.com/repos/<OWNER>/<REPOSITORY>/issues
	 * @param bug
	 * @return Boolean if the bug has been reported before
	 */
	@SuppressWarnings("unused")
	private Boolean isBugAlreadyReported(BugInstance bug) {
		// search issue list for Bug-ID (bug.getInstanceHash()) and don't create a new rather redirect to the existing issue
		// https://api.github.com/repos/<OWNER>/<REPOSITORY>/issues
		
		// loop through paginated response until we find a bug with our ID or reach the end
		// care about paginated answers using https://developer.github.com/guides/traversing-with-pagination/ to navigate 
		// always use the provided link header
		// link header uses "<" and ">" to enclose the url
		// but it gets parsed as name (starting with "<") and value (ending with ">")
		
		/*Header[] headers = response.getResponse().getAllHeaders();
		List<Header> headerList = Arrays.asList(headers);
		for(Header header : headerList) {
			//logger.info("header: " + header.getName());
			if(header.getName().equals("Link")) {
				logger.debug("found Link Header: " + header.getValue());
				for(HeaderElement e : header.getElements()) {
					logger.debug("element-name: "+ e.getName() + " value: " + e.getValue() + " parameters:" + e.getParameterCount());
					for(NameValuePair nvp:e.getParameters()) {
						logger.debug(nvp.getName()+"="+nvp.getValue());
					}
				}
			}
		}*/
		
		return false;
	}
	
	/**
	 * Performs a HTTP GET Request.
	 * @param url
	 * @return ResponseWithEntity(HTTPResponse, String entity)
	 */
	private ResponseWithEntity httpGetRequest(String url) {
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
}
