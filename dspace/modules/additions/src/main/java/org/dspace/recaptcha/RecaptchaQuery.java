package org.dspace.recaptcha;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

import com.google.gson.Gson;

public class RecaptchaQuery {
    private static final Logger log = Logger.getLogger(RecaptchaQuery.class);
	
	public RecaptchaResponse query(String token, String remoteip) throws URISyntaxException, IOException {
		String secret = ConfigurationManager.getProperty("recaptcha.secretkey");
		String siteverify = ConfigurationManager.getProperty("recaptcha.siteverify");
		
		URIBuilder uriBuilder = new URIBuilder(siteverify);
		
		uriBuilder.addParameter("secret", secret);
		uriBuilder.addParameter("response", token);
		uriBuilder.addParameter("remoteip", remoteip);
		
		HttpPost post = new HttpPost(uriBuilder.build());
		
		CloseableHttpClient client = HttpClients.createDefault();
		
		CloseableHttpResponse response = client.execute(post);
		
		HttpEntity responseEntity = response.getEntity();
		InputStream is = responseEntity.getContent();
		
		Reader reader = new InputStreamReader(is, "UTF-8");
		Gson gson = new Gson();
		RecaptchaResponse captchaResponse = gson.fromJson(reader, RecaptchaResponse.class);
		
		return captchaResponse;
	}
}
