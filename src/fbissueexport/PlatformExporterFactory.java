package fbissueexport;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.jgit.lib.Config;

import edu.umd.cs.findbugs.BugInstance;

/**
 * Factory class for platform exporters.
 * 
 * IPlatformExporter implementing classes can be created either by calling matchPlatform(...) or by createExporter(String platformIdentifier).
 * 
 * @author Kai Mindermann
 *
 */
public class PlatformExporterFactory {
	
	private static Logger logger = Logger.getLogger(PlatformExporterFactory.class);
	
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
			} else if((matcher = SourceForgeExporter.getplatformURLPattern().matcher(url)).matches()){
				// TODO create sourceforgepattern
				// TODO call SourceForgeExporter
				
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
	
	

	
}
