package de.kmindi.fbissueexport;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.BugInstance;

/**
 * PlatformExporter for Bitbucket.
 * @author Kai Mindermann
 *
 */
public class BitbucketExporter extends PlatformExporter implements IPlatformExporter {

	private static Logger logger = Logger.getLogger(BitbucketExporter.class);
	
	protected BitbucketExporter(String ownerName, String repositoryName,
			BugInstance bugInstance, IProject project) {
		super(ownerName, repositoryName, bugInstance, project);
	}
	
	public boolean exportBug() {
		final String apiRepoUrl = "https://api.bitbucket.org/2.0/repositories/";
		String issueRepo = ownerName + "/" + repositoryName;

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

}
