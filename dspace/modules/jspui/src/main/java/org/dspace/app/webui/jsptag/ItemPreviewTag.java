/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import org.dspace.app.webui.util.UIUtil;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;

/**
 * <p>
 * JSP tag for displaying a preview version of an item. For this tag to
 * output anything, the preview feature must be activated in DSpace.
 * </p>
 * 
 * @author Scott Yeadon
 * @version $Revision$
 */
public class ItemPreviewTag extends TagSupport
{
    /** Item to display */
    private transient Item item;

    private static final long serialVersionUID = -5535762797556685631L;

    public ItemPreviewTag()
    {
        super();
    }

    public int doStartTag() throws JspException
    {
    	if (!ConfigurationManager.getBooleanProperty("webui.preview.enabled"))
    	{
    		return SKIP_BODY;
    	}
        try
        {
        	showPreview();
        }
        catch (SQLException sqle)
        {
            throw new JspException(sqle);
        }
        catch (IOException ioe)
        {
            throw new JspException(ioe);
        }

        return SKIP_BODY;
    }

    public void setItem(Item itemIn)
    {
        item = itemIn;
    }
    
    private void showPreview() throws SQLException, IOException
    {
        JspWriter out = pageContext.getOut();
        
        // Only shows 1 preview image at the moment (the first encountered) regardless
        // of the number of bundles/bitstreams of this type
        Bundle[] bundles = item.getBundles("BRANDED_PREVIEW");
        Bundle[] largePreviewBundles = item.getBundles("LARGE_BRANDED_PREVIEW");
        
        if (bundles.length > 0)
        {
        	Bitstream[] bitstreams = bundles[0].getBitstreams();
        	
            HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
            Context context = UIUtil.obtainContext(request);

        	Bitstream preview = bitstreams[0];
        	String previewName = preview.getName();
        	int lastIndex = previewName.lastIndexOf(".");
        	// We want to provide an option for having a higher resolution image for zooming
        	String largePreviewName = preview.getName().substring(0, lastIndex) + ".large" + preview.getName().substring(lastIndex);
        	String largePreviewSource = null;
        	if (largePreviewBundles.length > 0) {
        		Bitstream largePreview = largePreviewBundles[0].getBitstreamByName(largePreviewName);
        		if (largePreview != null && AuthorizeManager.authorizeActionBoolean(context, largePreview, Constants.READ)) {
    				largePreviewSource = request.getContextPath() + "/retrieve/" 
    						+ largePreview.getID() + "/" + UIUtil.encodeBitstreamName(largePreview.getName());
        		}
        	}
        	if (AuthorizeManager.authorizeActionBoolean(context, preview, Constants.READ)) {
        		out.println("<link href=\""+request.getContextPath()+"/css/viewer.min.css\" rel=\"stylesheet\" type=\"text/css\" media=\"screen\" />");
        		out.println("<script type=\"text/javascript\" src=\""+request.getContextPath()+"/js/viewer.min.js\"></script>");
	            out.println("<br/><p align=\"center\">");
	            out.print("<div><img id=\"preview\" src=\""
	            		    + request.getContextPath() + "/retrieve/"
	            		    + bitstreams[0].getID() + "/"
	            		    + UIUtil.encodeBitstreamName(bitstreams[0].getName(),
	            		    		  Constants.DEFAULT_ENCODING));
	            if (largePreviewSource != null) {
	            	out.print("\" data-original=\""
	            			+ largePreviewSource);
	            	out.print("\" alt=\""+bitstreams[0].getName());
	            }
	            out.print("\"/></div>\n");
	            out.println("<script type=\"text/javascript\">");
	            out.println("window.onload = function () {");
	            out.println("'use strict';");
	            out.println("var Viewer = window.Viewer;");
	            out.println("var pictures = document.getElementById(\"preview\");");
	            out.println("var options = {");
	            out.println("url: 'data-original',");
	            out.println("inline: false,");
	            out.println("};");
	            out.println("var viewer;");
	            out.println("viewer = new Viewer(pictures, options);");
	            out.println("}");
	            out.println("</script>");
	            
//	            out.println("<script src=\""+request.getContextPath()+"/js/wheelzoom.js\"></script>\n");
////	            out.println("<script src=\"wheelzoom.js\"></script>\n");
//	            out.println("<script>wheelzoom(document.querySelector(\"img.zoom\"));</script>\n");
	            
	            // Currently only one metadata item supported. Only the first match is taken
	//            String s = ConfigurationManager.getProperty("webui.preview.dc");
	//            if (s != null)
	//            {
	//            	Metadatum[] dcValue;
	//            	
	//            	int i = s.indexOf('.');
	//            	
	//            	if (i == -1)
	//            	{
	//            		dcValue = item.getDC(s, Item.ANY, Item.ANY);
	//            	}
	//            	else
	//            	{
	//            		dcValue = item.getDC(s.substring(0,1), s.substring(i + 1), Item.ANY);
	//            	}
	//            	
	//            	if (dcValue.length > 0)
	//            	{
	//            		out.println("<br/>" + dcValue[0].value);
	//            	}
	//            }
	            
	            out.println("</p>");
        	}
        }     
    }

    public void release()
    {
        item = null;
    }
}
