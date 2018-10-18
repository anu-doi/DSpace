package org.dspace.identifier.doi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.dspace.authority.util.XMLUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.services.ConfigurationService;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ARDCConnector {
    private static final Logger log = LoggerFactory.getLogger(ARDCConnector.class);
    
	private static final String APP_ID = "ardc.appid";
	private static final String SECRET = "ardc.secret";
	private static final String ARDC_PREFIX = "ardc.urlprefix";
	
	protected String serviceLocation;
	protected String responseType;
	protected ConfigurationService configurationService;
	protected DisseminationCrosswalk crosswalk;
	
	protected String crosswalkName;
	
	@Required
	public void setServiceLocation(String serviceLocation) {
		this.serviceLocation = serviceLocation;
	}
	
	@Required
	public void setResponseType(String responseType) {
		this.responseType = responseType;
	}
	
    @Autowired
    @Required
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

	public void setCrosswalkName(String crosswalkName) {
		this.crosswalkName = crosswalkName;
	}
	
	protected void prepareCrosswalk() {
		if (crosswalk == null) {
			crosswalk = (DisseminationCrosswalk) PluginManager
					.getNamedPlugin(DisseminationCrosswalk.class, crosswalkName);
			if (crosswalk == null) {
				throw new RuntimeException("Cant find crosswalk '" + crosswalkName + "'.");
			}
		}
	}
	
	public String mintDOI(Context context, DSpaceObject dso) throws DOIIdentifierException {
		// TODO check if already registered
		
		prepareCrosswalk();
		if (!crosswalk.canDisseminate(dso)) {
			log.error("Crosswalk " + crosswalkName + " cannot disseminate DSO with type " + dso.getType() +
					" and ID " + dso.getID());
		}
		Element root = null;
		try {
			root = crosswalk.disseminateElement(dso);
		}
		catch (AuthorizeException e) {
			log.error("Caught an AuthorizeException while disseminating DOS with type " + dso.getType() + 
					" and ID " + dso.getID() + ".");
		}
		catch (CrosswalkException e) {
			log.error("Caught a CrosswalkException for the DSO with type " + dso.getType() + 
					" and ID " + dso.getID() + ".");
		}
		catch (IOException | SQLException e) {
			throw new RuntimeException(e);
		}
		HttpPost httpPost = null;
		try {
			URIBuilder uriBuilder = new URIBuilder(serviceLocation);
			String mintPath = "mint." + responseType;
			uriBuilder.setPath(uriBuilder.getPath() + mintPath);
			String doiUrl = configurationService.getProperty(ARDC_PREFIX) + "/handle/" + dso.getHandle();
			uriBuilder.addParameter("url", doiUrl);
			httpPost = new HttpPost(uriBuilder.build());
			
			Document responseDocument = sendPost(httpPost, root);
			if (null == responseDocument) {
				throw new DOIIdentifierException("Response is null");
			}
			try {
				Node response = XMLUtils.getNode(responseDocument, "response");
				String responseCode = XMLUtils.getTextContent(response, "responsecode");
				
				if (!"MT001".equals(responseCode)) {
					String message = generateMintingErrorMessage(response, responseCode);
					throw new DOIIdentifierException(message);
					
				}
				String doi = XMLUtils.getTextContent(response, "doi");
				log.info("DOI minted for item " + dso.getID() + " , the doi is " + doi);
				return doi;
			}
			catch (XPathExpressionException e) {
				log.error("Exception retreiving information from xml", e);
			}
		}
		catch(URISyntaxException | AuthenticationException | IOException e) {
			log.error("Exception minting doi", e);
			throw new RuntimeException("Exception minting doi", e);
		}
		return null;
	}
	
	public boolean deactivateDOI(Context context, String doi) throws DOIIdentifierException {
		try {
			URIBuilder uriBuilder = new URIBuilder(serviceLocation);
			String deactivatePath = "deactivate." + responseType;
			uriBuilder.setPath(uriBuilder.getPath() + deactivatePath);
			uriBuilder.addParameter("doi", doi);
			HttpGet httpGet = new HttpGet(uriBuilder.build());
			Document responseDocument = sendRequest(httpGet);
			
			Node response = XMLUtils.getNode(responseDocument, "response");
			String responseCode = XMLUtils.getTextContent(response, "responsecode");
			
			if (!"MT003".equals(responseCode)) {
				String message = generateMintingErrorMessage(response, responseCode);
				throw new DOIIdentifierException(message);
			}
			return true;
		}
		catch(URISyntaxException | AuthenticationException | IOException | XPathExpressionException e) {
			log.error("Exception minting doi", e);
			throw new RuntimeException("Exception minting doi", e);
		}
	}
	
	public boolean activateDOI(Context context, String doi) throws DOIIdentifierException {
		try {
			URIBuilder uriBuilder = new URIBuilder(serviceLocation);
			String deactivatePath = "activate." + responseType;
			uriBuilder.setPath(uriBuilder.getPath() + deactivatePath);
			uriBuilder.addParameter("doi", doi);
			HttpGet httpGet = new HttpGet(uriBuilder.build());
			Document responseDocument = sendRequest(httpGet);

			Node response = XMLUtils.getNode(responseDocument, "response");
			String responseCode = XMLUtils.getTextContent(response, "responsecode");
			
			if (!"MT004".equals(responseCode)) {
				String message = generateMintingErrorMessage(response, responseCode);
				throw new DOIIdentifierException(message);
				
			}
			return true;
		}
		catch(URISyntaxException | AuthenticationException | IOException | XPathExpressionException e) {
			log.error("Exception minting doi", e);
			throw new RuntimeException("Exception minting doi", e);
		}
	}
	
	public boolean updateDOI(Context context, String doi, DSpaceObject dso) throws DOIIdentifierException {
		prepareCrosswalk();
		if (!crosswalk.canDisseminate(dso)) {
			log.error("Crosswalk " + crosswalkName + " cannot disseminate DSO with type " + dso.getType() +
					" and ID " + dso.getID());
		}
		Element root = null;
		try {
			root = crosswalk.disseminateElement(dso);
		}
		catch (AuthorizeException e) {
			log.error("Caught an AuthorizeException while disseminating DOS with type " + dso.getType() + 
					" and ID " + dso.getID() + ".");
		}
		catch (CrosswalkException e) {
			log.error("Caught a CrosswalkException for the DSO with type " + dso.getType() + 
					" and ID " + dso.getID() + ".");
		}
		catch (IOException | SQLException e) {
			throw new RuntimeException(e);
		}
		HttpPost httpPost = null;
		try {
			URIBuilder uriBuilder = new URIBuilder(serviceLocation);
			String updatePath = "update." + responseType;
			uriBuilder.setPath(uriBuilder.getPath() + updatePath);
			uriBuilder.addParameter("doi", doi);
			String doiUrl = configurationService.getProperty(ARDC_PREFIX) + "/handle/" + dso.getHandle();
			uriBuilder.addParameter("url", doiUrl);
			httpPost = new HttpPost(uriBuilder.build());
			
			Document responseDocument = sendPost(httpPost, root);
			try {
				Node response = XMLUtils.getNode(responseDocument, "response");
				String responseCode = XMLUtils.getTextContent(response, "responsecode");
				
				if (!"MT002".equals(responseCode)) {
					String message = generateMintingErrorMessage(response, responseCode);
					throw new DOIIdentifierException(message);
				}
				return true;
			}
			catch (XPathExpressionException e) {
				log.error("Exception retreiving information from xml", e);
			}
		}
		catch(URISyntaxException | AuthenticationException | IOException e) {
			log.error("Exception minting doi", e);
			throw new RuntimeException("Exception minting doi", e);
		}
		return false;
	}
	
	public Document sendPost(HttpPost httpPost, Element rootElement) throws IOException, AuthenticationException {
		Format format = Format.getCompactFormat();
		format.setEncoding("UTF-8");
		XMLOutputter xmlOut = new XMLOutputter(format);
		String document = xmlOut.outputString(new org.jdom.Document(rootElement));
		
		return sendPost(httpPost, document);
	}
	
	public Document sendPost(HttpPost httpPost, String document) throws IOException, AuthenticationException {
		HttpEntity entity = new ByteArrayEntity(document.getBytes());
		httpPost.setEntity(entity);
		return sendRequest(httpPost);
	}
	
	public Document sendRequest(HttpUriRequest request) throws IOException, AuthenticationException {
		String appId = configurationService.getProperty(APP_ID);
		String secret = configurationService.getProperty(SECRET);
		
		CloseableHttpClient client = HttpClients.createDefault();
		
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(appId, secret);
		request.addHeader(new BasicScheme().authenticate(credentials, request, null));
		CloseableHttpResponse response = client.execute(request);
		//TODO stuff with the status?
		HttpEntity entity = response.getEntity();
		// For some reason there are multiple types of Dom uses... We want the w3c one so that we can
		// use the utils to extract info from it
		
		Document doc = XMLUtils.convertStreamToXML(entity.getContent());
		
		client.close();
		return doc;
	}
	
	private String generateMintingErrorMessage(Node response, String responseCode) throws XPathExpressionException {
		String message = XMLUtils.getTextContent(response, "message");
		String verboseMessage = XMLUtils.getTextContent(response, "verbosemessage");
		String exceptionMessage = "Exception processing doi. Code: " + responseCode + ", Message: " + message;
		if (StringUtils.isNotEmpty(verboseMessage)) {
			exceptionMessage = exceptionMessage + "\n" + verboseMessage;
		}
		return exceptionMessage;
	}
}
