package org.dspace.identifier.doi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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

public class ANUConnector {

    private static final Logger log = LoggerFactory.getLogger(ANUConnector.class);
    
    private static final String USER= "identifier.doi.user";
    private static final String PASSWORD = "identifier.doi.password";
    private static final String URL_PREFIX = "identifier.doi.resolveurl.prefix";
    private static final String DOI_PREFIX = "identifier.doi.prefix";
    
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

	public void deleteDOI(Context context, String doi) throws DOIIdentifierException {
		URIBuilder uriBuilder = new URIBuilder();
		String deletePath = "metadata/" + doi;
		uriBuilder.setPath(uriBuilder.getPath() + deletePath);
		try {
			HttpDelete httpDelete = new HttpDelete(uriBuilder.build());
			DataCiteResponse dataCiteResponse = sendDelete(httpDelete);
			log.debug("Data cite response, Status: {}, Content: {}", dataCiteResponse.getStatusCode(), dataCiteResponse.getContent());
		}
		catch (URISyntaxException e) {
			log.error("Exception with uri format", e);
		}
		catch (AuthenticationException | IOException e) {
			log.error("Exception deleting information in DataCite", e);
		}
	}

	public String reserveDOI(Context context, DSpaceObject dso) throws DOIIdentifierException {
		prepareCrosswalk();
		
		if (!crosswalk.canDisseminate(dso)) {
			log.error("Crosswalk " + crosswalkName + " cannot disseminate DSO with type " + dso.getType() + " and ID " + dso.getID());
		}
		
		Element root = null;
		try {
			root = crosswalk.disseminateElement(dso);
			addEmptyDOI(root);
		}
		catch (AuthorizeException e) {
			log.error("Caught an AuthorizeException while disseminating DSO with type " + dso.getType() + " and ID " + dso.getID() + ".");
		}
		catch (CrosswalkException e) {
			log.error("Caught a CrosswalkException for the DSO with type " + dso.getType() + 
					" and ID " + dso.getID() + ".");
		}
		catch (IOException | SQLException e) {
			throw new RuntimeException(e);
		}
		HttpPut httpPut = null;
		try {
			URIBuilder uriBuilder = new URIBuilder(serviceLocation);
			String reservePath = "metadata/" + configurationService.getProperty(DOI_PREFIX);
			uriBuilder.setPath(uriBuilder.getPath() + reservePath);
			httpPut = new HttpPut(uriBuilder.build());
			
			DataCiteResponse dataCiteResponse = sendPut(httpPut, root);
			
			switch (dataCiteResponse.getStatusCode()) {
			case (201) : {
				String content = dataCiteResponse.getContent();
				log.debug("DataCite Response content: {}", content);
				//Extract minted doi
				String doi = StringUtils.substringBetween(content, "(", ")");
				return doi;
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
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While reserving a DOI we got a http status code "
                        + "{} and the message \"{}\".", new String[]
                        {Integer.toString(dataCiteResponse.statusCode), dataCiteResponse.getContent()});
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
			}
		}
		catch(URISyntaxException | AuthenticationException | IOException e) {
			log.error("Exception minting doi", e);
			throw new RuntimeException("Exception minting doi", e);
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

	public void registerDOI(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException {
		HttpPut httpPut = null;
		try {
			URIBuilder uriBuilder = new URIBuilder(serviceLocation);
			String registerPath = "doi/" + doi;
			uriBuilder.setPath(uriBuilder.getPath() + registerPath);
			
			httpPut = new HttpPut(uriBuilder.build());
			
			String handlePath = configurationService.getProperty(URL_PREFIX) + "/handle/" + dso.getHandle();
			String registerValue = "doi=" + doi + "\nurl=" + handlePath;
			
			DataCiteResponse response = sendPut(httpPut, registerValue);

			switch (response.getStatusCode()) {
			case (201) : {
				return;
			}
			case (422) : {
                log.warn("DataCite was unable to understand the data we sent we sent.");
                log.warn("DataCite Metadata API returned a http status code "
                        +"422: " + response.getContent());
                throw new DOIIdentifierException("Unable to reserve a DOI" 
                        + ". Please inform your administrator or take a look "
                        +" into the log files.", DOIIdentifierException.BAD_REQUEST);
			}
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While reserving a DOI we got a http status code "
                        + "{} and the message \"{}\".", new String[]
                        {Integer.toString(response.statusCode), response.getContent()});
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
			}
			
			
		}
		catch(URISyntaxException | AuthenticationException | IOException e) {
			log.error("Exception setting url for doi", e);
			throw new RuntimeException("Exception minting doi", e);
		}
	}

	public void updateMetadata(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException {
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
		HttpPut httpPut = null;
		try {
			URIBuilder uriBuilder = new URIBuilder(serviceLocation);
			String updatePath = "metadata/" + doi;
			
			uriBuilder.setPath(uriBuilder.getPath() + updatePath);
			
			String doiUrl = configurationService.getProperty(URL_PREFIX) + "/handle/" + dso.getHandle();
			httpPut = new HttpPut(uriBuilder.build());
			
			DataCiteResponse response = sendPut(httpPut, root);
			
			switch(response.getStatusCode()) {
			case(201) : {
				return;
			}
			case(422) : {
                log.warn("DataCite was unable to understand the XML we sent.");
                log.warn("DataCite Metadata API returned a http status code "
                        +"422: " + response.getContent());
                Format format = Format.getCompactFormat();
                format.setEncoding("UTF-8");
                XMLOutputter xout = new XMLOutputter(format);
                log.info("We send the following XML:\n" + xout.outputString(root));
                throw new DOIIdentifierException("Unable to reserve a DOI" 
                        + ". Please inform your administrator or take a look "
                        +" into the log files.", DOIIdentifierException.BAD_REQUEST);
				
			}
            default :
            {
                log.warn("While reserving a DOI we got a http status code "
                        + "{} and the message \"{}\".", new String[]
                        {Integer.toString(response.statusCode), response.getContent()});
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
			}
		}
		catch(URISyntaxException | AuthenticationException | IOException e) {
			log.error("Exception minting doi", e);
			throw new RuntimeException("Exception minting doi", e);
		}
	}
	
	protected DataCiteResponse sendPut(HttpPut httpPut, Element rootElement) throws IOException, AuthenticationException, DOIIdentifierException {
		Format format = Format.getCompactFormat();
		format.setEncoding("UTF-8");
		XMLOutputter xmlOut = new XMLOutputter(format);
		String document = xmlOut.outputString(new org.jdom.Document(rootElement));
		
		return sendPut(httpPut, document);
	}
	
	protected DataCiteResponse sendPut(HttpPut httpPut, String document) throws IOException, AuthenticationException, DOIIdentifierException {
		HttpEntity entity = new ByteArrayEntity(document.getBytes());
		httpPut.setEntity(entity);
		return sendRequest(httpPut);
	}
	
	protected DataCiteResponse sendDelete(HttpDelete httpDelete)  throws IOException, AuthenticationException, DOIIdentifierException {
		return sendRequest(httpDelete);
	}
	
	protected DataCiteResponse sendRequest(HttpUriRequest request) throws IOException, AuthenticationException, DOIIdentifierException {
		String username = configurationService.getProperty(USER);
		String password = configurationService.getProperty(PASSWORD);
		
		CloseableHttpClient client = HttpClients.createDefault();
		
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
		request.addHeader(new BasicScheme().authenticate(credentials, request, null));
		
		CloseableHttpResponse response = client.execute(request);
		//TODO stuff with the status?
		// For some reason there are multiple types of Dom uses... We want the w3c one so that we can
		// use the utils to extract info from it

		HttpEntity entity = null;
		try {
			StatusLine status = response.getStatusLine();
			int statusCode = status.getStatusCode();
			
			
			String content = null;
			entity = response.getEntity();
			if (null != entity) {
				content = EntityUtils.toString(entity, "UTF-8");
			}
			log.debug("Request response - Status: {}, Content: {}", statusCode, content);
			
			switch (statusCode) {
	        // we get a 401 if we forgot to send credentials or if the username
	        // and password did not match.
	        case (401) :
	        {
	            log.info("We were unable to authenticate against the DOI registry agency.");
	            log.info("The response was: {}", content);
	            throw new DOIIdentifierException("Cannot authenticate at the "
	                    + "DOI registry agency. Please check if username "
	                    + "and password are set correctly.",
	                    DOIIdentifierException.AUTHENTICATION_ERROR);
	        }
	
	        // We get a 403 Forbidden if we are managing a DOI that belongs to
	        // another party or if there is a login problem.
	        case (403) :
	        {
	            log.info("Managing a DOI was prohibited by the DOI "
	                    + "registration agency: {}", content);
	            throw new DOIIdentifierException("We can check, register or "
	                    + "reserve DOIs that belong to us only.",
	                    DOIIdentifierException.FOREIGN_DOI);
	        }
	
	        // 500 is documented and signals an internal server error
	        case (500) :
	        {
	            log.warn("Caught an http status code 500 while managing DOI "
	                    +"{}. Message was: " + content);
	            throw new DOIIdentifierException("DataCite API has an internal error. "
	                    + "It is temporarily impossible to manage DOIs. "
	                    + "Further information can be found in DSpace log file.",
	                    DOIIdentifierException.INTERNAL_ERROR);
	        }
			}
			
			return new DataCiteResponse(statusCode, content);
		}
		finally {
			
			client.close();
			
		}
	}

    protected class DataCiteResponse
    {
        private final int statusCode;
        private final String content;

        protected DataCiteResponse(int statusCode, String content)
        {
            this.statusCode = statusCode;
            this.content = content;
        }
        
        protected int getStatusCode()
        {
            return this.statusCode;
        }
        
        protected String getContent()
        {
            return this.content;
        }
    }
	
}
