package org.dspace.impact;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Web of Science citation count service
 * 
 * @author Genevieve Turner
 *
 */
public class WebOfScienceImpactService extends ImpactService {
    private static Logger log = Logger.getLogger(WebOfScienceImpactService.class);
    
	private static final String NS = "http://www.isinet.com/xrpc42";
	private String doi;
	private Integer timesCited = 0;
	private String linkBack = null;
	
	/**
	 * Constructor
	 * 
	 * @param doi The doi to find citation counts for
	 */
	public WebOfScienceImpactService(String doi) {
		this.doi = doi;
	}

	@Override
	public void findCitationInformation() throws ParserConfigurationException, 
			TransformerException, IOException, SAXException {
		HttpPost method = null;
		Document requestDoc = getRequestDocument();
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(requestDoc);
		Writer writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		transformer.transform(source, result);
		
		StringEntity entity = new StringEntity(writer.toString());
		if (log.getLevel() == Level.DEBUG) {
			log.debug("Sending Request: " + writer.toString());
		}
		
		method = new HttpPost("https://ws.isiknowledge.com/cps/xrpc");
		method.setEntity(entity);
		HttpClient client = HttpClients.createDefault();
		HttpResponse response = client.execute(method);
		response.getStatusLine();
		
		HttpEntity entity2 = response.getEntity();
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(entity2.getContent());
		NodeList nodes = doc.getElementsByTagName("val");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element element = (Element)nodes.item(i);
			String name = element.getAttribute("name");
			String value = element.getTextContent();
			if ("timesCited".equals(name)) {
				try {
					timesCited = new Integer(value);
				}
				catch (NumberFormatException e) {
					log.error("Error formatting number", e);
				}
			}
			else if ("citingArticlesURL".equals(name)) {
				linkBack = value;
			}
		}
	}
	
	/**
	 * Generate the document to send as a request to Web of Science
	 * 
	 * @return The web of science request document
	 * @throws ParserConfigurationException
	 */
	private Document getRequestDocument() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = factory.newDocumentBuilder();
		Document doc = db.newDocument();
		
		Element rootElement = doc.createElementNS(NS, "request");
		rootElement.setAttributeNS(NS, "src", "app.id=PartnerApp,env.id=PartnerAppEnv,partner.email=EmailAddress");
		doc.appendChild(rootElement);
		
		Element fn = doc.createElementNS(NS, "fn");
		fn.setAttributeNS(NS, "name", "LinksAMR.retrieve");
		rootElement.appendChild(fn);
		
		Element list = doc.createElementNS(NS, "list");
		fn.appendChild(list);
		
		Element authMap = doc.createElementNS(NS, "map");
		list.appendChild(authMap);
		
		Element fieldMap = doc.createElementNS(NS, "map");
		list.appendChild(fieldMap);
		
		Element fieldList = doc.createElementNS(NS, "list");
		fieldList.setAttributeNS(NS, "name", "WOS");
		fieldMap.appendChild(fieldList);
		
		Element timesCited = doc.createElementNS(NS, "val");
		timesCited.setTextContent("timesCited");
		fieldList.appendChild(timesCited);
		
		Element citingURL = doc.createElementNS(NS, "val");
		citingURL.setTextContent("citingArticlesURL");
		fieldList.appendChild(citingURL);
		
		Element citationMap = doc.createElementNS(NS, "map");
		list.appendChild(citationMap);
		
		Element cit1Map = doc.createElementNS(NS, "map");
		cit1Map.setAttributeNS(NS, "name", "cite_1");
		citationMap.appendChild(cit1Map);
		
		Element doiVal = doc.createElementNS(NS, "val");
		doiVal.setAttributeNS(NS, "name", "doi");
		doiVal.setTextContent(doi);
		cit1Map.appendChild(doiVal);
		
		return doc;
	}
	
	@Override
	public Integer getCitationCount() {
		return timesCited;
	}
	
	@Override
	public String getLinkBack() {
		return linkBack;
	}
}
