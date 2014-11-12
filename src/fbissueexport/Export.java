package fbissueexport;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.BugInstance;

public class Export {
	
	private static Logger logger = Logger.getLogger(Export.class);
	public Export(BugInstance bug) {
		logger.info("new Export instance created Bug-ID: "+bug.getInstanceHash());
		
		// get project git directory if it exists
		// get remotes and check if it contains github(or other popular platforms)
		// for GitHub
			// check per API if the project is a fork 
			// via https://api.github.com/repos/<OWNER>/<REPOSITORY> 
			// check for parent and if parent exists for full_name
			String apiRepoUrl = "https://api.github.com/repos/";
			String issueRepo = "kmindi/yii2";
			
			try {
				ResponseWithEntity response = httpGetRequest(apiRepoUrl + issueRepo);
				if(response == null) {
					logger.info("no response");
					return;
				}
				ObjectMapper mapper = new ObjectMapper();
				Map<String, Object> repoData = mapper.readValue(response.getEntity(), Map.class);
				Object parentData = repoData.get("parent");
				
				if(parentData != null) {
					Map<String,String> parentD = (Map<String, String>) parentData;
					issueRepo = parentD.get("full_name");
					logger.info("parent repo: " + issueRepo);
					
					// TODO use isBugAlreadyReported to check if the bug was already reported
				
					// if the bug is not filed yet create a new issue
					// https://github.com/<OWNER>/<REPOSITORY>/issues/new?title=<TITLE>&body=<DESCRIPTION>
					String title = "New Bug Title";
					String description = "Description for the bug";
					URIBuilder uriBuilder = new URIBuilder("https://github.com/" + issueRepo + "/issues/new");
					uriBuilder.addParameter("title", title);
					uriBuilder.addParameter("body", description);
					openWebPage(uriBuilder.build());
				}
			} catch (ParseException | IOException | URISyntaxException e) {
				logger.error(e.getMessage() + "\n" + e.getStackTrace());
			};
	}
	
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
		// search issue liste for Bug-ID (bug.getInstanceHash()) and dont create a new rather redirect to the existing issue
		// https://api.github.com/repos/<OWNER>/<REPOSITORY>/issues
		
		String apiRepoUrl = "https://api.github.com/repos/";
		String issueRepo = "kmindi/yii2";
		
		ResponseWithEntity response = httpGetRequest(apiRepoUrl + issueRepo + "/issues");
		// loop throug paginated response until we find a bug with our ID or reach the end
		// care about paginated answers using https://developer.github.com/guides/traversing-with-pagination/ to navigate 
		// always use the provided link header
		// link header uses "<" and ">" to enclose the url
		// but it gets parsed as name (starting with "<") and value (ending with ">")
		
		Header[] headers = response.getResponse().getAllHeaders();
		List<Header> headerList = Arrays.asList(headers);
		for(Header header : headerList) {
			//logger.info("header: " + header.getName());
			if(header.getName().equals("Link")) {
				logger.info("found Link Header: " + header.getValue());
				for(HeaderElement e : header.getElements()) {
					logger.info("element-name: "+ e.getName() + " value: " + e.getValue() + " parameters:" + e.getParameterCount());
					for(NameValuePair nvp:e.getParameters()) {
						logger.info(nvp.getName()+"="+nvp.getValue());
					}
				}
			}
		}
		
		return false;
	}
	
	private ResponseWithEntity httpGetRequest(String url) {
		logger.info("requesting url:"+url);
		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet(url);
            request.addHeader("content-type", "application/json");
            logger.info("request line:" + request.getRequestLine());
            HttpResponse result = httpClient.execute(request);
            logger.info(result.getStatusLine());
            return new ResponseWithEntity(result,EntityUtils.toString(result.getEntity(), "UTF-8"));
        } catch (IOException e) {
        	logger.info(e.getMessage() + "\n" + e.getStackTrace());
        }
		return null;
	}
}
