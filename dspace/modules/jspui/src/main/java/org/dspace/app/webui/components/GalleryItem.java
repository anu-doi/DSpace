package org.dspace.app.webui.components;

/**
 * @author Osama Alkadi
 */
public class GalleryItem {
	
	private String url;
	private String title;
	private String handle;
	private String next;
	private String prev;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

	public String getNext() {
		return next;
	}

	public void setNext(String next) {
		this.next = next;
	}

	public String getPrev() {
		return prev;
	}

	public void setPrev(String prev) {
		this.prev = prev;
	}

}
