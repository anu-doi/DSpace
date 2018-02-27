package org.dspace.authority.orcid.xml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.dspace.authority.orcid.model.CommonIdentifier;
import org.dspace.authority.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XMLtoIdentifier extends Converter {
    private static Logger log = Logger.getLogger(XMLtoIdentifier.class);
	
	private static final NamespaceContext nsContext = new OrcidNamespace();
	protected String ORCID = "//common:orcid-identifier";
	protected String URI = "common:uri";
	protected String PATH = "common:path";
	protected String HOST = "common:host";

    public List<CommonIdentifier> convert(Document xml) {
    	List<CommonIdentifier> result = new ArrayList<CommonIdentifier>();
    	if (XMLErrors.check(xml)) {
    		try {
    			Iterator<Node> iterator = XMLUtils.getNodeListIterator(xml, ORCID, nsContext);
    			while (iterator.hasNext()) {
    				CommonIdentifier identifier = convertIdentifier(iterator.next());
    				result.add(identifier);
    			}
    		}
    		catch (XPathExpressionException e) {
    			
    		}
    	}
    	
    	return result;
    }
    
    private CommonIdentifier convertIdentifier(Node node) {
    	CommonIdentifier identifier = new CommonIdentifier();
    	
    	setURI(node, identifier);
    	setPath(node, identifier);
    	setHost(node, identifier);
    	
    	return identifier;
    }
    
    private void setURI(Node node, CommonIdentifier identifier) {
    	try {
    		String uri = XMLUtils.getTextContent(node, URI);
    		identifier.setUri(uri);
    	}
    	catch (XPathExpressionException e) {
    		log.debug("Error in finding the uri in this bio xml.", e);
    	}
    }
    
    private void setPath(Node node, CommonIdentifier identifier) {
    	try {
    		String path = XMLUtils.getTextContent(node, PATH);
    		identifier.setPath(path);
    	}
    	catch (XPathExpressionException e) {
    		log.debug("Error in finding the path in this bio xml.", e);
    	}
    }
    
    private void setHost(Node node, CommonIdentifier identifier) {
    	try {
    		String host = XMLUtils.getTextContent(node, HOST);
    		identifier.setHost(host);
    	}
    	catch (XPathExpressionException e) {
    		log.debug("Error in finding the host in this bio xml.", e);
    	}
    }
}
