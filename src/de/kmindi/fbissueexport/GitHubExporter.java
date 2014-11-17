package de.kmindi.fbissueexport;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.http.ParseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.BugInstance;

/**
 * PlatformExporter for GitHub.
 * @author Kai Mindermann
 *
 */
public class GitHubExporter extends PlatformExporter implements IPlatformExporter {

	private static Logger logger = Logger.getLogger(GitHubExporter.class);
	
	protected GitHubExporter(String ownerName, String repositoryName,
			BugInstance bugInstance, IProject project) {
		super(ownerName, repositoryName, bugInstance, project);
	}
	
	public boolean exportBug() {
		final String apiRepoUrl = "https://api.github.com/repos/";
		String issueRepo = ownerName + "/" + repositoryName;

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
			logger.error(e.getMessage(), e);
		};

		return false;
	}
	
	/**
	 * Check all issues of the project if a given bug was already exported/reported.
	 * @todo not implemented yet
	 * @todo cache result and used cache version to check for further queries
	 * @see https://api.github.com/repos/<OWNER>/<REPOSITORY>/issues
	 * @param bug
	 * @return Boolean if the bug has been reported before
	 */
	public URI isBugAlreadyExported() {
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

		return null;
	}

}
