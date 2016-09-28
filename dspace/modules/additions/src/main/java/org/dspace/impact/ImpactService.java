package org.dspace.impact;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import com.google.gson.JsonObject;

/**
 * Abstract Impact Service so that other citation counts can be added later.
 * 
 * @author Genevieve Turner
 *
 */
public abstract class ImpactService {
	public void query() {
		
	}
	
	abstract public void findCitationInformation() throws ParserConfigurationException, 
			TransformerException, IOException, SAXException;
	
	/**
	 * Get the citation count for this Impact Service
	 * 
	 * @return The citation count
	 */
	abstract public Integer getCitationCount();
	
	/**
	 * Get the service link from the vendor for the citation count
	 * @return
	 */
	abstract public String getLinkBack();
	
	/**
	 * Get a json object of the citation count and link url.
	 * 
	 * @return The json object
	 */
	public JsonObject getJsonObject() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("citationCount", getCitationCount());
		jsonObject.addProperty("linkBack", getLinkBack());
		return jsonObject;
	}
	
}
