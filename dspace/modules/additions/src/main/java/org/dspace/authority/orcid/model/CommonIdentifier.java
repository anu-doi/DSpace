package org.dspace.authority.orcid.model;

public class CommonIdentifier {
	private String uri;
	private String path;
	private String host;
	
	public CommonIdentifier() {
		
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}
}
