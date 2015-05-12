package org.dspace.app.webui.reference;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.PluginManager;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class EndNoteReferenceExport extends ReferenceExport {
	private DSpaceObject dso;
	
	public EndNoteReferenceExport(DSpaceObject dso) {
		this.dso = dso;
	}

	@Override
	public String getContentType() {
		return "application/xml; charset=utf-8";
	}

	@Override
	public String getPostfix() {
		return "xml";
	}

	@Override
	public void export(Writer writer) throws IOException {
		// TODO Auto-generated method stub
		DisseminationCrosswalk xwalk = (DisseminationCrosswalk) PluginManager.getNamedPlugin(DisseminationCrosswalk.class, "EndNote");
		
		try {
			Element element = xwalk.disseminateElement(dso);
			Format format = Format.getCompactFormat();
			format.setEncoding("UTF-8");
			format.setOmitDeclaration(false);
			format.setOmitEncoding(false);
			
			XMLOutputter xout = new XMLOutputter(format);
			writer.write(xout.outputString(element));
		} catch (CrosswalkException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (AuthorizeException e) {
			e.printStackTrace();
		}
		//xwalk.disseminateElement(dso)
	}

}
