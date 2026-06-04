/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

/**
 * Try to look to an item metadata for the corresponding author name and email.
 * Failover to the RequestItemSubmitterStrategy.
 *
 * @author Andrea Bollini
 */
public class RequestItemMetadataStrategy extends RequestItemSubmitterStrategy {

    protected String emailMetadata;
    protected String fullNameMetadata;
    
    private final static Logger log = LogManager.getLogger();
    
    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected ItemService itemService;

    public RequestItemMetadataStrategy() {
    }

    @Override
    @NonNull
    public List<RequestItemAuthor> getRequestItemAuthor(Context context, Item item)
        throws SQLException {
        List<RequestItemAuthor> authors =  new ArrayList<>(1);
        if (emailMetadata != null) {
            List<MetadataValue> vals = itemService.getMetadataByMetadataString(item, emailMetadata);
            List<MetadataValue> nameVals;
            if (null != fullNameMetadata) {
                nameVals = itemService.getMetadataByMetadataString(item, fullNameMetadata);
            } else {
                nameVals = Collections.EMPTY_LIST;
            }
            boolean useNames = vals.size() == nameVals.size();
            if (!vals.isEmpty()) {
                authors = new ArrayList<>(vals.size());
                for (int authorIndex = 0; authorIndex < vals.size(); authorIndex++) {
                    String email = vals.get(authorIndex).getValue();
                    String fullname = null;
                    if (useNames) {
                        fullname = nameVals.get(authorIndex).getValue();
                    }

                    if (StringUtils.isBlank(fullname)) {
                        fullname = I18nUtil.getMessage(
                                "org.dspace.app.requestitem.RequestItemMetadataStrategy.unnamed",
                                context);
                    }
                    RequestItemAuthor author = new RequestItemAuthor(
                            fullname, email);
                    authors.add(author);
                }
               return authors;
            } else {
            	// No author email addresses!  Fall back
                // Get help desk name and email
                String email = configurationService.getProperty("mail.helpdesk");
                String name = configurationService.getProperty("mail.helpdesk.name");
                // If help desk mail is null get the mail and name of admin
                if (email == null || email.isEmpty()) {
                    email = configurationService.getProperty("mail.admin");
                    name = configurationService.getProperty("mail.admin.name");
                }
                RequestItemAuthor author = new RequestItemAuthor(
                        name, email);
                authors.add(author);
                
                return authors;
            }
        }
        else {
        		// In case emailMetadata like local.request.email is not uncommented in requestitem.xml
        		// No author email addresses!  Fall back
                // Get help desk name and email
        		log.info("The emailMetadata field is not configured in the requestItem.xml. So helpdesk/admin email and name is considered.");
                String email = configurationService.getProperty("mail.helpdesk");
                String name = configurationService.getProperty("mail.helpdesk.name");
                // If help desk mail is null or left empty get the mail and name of admin
                if (email == null || email.isEmpty()) {
                    email = configurationService.getProperty("mail.admin");
                    name = configurationService.getProperty("mail.admin.name");
                }
               authors.add(new RequestItemAuthor(name, email));
            return authors;
        }
}

    public void setEmailMetadata(@NonNull String emailMetadata) {
        this.emailMetadata = emailMetadata;
    }

    public void setFullNameMetadata(@NonNull String fullNameMetadata) {
        this.fullNameMetadata = fullNameMetadata;
    }

}
