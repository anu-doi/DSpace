package org.dspace.app.webui.util;

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.RequestItemServlet;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

public class RequestItemRedirection {
    private static Logger log = Logger.getLogger(RequestItemRedirection.class);
    
    private static java.util.Map<String, String> redirections;
    
    public String getRedirectForItem(Item item) throws SQLException {
    	Collection owningCollection = item.getOwningCollection();
    	String redirection = getFromMap(owningCollection.getHandle());
    	if (redirection == null) {
	    	Community[] communities = item.getCommunities();
	    	for (int i = 0; redirection == null && i < communities.length; i++) {
	    		redirection = getFromMap(communities[i].getHandle());
	    	}
    	}
    	return redirection;
    }

    private void readKeyRedirectionConfig() {
       redirections = new HashMap<String, String>();
        
       Enumeration<String> e = (Enumeration<String>)ConfigurationManager.propertyNames();

       while (e.hasMoreElements())
       {
           String key = e.nextElement();

           if (key.startsWith("request.item.redirect.")
                   && key.endsWith(".handles"))
           {
               String styleName = key.substring("request.item.redirect.".length(),
                       key.length() - ".handles".length());
               String[] handles = ConfigurationManager.getProperty(key)
                       .split(",");

               for (int i = 0; i < handles.length; i++)
               {
            	   redirections.put(handles[i].trim(), styleName.toLowerCase());
               }
           }
       }
    }

    public String getFromMap(String handle)
    {
    	if (redirections == null) {
    		readKeyRedirectionConfig();
    		log.info("Number of stored handles: "+redirections.size());
    	}
    	log.info("Attempting to get redirect url for "+handle);
    	String redirectionStyle = (String) redirections.get(handle);
    	log.info("Redirect style for handle "+handle+" is "+redirectionStyle);
    	
    	if (redirectionStyle == null)
    	{
    		return null;
    	}
    	
    	return getConfigurationForStyle(redirectionStyle);
    }
    
    public String getConfigurationForStyle(String redirectionStyle)
    {
        return ConfigurationManager.getProperty("request.item.redirect." + redirectionStyle + ".uri");
    }
}
