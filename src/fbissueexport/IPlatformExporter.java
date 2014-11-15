package fbissueexport;

import java.net.URI;

/**
 * Interface for PlatformExporters
 * @author Kai Mindermann
 *
 */
public interface IPlatformExporter {
	/**
	 * Exports the bug to the specific platform
	 * @return
	 */
	abstract public boolean exportBug();
	
	/**
	 * Checks if the Bug has already bin exported/reported
	 * @return null if not exported an URI to the bug-report otherwise.
	 */
	abstract public URI isBugAlreadyExported();
}
