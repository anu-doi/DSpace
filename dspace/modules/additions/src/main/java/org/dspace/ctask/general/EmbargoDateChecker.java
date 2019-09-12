package org.dspace.ctask.general;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;
import org.joda.time.LocalDate;

@Distributive
public class EmbargoDateChecker extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(EmbargoDateChecker.class);
	
	
//	private int status = Curator.CURATE_UNSET;
//	private String result = null;
	private int numFound;
	private StringBuilder messageText;
	private Map<String, List<String>> found;
	private List<String> recipients = null;
	private int futureDays = 0;
	
	@Override
	public void init(Curator curator, String taskId) throws IOException {
		super.init(curator, taskId);
//		log.debug("In init");
		String recipientsProperty = taskProperty("recipients");
//		log.debug("Recipients: " + recipients);

		if (recipientsProperty != null && !"".equals(recipientsProperty)) {
			String[] recipientsList = recipientsProperty.split(",\\s*");
			recipients = Arrays.asList(recipientsList);
		}
		futureDays = taskIntProperty("futuredays", 0);
	}

	@Override
	public int perform(DSpaceObject dso) throws IOException {
		log.info("Performing check on dso " + dso.getID() + " of type " + dso.getType());
		numFound = 0;
		messageText = new StringBuilder("id,handle,embargo_date,collection_handle,collection_name\n");
		found = new HashMap<String, List<String>>();
		
		
		distribute(dso);
		
		String resultText = String.format("%d item(s) found with policies that will automatically apply/cease to apply at a future date", numFound);
		report(resultText);
		
		boolean hasError = false;
		
		if (recipients != null && !recipients.isEmpty()) {
			if (numFound > 0) {
				Email email = Email.getEmail(I18nUtil.getEmailFilename(Locale.getDefault(), "expired_embargos"));
				for (String recipient : recipients) {
					email.addRecipient(recipient);
				}
				
				email.addArgument(resultText);
				InputStream inputStream = new ByteArrayInputStream(messageText.toString().getBytes("utf-8"));
				email.addAttachment(inputStream, "expired-embargos.csv", "text/csv");
				
				try {
					email.send();

					setResult(resultText);
				}
				catch (MessagingException e) {
					String error = "Cannot send email: " + e.getMessage();
					log.error(error, e);;
					report(error);
					setResult(error);
					hasError = true;
				}
				
				log.debug("Message: " + messageText.toString());
			}
		}
		
		
		messageText = null;
		found = null;
		
		if (hasError) {
			return Curator.CURATE_ERROR;
		}
		if (numFound > 0) {
			return Curator.CURATE_SUCCESS;
		}
		else {
			return Curator.CURATE_SKIP;
		}
	}
	
	@Override
	protected void performItem(Item item) throws SQLException, IOException {
		if (!item.isArchived()) {
			return;
		}
		
		String autoLiftDate = null;
		autoLiftDate = checkMetadataEmbargo(item);
		if (autoLiftDate == null) {
			autoLiftDate = checkAutolift(item, item.getHandle());
		}
		if (autoLiftDate == null) {
			autoLiftDate = checkBundlesBitstreamsAutolift(item);
		}
		
		if (autoLiftDate != null) {
			numFound++;
			String parentHandle = item.getParentObject().getHandle();
			String detail = item.getID() + ",http://hdl.handle.net/"+item.getHandle() + "," + autoLiftDate + "," + parentHandle + "," + item.getParentObject().getName();

			report(detail);
			messageText.append(detail).append("\n");
		}
	}
	
	protected String checkMetadataEmbargo(Item item) {
		Metadatum[] embargoDates = item.getMetadata("local", "description", "embargo", Item.ANY);
		LocalDate localDate = LocalDate.now().plusDays(futureDays);
		Date checkDate = localDate.toDate();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String formattedNow = sdf.format(checkDate);
		for (Metadatum date : embargoDates) {
			int comparison = formattedNow.compareTo(date.value);
			if (comparison > 0) {
				return date.value;
			}
		}
		return null;
	}
	
	private String checkBundlesBitstreamsAutolift(Item item) throws SQLException {
		Bundle[] bundles = item.getBundles();
		String autoLiftDate = null;
		for (Bundle bundle : bundles) {
			Bitstream[] bitstreams = bundle.getBitstreams();
			for (Bitstream bitstream : bitstreams) {
				autoLiftDate = checkAutolift(bitstream, item.getHandle());
				if (autoLiftDate != null) {
					return autoLiftDate;
				}
			}
		}
		return null;
	}
	
	private String checkAutolift(DSpaceObject dso, String parentHandle) throws SQLException {
		LocalDate localDate = LocalDate.now().plusDays(futureDays);
		Date checkDate = localDate.toDate();
		
		List<ResourcePolicy> policies = AuthorizeManager.getPolicies(Curator.curationContext(), dso);
		for (ResourcePolicy policy : policies) {
			Date startDate = policy.getStartDate();
			Date endDate = policy.getEndDate();
			
			if ((endDate != null && endDate.before(checkDate)) || (startDate != null && startDate.before(checkDate))) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				String stringStartDate = sdf.format(startDate);
				
				return stringStartDate;
			}
		}
		
		return null;
	}
}
