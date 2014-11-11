package fbissueexport;

import org.apache.log4j.Logger;

import edu.umd.cs.findbugs.BugInstance;

public class Export {
	
	private Logger logger = Logger.getLogger(Export.class);
	public Export(BugInstance bug) {
		logger.info("new Export instance created.");
		logger.info("bug instance hash:"+bug.getInstanceHash());
	}
}
