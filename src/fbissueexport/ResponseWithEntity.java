package fbissueexport;

import org.apache.http.HttpResponse;

public class ResponseWithEntity {
	private HttpResponse response;
	private String entity;
	
	public ResponseWithEntity(HttpResponse response, String entity) {
		this.setResponse(response);
		this.setEntity(entity);
	}

	public String getEntity() {
		return entity;
	}

	public void setEntity(String entity) {
		this.entity = entity;
	}

	public HttpResponse getResponse() {
		return response;
	}

	public void setResponse(HttpResponse response) {
		this.response = response;
	}
}
