/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;


import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static org.apache.commons.lang.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authenticate.oidc.OidcClient;
import org.dspace.authenticate.oidc.model.OidcTokenResponseDTO;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.bind.DatatypeConverter;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

/**
 * OpenID Connect Authentication for DSpace.
 *
 * This implementation doesn't allow/needs to register user, which may be holder
 * by the openID authentication server.
 *
 * @link   https://openid.net/developers/specs/
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class OidcAuthenticationBean implements AuthenticationMethod {

    public static final String OIDC_AUTH_ATTRIBUTE = "oidc";

    private final static String LOGIN_PAGE_URL_FORMAT = "%s?client_id=%s&response_type=code&scope=%s&redirect_uri=%s";

    private static final Logger LOGGER = LoggerFactory.getLogger(OidcAuthenticationBean.class);

    private static final String OIDC_AUTHENTICATED = "oidc.authenticated";
    
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private OidcClient oidcClient;

    @Autowired
    private EPersonService ePersonService;

    @Override
    public boolean allowSetPassword(Context context, HttpServletRequest request, String username) throws SQLException {
        return false;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }

    @Override
    public boolean canSelfRegister(Context context, HttpServletRequest request, String username) throws SQLException {
        return canSelfRegister();
    }

    @Override
    public void initEPerson(Context context, HttpServletRequest request, EPerson eperson) throws SQLException {
    }

    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) throws SQLException {
        // return List.of();
    	
    	try {
            // without a logged in user, this method should return an empty list
            if (context.getCurrentUser() == null) {
                return Collections.EMPTY_LIST;
            }
            // if the logged in user does not have a netid, it's not an OIDC user
            // and this method should return an empty list
            if (context.getCurrentUser().getNetid() == null) {
                return Collections.EMPTY_LIST;
            }
            if (!context.getCurrentUser().getNetid().equals("")) {
                String groupName = configurationService.getProperty("authentication-oidc.login.specialgroup");
                if ((groupName != null) && (!groupName.trim().equals(""))) {
                    Group oidcGroup = groupService.findByName(context, groupName);
                    if (oidcGroup == null) {
                    	LOGGER.warn(LogHelper.getHeader(context,
                                "oidc_specialgroup",
                                "Group defined in login.specialgroup does not exist"));
                        return Collections.EMPTY_LIST;
                    } else {
                        return Arrays.asList(oidcGroup);
                    }
                }
            }
        } catch (SQLException ex) {
            // The user is not an OIDC user, so we don't need to worry about them
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public String getName() {
        return OIDC_AUTH_ATTRIBUTE;
    }

    @Override
    public int authenticate(Context context, String username, String password, String realm, HttpServletRequest request)
        throws SQLException {

        if (request == null) {
            LOGGER.warn("Unable to authenticate using OIDC because the request object is null.");
            return BAD_ARGS;
        }

        if (request.getAttribute(OIDC_AUTH_ATTRIBUTE) == null) {
            return NO_SUCH_USER;
        }

        String code = (String) request.getParameter("code");
        if (StringUtils.isEmpty(code)) {
            LOGGER.warn("The incoming request has not code parameter");
            return NO_SUCH_USER;
        }

        return authenticateWithOidc(context, code, request);
    }

    private int authenticateWithOidc(Context context, String code, HttpServletRequest request) throws SQLException {

        OidcTokenResponseDTO accessToken = getOidcAccessToken(code);
        //System.out.println("Code : "+code);

        if (accessToken == null) {
            LOGGER.warn("No access token retrieved by code");
            return NO_SUCH_USER;
        }

        Map<String, Object> userInfo = getOidcUserInfo(accessToken.getAccessToken());
        
        //String[] decodedAccessToken = DatatypeConverter.pars(accessToken.getAccessToken());
        String decodedId = new String(Base64.getDecoder().decode(accessToken.getAccessToken()));
        System.out.println("The decoded access token : " + decodedId);
//        System.out.println("Access ID token : "+ accessToken.getIdToken());
//        System.out.println("Access token get : "+  accessToken.getAccessToken());
//        System.out.println("Scope : "+ accessToken.getScope());
//        System.out.println("Try string access token : "+ accessToken.toString());
        //String email = getAttributeAsString(userInfo, getEmailAttribute());
        
        for(Map.Entry<String, Object> info : userInfo.entrySet()) {
        	System.out.println(info.getKey() + ":" + info.getValue().toString());
        }
        
        String netId = getAttributeAsString(userInfo, getNetIdAttribute());
        if (StringUtils.isBlank(netId)) {
            LOGGER.warn("No netId found in the user info attributes : "+netId);
            return NO_SUCH_USER;
        }
        
        String currentDate = ISO_LOCAL_DATE_TIME.format(now());
        System.out.println(currentDate + ": netId: "+netId);
        
        //EPerson ePerson = ePersonService.findByEmail(context, email);
        
        EPerson ePerson = ePersonService.findByNetid(context, netId);
        if (ePerson != null) {
            request.setAttribute(OIDC_AUTHENTICATED, true);
            return ePerson.canLogIn() ? logInEPerson(context, ePerson) : BAD_ARGS;
        }

        // if self registration is disabled, warn about this failure to find a matching eperson
        if (! canSelfRegister()) {
            LOGGER.warn("Self registration is currently disabled for OIDC, and no ePerson could be found for NetId: {}",
                netId);
        }

        return canSelfRegister() ? registerNewEPerson(context, userInfo, netId) : NO_SUCH_USER;
    }

    @Override
    public String loginPageURL(Context context, HttpServletRequest request, HttpServletResponse response) {

        String authorizeUrl = configurationService.getProperty("authentication-oidc.authorize-endpoint");
        String clientId = configurationService.getProperty("authentication-oidc.client-id");
        String clientSecret = configurationService.getProperty("authentication-oidc.client-secret");
        String redirectUri = configurationService.getProperty("authentication-oidc.redirect-url");
        String tokenUrl = configurationService.getProperty("authentication-oidc.token-endpoint");
        String userInfoUrl = configurationService.getProperty("authentication-oidc.user-info-endpoint");
        String[] defaultScopes =
            new String[] {
                "openid", "email", "profile"
            };
        String scopes = String.join(" ", configurationService.getArrayProperty("authentication-oidc.scopes",
            defaultScopes));

        if (isAnyBlank(authorizeUrl, clientId, redirectUri, clientSecret, tokenUrl, userInfoUrl)) {
            LOGGER.error("Missing mandatory configuration properties for OidcAuthenticationBean");

            // prepare a Map of the properties which can not have sane defaults, but are still required
            final Map<String, String> map = Map.of("authorizeUrl", authorizeUrl, "clientId", clientId, "redirectUri",
                redirectUri, "clientSecret", clientSecret, "tokenUrl", tokenUrl, "userInfoUrl", userInfoUrl);
            final Iterator<Entry<String, String>> iterator = map.entrySet().iterator();

            while (iterator.hasNext()) {
                final Entry<String, String> entry = iterator.next();

                if (isBlank(entry.getValue())) {
                    LOGGER.error(" * {} is missing", entry.getKey());
                }
            }
            return "";
        }

        try {
            return format(LOGIN_PAGE_URL_FORMAT, authorizeUrl, clientId, scopes, encode(redirectUri, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
            return "";
        }

    }

    private int logInEPerson(Context context, EPerson ePerson) {
        context.setCurrentUser(ePerson);
        return SUCCESS;
    }

    private int registerNewEPerson(Context context, Map<String, Object> userInfo, String email) throws SQLException {
        try {

            context.turnOffAuthorisationSystem();

            EPerson eperson = ePersonService.create(context);

            //New logic to use Uni ID uid as the netID - Starts here
            String netId = getAttributeAsString(userInfo, getNetIdAttribute());
            if (netId != null) {
                eperson.setNetid(netId);
                LOGGER.info("Self registering new person using OIDC-based on netID : "+netId);
            }
            else {
            	eperson.setNetid(email);
            	LOGGER.info("Self registering new person using OIDC-based on email : "+email);
            }
            //New logic to use Uni ID uid as the netID - Ends here
            
            eperson.setEmail(email);

            String firstName = getAttributeAsString(userInfo, getFirstNameAttribute());
            if (firstName != null) {
                eperson.setFirstName(context, firstName);
            }

            String lastName = getAttributeAsString(userInfo, getLastNameAttribute());
            if (lastName != null) {
                eperson.setLastName(context, lastName);
            }

            eperson.setCanLogIn(true);
            eperson.setSelfRegistered(true);

            ePersonService.update(context, eperson);
            context.setCurrentUser(eperson);
            context.dispatchEvents();

            String message = "Self registered new eperson using OIDC-based attributes:";
            if (netId != null) {
                message += "  NetId: '" + netId + "'\n";
            }
            if(email != null) {
            	message += "  Email: '" + email + "'";
            }
            
            LOGGER.info(message);
            
            return SUCCESS;

        } catch (Exception ex) {
            LOGGER.error("An error occurs registering a new EPerson from OIDC", ex);
            return NO_SUCH_USER;
        } finally {
            context.restoreAuthSystemState();
        }
    }

	private OidcTokenResponseDTO getOidcAccessToken(String code) {
        try {
            return oidcClient.getAccessToken(code);
        } catch (Exception ex) {
            LOGGER.error("An error occurs retriving the OIDC access_token", ex);
            return null;
        }
    }

    private Map<String, Object> getOidcUserInfo(String accessToken) {
        try {
            return oidcClient.getUserInfo(accessToken);
        } catch (Exception ex) {
            LOGGER.error("An error occurs retriving the OIDC user info", ex);
            return Map.of();
        }
    }

    private String getAttributeAsString(Map<String, Object> userInfo, String attribute) {
        if (isBlank(attribute)) {
            return null;
        }
        return userInfo.containsKey(attribute) ? String.valueOf(userInfo.get(attribute)) : null;
    }

    private String getEmailAttribute() {
        return configurationService.getProperty("authentication-oidc.user-info.email", "email");
    }

    private String getFirstNameAttribute() {
        return configurationService.getProperty("authentication-oidc.user-info.first-name", "given_name");
    }

    private String getLastNameAttribute() {
        return configurationService.getProperty("authentication-oidc.user-info.last-name", "family_name");
    }
    
    //New function to accommodate the new logic of using Uid as NetID
    private String getNetIdAttribute() {
    	return configurationService.getProperty("authentication-oidc.user-info.id-field", "userprincipalname");
	}

    private boolean canSelfRegister() {
        String canSelfRegister = configurationService.getProperty("authentication-oidc.can-self-register", "true");
        if (isBlank(canSelfRegister)) {
            return true;
        }
        return toBoolean(canSelfRegister);
    }

    public OidcClient getOidcClient() {
        return this.oidcClient;
    }

    public void setOidcClient(OidcClient oidcClient) {
        this.oidcClient = oidcClient;
    }

    @Override
    public boolean isUsed(final Context context, final HttpServletRequest request) {
        if (request != null &&
                context.getCurrentUser() != null &&
                request.getAttribute(OIDC_AUTHENTICATED) != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canChangePassword(Context context, EPerson ePerson, String currentPassword) {
        return false;
    }

}
