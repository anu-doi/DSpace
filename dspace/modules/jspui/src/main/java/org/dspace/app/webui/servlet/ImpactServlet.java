package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.impact.ImpactService;
import org.dspace.impact.WebOfScienceImpactService;
import org.xml.sax.SAXException;

import com.google.gson.JsonObject;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Servlet for retrieving citation counts
 * 
 * @author Genevieve Turner
 *
 */
public class ImpactServlet extends DSpaceServlet {
    private static Logger log = Logger.getLogger(ImpactServlet.class);
    
	private static final long serialVersionUID = 1L;

	public void init()
	{
		Cache impactCache = new Cache("impactCache", 2000, Boolean.FALSE, Boolean.FALSE, 3600*24*7,0);
		CacheManager.getInstance().addCache(impactCache);
	}
	
	protected void doDSGet(Context context, HttpServletRequest request, 
			HttpServletResponse response) throws ServletException, 
			IOException, SQLException, AuthorizeException
	{
		doDSPost(context, request, response);
		return;
	}
	
	protected void doDSPost(Context context, HttpServletRequest request, 
			HttpServletResponse response) throws ServletException, 
			IOException, SQLException, AuthorizeException
	{
		ImpactService impactService = getImpactService(context, request, response);
		JsonObject object = impactService.getJsonObject();
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.write(object.toString());
		
		out.flush();
		out.close();
		return;
	}
	
	protected ImpactService getImpactService(Context context, HttpServletRequest request, 
			HttpServletResponse response) {
		String service = request.getParameter("service");
		String doi = request.getParameter("doi");
		String cacheKey = service + ":" + doi;
		Cache cache = CacheManager.getInstance().getCache("impactCache");
		Element cachedElement = cache.get(cacheKey);
		ImpactService impactService = null;
		if (cachedElement == null) {
			if ("wos".equals(service)) {
				log.debug("Retrieving web of science citation counts for service " + service + " with doi " + doi);
				impactService = new WebOfScienceImpactService(doi);
				try {
					impactService.findCitationInformation();
					cachedElement = new Element(cacheKey, impactService);
					cache.put(cachedElement);
				}
				catch (ParserConfigurationException | TransformerException | IOException | SAXException e) {
					log.error("Error retrieving web of science citation count", e);
				}
			}
		}
		else {
			log.debug("Retrieving citation counts from cache for service " + service + " with doi " + doi);
			impactService = (ImpactService) cachedElement.getObjectValue();
		}
		return impactService;
	}
}
