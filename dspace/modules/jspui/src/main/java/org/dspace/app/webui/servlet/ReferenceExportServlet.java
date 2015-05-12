package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.reference.BibTexReferenceExport;
import org.dspace.app.webui.reference.EndNoteReferenceExport;
import org.dspace.app.webui.reference.ReferenceExport;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

public class ReferenceExportServlet extends DSpaceServlet {
	private static final long serialVersionUID = 1L;
	
	private static Logger log = Logger.getLogger(ReferenceExportServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	String handle = request.getParameter("handle");
    	String format = request.getParameter("format");
    	
    	if (handle != null)
    	{
            //log.info(LogManager.getHeader(context, "metadataexport", "exporting_handle:" + handle));
            DSpaceObject thing = HandleManager.resolveToObject(context, handle);
    		if (thing != null)
    		{
    			ReferenceExport exporter = null;
    			if ("bibtex".equals(format)) {
    				exporter = new BibTexReferenceExport(context, thing);
    			}
    			else if ("endnote".equals(format)) {
    				exporter = new EndNoteReferenceExport(thing);
    			}
    			response.setContentType(exporter.getContentType());
    			String filename = handle.replaceAll("/", "-") + "." + exporter.getPostfix();
    			response.setHeader("Content-Disposition", "attachment); filename=" + filename);
    			PrintWriter out = response.getWriter();
    			exporter.export(out);
    			out.flush();
    			out.close();
    			return;
    		}
    	}
    	
        // Something has gone wrong
        JSPManager.showIntegrityError(request, response);
    }
}
