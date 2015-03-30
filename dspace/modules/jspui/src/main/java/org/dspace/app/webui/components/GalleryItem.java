package org.dspace.app.webui.components;

import lombok.Getter;
import lombok.Setter;


/**
 * @author Osama Alkadi
 */
public class GalleryItem {
	
	@Getter @Setter private String url;
	@Getter @Setter private String title;
	@Getter @Setter private String handle;
	@Getter @Setter private String next;
	@Getter @Setter private String prev;
}
