package fbissueexport;

import org.apache.http.HttpResponse;

/**
 * Class for a HttpResponse including its entity.
 * 
 * If one would only get the response the socket to get the entity might be closed already.
 * 
 * @author Kai Mindermann
 */
public class ResponseWithEntity {
	private HttpResponse response;
	private String entity;
	
	/**
	 * Constructor.
	 * @param response the HttpResponse
	 * @param entity the entity of the HttpResponse as String
	 */
	public ResponseWithEntity(HttpResponse response, String entity) {
		this.setResponse(response);
		this.setEntity(entity);
	}

	/**
	 * Getter for entity
	 * @return the entity
	 */
	public String getEntity() {
		return entity;
	}

	/**
	 * Setter for entity
	 * @param entity
	 */
	public void setEntity(String entity) {
		this.entity = entity;
	}

	/**
	 * Getter for the HttpResponse
	 * @return the HttpResponse
	 */
	public HttpResponse getResponse() {
		return response;
	}

	/**
	 * Setter for the HttpResponse
	 * @param response
	 */
	public void setResponse(HttpResponse response) {
		this.response = response;
	}
}
