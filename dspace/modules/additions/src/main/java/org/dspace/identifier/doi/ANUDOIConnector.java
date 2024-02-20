package org.dspace.identifier.doi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.crosswalk.ParameterizedDisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.ConfigurationService;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ANUDOIConnector {
	private static final Logger log = LoggerFactory.getLogger(ANUDOIConnector.class);
	
	static final String CFG_USER = "identifier.doi.user";
	static final String CFG_PASSWORD = "identifier.doi.password";
	static final String CFG_PREFIX = "identifier.doi.prefix";
	static final String CFG_PUBLISHER = "crosswalk.dissemination.DataCite.publisher";
	static final String CFG_DATAMANAGER = "crosswalk.dissemination.DataCite.dataManager";
	static final String CFG_HOSTINGINSTITUTION = "crosswalk.dissemination.DataCite.hostingInstitution";
	static final String CFG_NAMESPACE = "crosswalk.dissemination.DataCite.namespace";
	//	static final String CFG_HOST = "identifier.doi.resolveurl.prefix";
	static final String CFG_HOST = "identifier.doi.service.host";
	static final String DOI_PREFIX = "identifier.doi.host";
	static final String URL_PREFIX = "identifier.doi.url.prefix";
	
	
	protected String SCHEME;
	protected String HOST;
	protected String DOI_PATH;
	protected String METADATA_PATH;
	protected String CROSSWALK_NAME;

	protected ParameterizedDisseminationCrosswalk crosswalk;
	
	protected ConfigurationService configurationService;
	
	protected String USERNAME;
	protected String PASSWORD;
	
	public ANUDOIConnector() {
		this.crosswalk = null;
		this.USERNAME = null;
		this.PASSWORD = null;
	}
	
	@Autowired(required = true)
	public void setDATACITE_SCHEME(String DATACITE_SCHEME) {
		this.SCHEME = DATACITE_SCHEME;
	}

	@Autowired(required = true)
	public void setDATACITE_DOI_PATH(String DATACITE_DOI_PATH) {
		if (!DATACITE_DOI_PATH.startsWith("/")) {
			DATACITE_DOI_PATH = "/" + DATACITE_DOI_PATH;
		}
		if (!DATACITE_DOI_PATH.endsWith("/")) {
			DATACITE_DOI_PATH = DATACITE_DOI_PATH + "/";
		}

		this.DOI_PATH = DATACITE_DOI_PATH;
	}

	@Autowired(required = true)
	public void setDATACITE_METADATA_PATH(String DATACITE_METADATA_PATH) {
		if (!DATACITE_METADATA_PATH.startsWith("/")) {
			DATACITE_METADATA_PATH = "/" + DATACITE_METADATA_PATH;
		}
		if (!DATACITE_METADATA_PATH.endsWith("/")) {
			DATACITE_METADATA_PATH = DATACITE_METADATA_PATH + "/";
		}

		this.METADATA_PATH = DATACITE_METADATA_PATH;
	}
	
	@Autowired(required = true)
	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}
	
	@Autowired(required = true)
	public void setDisseminationCrosswalkName(String CROSSWALK_NAME) {
		this.CROSSWALK_NAME = CROSSWALK_NAME;
	}

	public void deleteDOI(Context context, String doi) throws DOIIdentifierException {
		URIBuilder uriBuilder = new URIBuilder();
		String doiPath = METADATA_PATH + doi;
		uriBuilder.setScheme(SCHEME).setHost(getHost()).setPath(doiPath);
		
		HttpDelete httpDelete = null;
		try {
			httpDelete = new HttpDelete(uriBuilder.build());
			sendHttpRequest(httpDelete, doi);
		}
		catch (URISyntaxException e) {
			log.error("The URL we constructed to delete a DOI produced a URISyntaxException. Please check the configuration parameters.");
			log.error("The URL was {}.", SCHEME + "://" + getHost() + doiPath);
			throw new RuntimeException("The URL we constructed to delete a DOI produced a URI Please check the configuration parameters.", e);
		}
	}

