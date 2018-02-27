package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.xml.serializer.Method;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.rest.RestSource;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.ChoicesXMLGenerator;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

public class OrcidServlet extends DSpaceServlet {
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(OrcidServlet.class);
	
    private RestSource source = new DSpace().getServiceManager().getServiceByName("AuthoritySource", RestSource.class);
    
	@Override
	protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
		process(context, request, response);
	}
	
	@Override
	protected void doDSPost(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
		process(context, request, response);
	}
	private void process(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
		String orcid = request.getParameter("orcid");
		
		AuthorityValue value = source.queryAuthorityID(orcid);
		Choice[] choiceValues = {new Choice(value.generateString(), value.getValue(), value.getValue(), value.choiceSelectMap())};
		Choices choices = new Choices(choiceValues, 0, 1, 600, Boolean.FALSE);

		response.setContentType("text/xml; charset=\"utf-8\"");
		Writer writer = response.getWriter();
        Properties props =
		OutputPropertiesFactory.getDefaultMethodProperties(Method.XML);
		Serializer ser = SerializerFactory.getSerializer(props);
		ser.setWriter(writer);
		try {
			ChoicesXMLGenerator.generate(choices, "select", ser.asContentHandler());
		}
		catch (SAXException e) {
			log.error("Exception generating choices", e);
		}
		writer.flush();
	}
}