//	@Override
	public String reserveDOI(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException {
		// TODO Auto-generated method stub
		prepareCrosswalk();

		DSpaceObjectService<DSpaceObject> dSpaceObjectService = ContentServiceFactory.getInstance()
				.getDSpaceObjectService(dso);

		if (!this.crosswalk.canDisseminate(dso)) {
			log.error("Crosswalk " + CROSSWALK_NAME + " cannot disseminate DSO with type " + dso.getType() + " and ID "
					+ dso.getID() + ".");
			throw new DOIIdentifierException("Cannot disseminate " + dSpaceObjectService.getTypeText(dso) + "/" + dso.getID()
					+ " using crosswalk " + this.CROSSWALK_NAME + ".", DOIIdentifierException.CONVERSION_ERROR);
		}
		

		Map<String, String> parameters = new HashMap<>();
		if (configurationService.hasProperty(CFG_PREFIX)) {
			parameters.put("prefix", configurationService.getProperty(CFG_PREFIX));
		}
		if (configurationService.hasProperty(CFG_PUBLISHER)) {
			parameters.put("publisher", configurationService.getProperty(CFG_PUBLISHER));
		}
		if (configurationService.hasProperty(CFG_DATAMANAGER)) {
			parameters.put("datamanager", configurationService.getProperty(CFG_DATAMANAGER));
		}
		if (configurationService.hasProperty(CFG_HOSTINGINSTITUTION)) {
			parameters.put("hostinginstitution",
				configurationService.getProperty(CFG_HOSTINGINSTITUTION));
		}
		
		Element root = null;
		try {
			root = crosswalk.disseminateElement(context, dso, parameters);
			addEmptyDOI(root);
		}
		catch (AuthorizeException ae) {
			log.error("Cauth an AuthorizeException while disseminating DSO with type " + dso.getType() + " and ID "
					+ dso.getID() + ".");
			throw new DOIIdentifierException("AuthorizeException occured while converting " 
					+ dSpaceObjectService.getTypeText(dso) + "/" + dso.getID() + " using crosswalk " + this.CROSSWALK_NAME
					+ ".", ae, DOIIdentifierException.CONVERSION_ERROR);
		} catch (CrosswalkException ce) {
			log.error("Cauth an CrosswalkException while disseminating DSO with type " + dso.getType() + " and ID "
					+ dso.getID() + ".");
			throw new DOIIdentifierException("CrosswalkException occured while converting " 
					+ dSpaceObjectService.getTypeText(dso) + "/" + dso.getID() + " using crosswalk " + this.CROSSWALK_NAME
					+ ".", ce, DOIIdentifierException.CONVERSION_ERROR);
		} catch (IOException | SQLException ex) {
			throw new RuntimeException(ex);
		}
		
		HttpPut httpPut = null;
		try {
			URIBuilder uriBuilder = new URIBuilder();
			String doiPath = METADATA_PATH + configurationService.getProperty(CFG_PREFIX);
			uriBuilder.setScheme(SCHEME).setHost(getHost()).setPath(doiPath);
			httpPut = new HttpPut(uriBuilder.build());
			
			DataCiteResponse dataCiteResponse = sendPut(httpPut, root);
			switch (dataCiteResponse.getStatusCode()) {
				case(201): {
					String content = dataCiteResponse.getContent();
					log.debug("DataCite Response content: {}", content);
				//TODO check if this updates it for use afterwards
				doi = StringUtils.substringBetween(content, "(",")");
				log.info("DataCite generated the doi: {} for the object: {}", doi, dso.getID());
				return doi;
//				return;
			}
			case (422) : {
				log.warn("DataCite was unable to understand the XML we sent.");
				log.warn("DataCite Metadata API returned a http status code "
						+"422: " + dataCiteResponse.getContent());
				Format format = Format.getCompactFormat();
				format.setEncoding("UTF-8");
				XMLOutputter xout = new XMLOutputter(format);
				log.info("We send the following XML:\n" + xout.outputString(root));
				throw new DOIIdentifierException("Unable to reserve a DOI" 
						+ ". Please inform your administrator or take a look "
						+" into the log files.", DOIIdentifierException.BAD_REQUEST);
			}
			default: {
				log.warn("While reserving a DOI we got a http status code {} and the message \"{}\""
						, Integer.toString(dataCiteResponse.getStatusCode()), dataCiteResponse.getContent());
				throw new DOIIdentifierException("Unable to parse an answer from "
						+ "DataCite API. Please have a look into DSpace logs.",
						DOIIdentifierException.BAD_ANSWER);
				}
			}
		}
		catch (URISyntaxException e) {
			log.error("Exception minting doi", e);
			throw new RuntimeException("Exception minting doi", e);
		}
	}

	public void registerDOI(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException {
		HttpPut httpPut = null;
		try {
			URIBuilder uriBuilder = new URIBuilder();
			String doiPath = DOI_PATH + doi;
			uriBuilder.setScheme(SCHEME).setHost(getHost()).setPath(doiPath);
			httpPut = new HttpPut(uriBuilder.build());
			
			String handlePath = configurationService.getProperty(URL_PREFIX) + "/handle/" + dso.getHandle();
			String registerValue = "doi=" + doi + "\nurl=" + handlePath;
			log.info("Register value: {}", registerValue);
			DataCiteResponse response = sendPut(httpPut, registerValue);
			
			switch (response.getStatusCode()) {
				case(201): {
					return;
				}
				case (422): {
					log.warn("DataCite was unable to understand the data we sent.");
					log.warn("DataCite Metadata API returned a http status code 422: ", response.getContent());
					throw new DOIIdentifierException("Unable to reserve a DOI. Please inform your administrator"
							+ "or take a log into the log files.", DOIIdentifierException.BAD_REQUEST);
				}
				default: {
					log.warn("While reserving a DOI we got a http status code {} and the message \"{}\"."
						, Integer.toString(response.getStatusCode()), response.getContent());
					throw new DOIIdentifierException("Unable to parse an answer from DataCite API."
							+ "Please have a look into the DSpace logs.", DOIIdentifierException.BAD_ANSWER);
				}
			}
		}
		catch (URISyntaxException e) {
			log.error("Exception setting url for doi", e);
			throw new RuntimeException("Exception minting doi", e);
		}
		
	}

	public void updateMetadata(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException {
		// TODO Auto-generated method stub
		prepareCrosswalk();
		
		DSpaceObjectService<DSpaceObject> dSpaceObjectService = ContentServiceFactory.getInstance()
				.getDSpaceObjectService(dso);
		
		if (!crosswalk.canDisseminate(dso)) {
			log.error("Crosswalk " + CROSSWALK_NAME + " cannot disseminate DSO with type " + dso.getType()
					+ " and ID " + dso.getID());
		}
		

		Map<String, String> parameters = new HashMap<>();
		if (configurationService.hasProperty(CFG_PREFIX)) {
			parameters.put("prefix", configurationService.getProperty(CFG_PREFIX));
		}
		if (configurationService.hasProperty(CFG_PUBLISHER)) {
			parameters.put("publisher", configurationService.getProperty(CFG_PUBLISHER));
		}
		if (configurationService.hasProperty(CFG_DATAMANAGER)) {
			parameters.put("datamanager", configurationService.getProperty(CFG_DATAMANAGER));
		}
		if (configurationService.hasProperty(CFG_HOSTINGINSTITUTION)) {
			parameters.put("hostinginstitution",
				configurationService.getProperty(CFG_HOSTINGINSTITUTION));
		}
		
		Element root = null;
		try {
			root = crosswalk.disseminateElement(context, dso, parameters);
			root = addDOI(doi, root);
		}
		catch (AuthorizeException ae) {
			log.error("Caught an AuthorizeException while disseminating DSO with type " + dso.getType() + " and ID "
					+ dso.getID() + ".");
			throw new DOIIdentifierException("AuthorizeException occured while converting " 
					+ dSpaceObjectService.getTypeText(dso) + "/" + dso.getID() + " using crosswalk " + this.CROSSWALK_NAME
					+ ".", ae, DOIIdentifierException.CONVERSION_ERROR);
		} catch (CrosswalkException ce) {
			log.error("Cauth an CrosswalkException while disseminating DSO with type " + dso.getType() + " and ID "
					+ dso.getID() + ".");
			throw new DOIIdentifierException("CrosswalkException occured while converting " 
					+ dSpaceObjectService.getTypeText(dso) + "/" + dso.getID() + " using crosswalk " + this.CROSSWALK_NAME
					+ ".", ce, DOIIdentifierException.CONVERSION_ERROR);
		} catch (IOException | SQLException ex) {
			throw new RuntimeException(ex);
		}
		
		HttpPut httpPut = null;
		try {
			URIBuilder uriBuilder = new URIBuilder();
			String doiPath = METADATA_PATH + doi;
			uriBuilder.setScheme(SCHEME).setHost(getHost()).setPath(doiPath);
			httpPut = new HttpPut(uriBuilder.build());
			
			DataCiteResponse dataCiteResponse = sendPut(httpPut, root);
			//TODO remove
			if (null == dataCiteResponse) {
				return;
			}
			switch (dataCiteResponse.getStatusCode()) {
			case(201): {
				return;
			}
			case (422) : {
				log.warn("DataCite was unable to understand the XML we sent.");
				log.warn("DataCite Metadata API returned a http status code "
						+"422: " + dataCiteResponse.getContent());
				Format format = Format.getCompactFormat();
				format.setEncoding("UTF-8");
				XMLOutputter xout = new XMLOutputter(format);
				log.info("We send the following XML:\n" + xout.outputString(root));
				throw new DOIIdentifierException("Unable to reserve a DOI" 
						+ ". Please inform your administrator or take a look "
						+" into the log files.", DOIIdentifierException.BAD_REQUEST);
			}
			default: {
				log.warn("While reserving a DOI we got a http status code {} and the message \"{}\""
						, Integer.toString(dataCiteResponse.getStatusCode()), dataCiteResponse.getContent());
				throw new DOIIdentifierException("Unable to parse an answer from "
						+ "DataCite API. Please have a look into DSpace logs.",
						DOIIdentifierException.BAD_ANSWER);
				}
			}
		}
		catch (URISyntaxException e) {
			log.error("Exception minting doi", e);
			throw new RuntimeException("Exception minting doi", e);
		}
	}
	
	protected DataCiteResponse sendPut(HttpPut httpPut, Element rootElement) throws DOIIdentifierException {
		Format format = Format.getCompactFormat();
		format.setEncoding("UTF-8");
		XMLOutputter xmlOut = new XMLOutputter(format);
		String document = xmlOut.outputString(new Document(rootElement));
		return sendPut(httpPut, document);
	}
	
	protected DataCiteResponse sendPut(HttpPut httpPut, String document) throws DOIIdentifierException {
		HttpEntity entity =  new ByteArrayEntity(document.getBytes());
		httpPut.setEntity(entity);
		return sendHttpRequest(httpPut, null);
	}
	
	protected DataCiteResponse sendHttpRequest(HttpUriRequest req, String doi) throws DOIIdentifierException {
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(new AuthScope(HOST, 443),
				new UsernamePasswordCredentials(this.getUsername(), this.getPassword()));
        
        String proxyHost = configurationService.getProperty("http.proxy.host");
        String proxyPort = configurationService.getProperty("http.proxy.port");

        if (StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
        }

		HttpClientContext httpContext = HttpClientContext.create();
		httpContext.setCredentialsProvider(credentialsProvider);

		HttpEntity entity = null;
		try ( CloseableHttpClient httpclient = HttpClientBuilder.create().build(); ) {
			HttpResponse response = httpclient.execute(req, httpContext);

			StatusLine status = response.getStatusLine();
			int statusCode = status.getStatusCode();

			String content = null;
			entity = response.getEntity();
			if (null != entity) {
				content = EntityUtils.toString(entity, "UTF-8");
			}

			/* While debugging it can be useful to see which requests are sent:
			 *
			 * log.debug("Going to send HTTP request of type " + req.getMethod() + ".");
			 * log.debug("Will be send to " + req.getURI().toString() + ".");
			 * if (req instanceof HttpEntityEnclosingRequestBase)
			 * {
			 *	 log.debug("Request contains entity!");
			 *	 HttpEntityEnclosingRequestBase reqee = (HttpEntityEnclosingRequestBase) req;
			 *	 if (reqee.getEntity() instanceof StringEntity)
			 *	 {
			 *		 StringEntity se = (StringEntity) reqee.getEntity();
			 *		 try {
			 *			 BufferedReader br = new BufferedReader(new InputStreamReader(se.getContent()));
			 *			 String line = null;
			 *			 while ((line = br.readLine()) != null)
			 *			 {
			 *				 log.debug(line);
			 *			 }
			 *			 log.info("----");
			 *		 } catch (IOException ex) {
			 *
			 *		 }
			 *	 }
			 * } else {
			 *	 log.debug("Request contains no entity!");
			 * }
			 * log.debug("The request got http status code {}.", Integer.toString(statusCode));
			 * if (null == content)
			 * {
			 *	 log.debug("The response did not contain any answer.");
			 * } else {
			 *	 log.debug("DataCite says: {}", content);
			 * }
			 *
			 */

			// We can handle some status codes here, others have to be handled above
			switch (statusCode) {
				// we get a 401 if we forgot to send credentials or if the username
				// and password did not match.
				case (401): {
					log.info("We were unable to authenticate against the DOI registry agency.");
					log.info("The response was: {}", content);
					throw new DOIIdentifierException("Cannot authenticate at the "
														 + "DOI registry agency. Please check if username "
														 + "and password are set correctly.",
													 DOIIdentifierException.AUTHENTICATION_ERROR);
				}

				// We get a 403 Forbidden if we are managing a DOI that belongs to
				// another party or if there is a login problem.
				case (403): {
					log.info("Managing a DOI ({}) was prohibited by the DOI "
								 + "registration agency: {}", doi, content);
					throw new DOIIdentifierException("We can check, register or "
														 + "reserve DOIs that belong to us only.",
													 DOIIdentifierException.FOREIGN_DOI);
				}


				// 500 is documented and signals an internal server error
				case (500): {
					log.warn("Caught an http status code 500 while managing DOI "
								 + "{}. Message was: " + content);
					throw new DOIIdentifierException("DataCite API has an internal error. "
														 + "It is temporarily impossible to manage DOIs. "
														 + "Further information can be found in DSpace log file.",
													 DOIIdentifierException.INTERNAL_ERROR);
				}
				default:
					break;
			}


			return new DataCiteResponse(statusCode, content);
		} catch (IOException e) {
			log.warn("Caught an IOException: " + e.getMessage());
			throw new RuntimeException(e);
		} finally {
			try {
				// Release any resources used by HTTP-Request.
				if (null != entity) {
					EntityUtils.consume(entity);
				}
			} catch (IOException e) {
				log.warn("Can't release HTTP-Entity: " + e.getMessage());
			}
		}
		
	}
	
	protected void prepareCrosswalk() {
		if (null != this.crosswalk) {
			return;
		}
		
		crosswalk = (ParameterizedDisseminationCrosswalk) CoreServiceFactory.getInstance().getPluginService()
				.getNamedPlugin(DisseminationCrosswalk.class, this.CROSSWALK_NAME);
		
		if (crosswalk == null) {
			throw new RuntimeException("Can't find crosswalk '"
					+ CROSSWALK_NAME + "'!");
		}
	}
	
	protected String getUsername() {
		if (null == this.USERNAME) {
			this.USERNAME = this.configurationService.getProperty(CFG_USER);
			if (null == this.USERNAME) {
				throw new RuntimeException("Unable to load username from configuration. Cannot find property " + CFG_USER + ".");
			}
		}
		return this.USERNAME;
	}
	
	protected String getPassword() {
		if (null == this.PASSWORD) {
			this.PASSWORD = this.configurationService.getProperty(CFG_PASSWORD);
			if (null == this.PASSWORD) {
				throw new RuntimeException("Unable to load username from configuration. Cannot find property " + CFG_PASSWORD + ".");
			}
		}
		return this.PASSWORD;
	}
	
	protected String getHost() {
		if (null == this.HOST) {
			this.HOST = this.configurationService.getProperty(CFG_HOST);
			if (null == this.HOST) {
				throw new RuntimeException("Unable to load DOI host from configuration. Cannot find property " +CFG_HOST + ".");
			}
		}
		return this.HOST;
	}
	
	protected class DataCiteResponse {
		private final int statusCode;
		private final String content;

		protected DataCiteResponse(int statusCode, String content) {
			this.statusCode = statusCode;
			this.content = content;
		}

		protected int getStatusCode() {
			return this.statusCode;
		}

		protected String getContent() {
			return this.content;
		}
	}
	
	protected String extractDOI(Element root) {
		Element doi = root.getChild("identifier", root.getNamespace());
		return (null == doi) ? null : doi.getTextTrim();
	}
	
	protected Element addEmptyDOI(Element root) {
		if (null != extractDOI(root)) {
			return root;
		}
		Element identifier = new Element("identifier", "http://datacite.org/schema/kernel-4");
		identifier.setAttribute("identifierType", "DOI");
		identifier.addContent(configurationService.getProperty(DOI_PREFIX));
		return root.addContent(0, identifier);
	}
	
	protected Element addDOI(String doi, Element root) {
		if (null != extractDOI(root)) {
			return root;
		}
		
		Element identifier = new Element("identifier"
				, configurationService.getProperty(CFG_NAMESPACE, "http://datacite.org/schema/kernel-4"));
		
		identifier.setAttribute("identifierType", "DOI");
		identifier.addContent(doi);
		return root.addContent(0, identifier);
		
	}
}
