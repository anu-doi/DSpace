package org.dspace.identifier.doi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.MessagingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.identifier.ANUDOIIdentifierProvider;
import org.dspace.identifier.DOI;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ANUDOIOrganiser {
	private static final Logger log = LoggerFactory.getLogger(DOIOrganiser.class);
	
	private final ANUDOIIdentifierProvider provider;
	private final Context context;
	private boolean quiet;
	private int count = 0;
	protected DOIService doiService;
	protected ConfigurationService configurationService;
	protected boolean skipFilter;
	private static final String QUOTE = "\"";
	
	public ANUDOIOrganiser(Context context, ANUDOIIdentifierProvider provider) {
		this.context = context;
		this.provider = provider;
		this.doiService = IdentifierServiceFactory.getInstance().getDOIService();
		this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
		this.skipFilter = false;
	}

	public static void main(String[] args) {
		//TODO set this back to debug
		log.debug("Starting ANU DOI organiser");
//		log.info("Starting ANU DOI organiser");
		
		Context context = new Context();
		context.turnOffAuthorisationSystem();
		
		new DSpace().getSingletonService(ANUDOIIdentifierProvider.class);
		
		ANUDOIOrganiser organiser = new ANUDOIOrganiser(context, new DSpace().getSingletonService(ANUDOIIdentifierProvider.class));
		
		runCLI(context, organiser, args);
		
		try {
			context.complete();
		} catch (SQLException sqle) {
			System.err.println("Cannot save changes to database: " + sqle.getMessage());
			System.exit(-1);
		}
		
	}
	
	public static void runCLI(Context context, ANUDOIOrganiser organiser, String[] args) {
		Options options = new Options();
		options.addOption("h", "help", false, "Help");
		options.addOption("l", "list", false, "List all objects to be reserved, registered, deleted, or updated");
		options.addOption("r", "register-all", false, "");
		options.addOption("s", "reserve-all", false, "");
		options.addOption("u", "update-all", false, "Perform online metadata update "
				+ "for all identifiers queued for metadata update.");
		options.addOption("d", "deactivate-all", false, "Perform online deactivation for all identifiers queued for deactivation");
		options.addOption("a", "reactivate-all", false, "Perform online re-activation for all identifiers queued for re-activation");
		options.addOption("c", "count", true, "Maximum number of items to process");
		
		Option registerDoi = Option.builder().longOpt("register-doi").hasArg().argName("ItemID|handle")
			.desc("Register a specified identifier. You can specify the identifier by ItemID or Handle").build();
		

//		Option registerDoi = OptionBuilder.withArgName("ItemID|handle").withLongOpt("register-doi")
//				.hasArgs(1).withDescription("Register a specified identifier. You can specify "
//						+ "the identifier by ItemID or Handle").create();
		options.addOption(registerDoi);
		
		Option update = Option.builder().longOpt("update-doi").hasArg().argName("ItemID|handle")
				.desc("Update online an object for a given DOI identifier or ItemID or Handle. A DOI identifier or an ItemID or a Handle is needed.\\n").build();
		options.addOption(update);
		
		CommandLineParser parser = new DefaultParser();
		CommandLine line = null;
		HelpFormatter helpFormatter = new HelpFormatter();
		try {
			line = parser.parse(options, args);
		} catch (ParseException e) {
			log.error("Exception parsing command line", e);
			System.exit(1);
		}
		
		if (line.hasOption('h') || 0 == line.getOptions().length) {
			helpFormatter.printHelp("\nANU DOI organiser\n", options);
		}
		
		if (line.hasOption('q')) {
			organiser.setQuiet();
		}
		//TODO implement count limits
		if (line.hasOption('c')) {
			String countStr = line.getOptionValue("c");
			organiser.setCount(Integer.parseInt(countStr));
		}
		
		if (line.hasOption('l')) {
			organiser.list("registration", null, null, ANUDOIIdentifierProvider.TO_BE_REGISTERED);
			organiser.list("reservation", null, null, ANUDOIIdentifierProvider.TO_BE_RESERVED);
			organiser.list("update", null, null, ANUDOIIdentifierProvider.UPDATE_BEFORE_REGISTRATION
					, ANUDOIIdentifierProvider.UPDATE_REGISTERED, ANUDOIIdentifierProvider.UPDATE_RESERVED);
			organiser.list("deactivate", null, null, ANUDOIIdentifierProvider.TO_BE_DELETED);
			organiser.list("reactivate", null, null, ANUDOIIdentifierProvider.TO_BE_ACTIVATED);
		}
		
		if (line.hasOption("r")) {
			organiser.registerAll();
		}
		
		if (line.hasOption("s")) {
			organiser.reserveAll();
		}
		
		if (line.hasOption("u")) {
			organiser.updateAll();
		}
		
		if (line.hasOption("d")) {
			organiser.deactivateAll();
		}
		
		if (line.hasOption("a")) {
			organiser.reactivateAll();
		}
	}
	
	public  void list(String processName, PrintStream out, PrintStream err, Integer ... status) {
		String indent = "	";
		if (null == out) {
			out = System.out;
		}
		if (null == err) {
			err = System.err;
		}
		
		try {
			List<DOI> doiList = doiService.getDOIsByStatus(context, Arrays.asList(status));
			if (0 < doiList.size()) {
				out.println("DOIs queued for " + processName + ": ");
			}
			else {
				out.println("There are no DOIs queued for " + processName + ".");
			}
			
			for (DOI doiRow : doiList) {
				out.print(indent + DOI.SCHEME + doiRow.getDoi());
				DSpaceObject dso = doiRow.getDSpaceObject();
				if (null != dso) {
					out.println(" (belongs to item with handle " + dso.getHandle() + ")");
				} else {
					out.println(" (cannot determine handle of assigned object)");
				}
			}
			
			out.println("");
		} catch (SQLException e) {
			err.println("Error in database connection: " + e.getMessage());
			e.printStackTrace(err);
		}
	}
	
	public void registerAll() {
		try {
			List<DOI> dois = doiService.getDOIsByStatus(context, Arrays.asList(ANUDOIIdentifierProvider.TO_BE_REGISTERED));
			if (count > 0) {
				int listSize = Math.min(dois.size(), count);
				dois = dois.subList(0, listSize);
			}
			if (dois.isEmpty()) {
				System.err.println("There are no objects in the database to be registered.");
			}
			Map<DSpaceObject, String> errorObjects = new HashMap<DSpaceObject, String>();
			for (DOI doi : dois) {
				try {
					register(doi);
				}
				catch(IdentifierException e) {
					errorObjects.put(doi.getDSpaceObject(), e.getMessage());
					log.error("Exception minting doi", e);
				}
				context.uncacheEntity(doi);
			}
			if (errorObjects.size() > 0) {
				generateErrorEmail(errorObjects);
			}
		}
		catch (SQLException e) {
			System.err.println("Error in database connection: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void register(DOI doiRow) throws IdentifierException {
		DSpaceObject dso = doiRow.getDSpaceObject();
		if (Constants.ITEM != dso.getType()) {
			throw new IllegalArgumentException("Current DSpace supports DOIs for Items only.");
		}
		try {
			provider.registerOnline(context, dso, doiRow.getDoi());
		} catch (SQLException e) {
			log.error("Error while trying to get data from database", e);
			if (!quiet) {
				System.err.println("It wasn't possible to register this identifier: " + DOI.SCHEME + doiRow.getDoi());
				
			}
		}
	}
	
	public void reserveAll() {
		try {
			List<DOI> dois = doiService.getDOIsByStatus(context, Arrays.asList(ANUDOIIdentifierProvider.TO_BE_RESERVED));
			if (count > 0) {
				int listSize = Math.min(dois.size(), count);
				dois = dois.subList(0, listSize);
			}
			if (dois.isEmpty()) {
				System.err.println("There are no objects in the database to be registered.");
			}
			Map<DSpaceObject, String> errorObjects = new HashMap<DSpaceObject, String>();
			for (DOI doi : dois) {
				try {
					reserve(doi);
				}
				catch(IdentifierException e) {
					errorObjects.put(doi.getDSpaceObject(), e.getMessage());
					log.error("Exception minting doi", e);
				}
				context.uncacheEntity(doi);
			}
			if (errorObjects.size() > 0) {
				generateErrorEmail(errorObjects);
			}
		}
		catch (SQLException e) {
			System.err.println("Error in database connection: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	public void reserve(DOI doiRow) throws IdentifierException {
		DSpaceObject dso = doiRow.getDSpaceObject();
		if (Constants.ITEM != dso.getType()) {
			throw new IllegalArgumentException("Current DSpace supports DOIs for Items only.");
		}
		try {
//			provider.reserveOnline(context, dso, skipFilter);
			provider.reserveOnline(context, dso);
//			provider.registerOnline(context, dso, doiRow.getDoi());
		} catch (SQLException e) {
			log.error("Error while trying to get data from database", e);
			if (!quiet) {
				System.err.println("It wasn't possible to register this identifier: " + dso.getID());
				e.printStackTrace();
			}
		}
		
	}
	
	public void updateAll() {
		try {
			List<DOI> dois = doiService.getDOIsByStatus(context, Arrays.asList(ANUDOIIdentifierProvider.UPDATE_REGISTERED, ANUDOIIdentifierProvider.UPDATE_BEFORE_REGISTRATION, ANUDOIIdentifierProvider.UPDATE_RESERVED));
			if (count > 0) {
				int listSize = Math.min(dois.size(), count);
				dois = dois.subList(0, listSize);
			}
			if (dois.isEmpty()) {
				System.err.println("There are no objects in the database to be registered.");
			}
			Map<DSpaceObject, String> errorObjects = new HashMap<DSpaceObject, String>();
			for (DOI doi : dois) {
				try {
					update(doi);
				}
				catch(IdentifierException e) {
					errorObjects.put(doi.getDSpaceObject(), e.getMessage());
					log.error("Exception minting doi", e);
				}
				context.uncacheEntity(doi);
			}
//			if (errorObjects.size() > 0) {
//				generateErrorEmail(errorObjects);
//			}
		}
		catch (SQLException e) {
			System.err.println("Error in database connection: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	public void update(DOI doiRow) throws IdentifierException {
		DSpaceObject dso = doiRow.getDSpaceObject();
		if (Constants.ITEM != dso.getType()) {
			throw new IllegalArgumentException("Current DSpace supports DOIs for Items only.");
		}
		try {
			provider.updateMetadataOnline(context, dso, doiRow.getDoi());
		} catch (SQLException e) {
			log.error("Error while trying to get data from database", e);
			if (!quiet) {
				System.err.println("It wasn't possible to update this identifier: " + dso.getID());
				e.printStackTrace();
			}
		}
		
	}
	
	public void deactivateAll() {
		try {
			List<DOI> dois = doiService.getDOIsByStatus(context, Arrays.asList(ANUDOIIdentifierProvider.TO_BE_DELETED));
			if (count > 0) {
				int listSize = Math.min(dois.size(), count);
				dois = dois.subList(0, listSize);
			}
			if (dois.isEmpty()) {
				System.err.println("There are no objects in the database to be registered.");
			}
			Map<DSpaceObject, String> errorObjects = new HashMap<DSpaceObject, String>();
			for (DOI doi : dois) {
				try {
					deactivate(doi);
				}
				catch(IdentifierException e) {
					errorObjects.put(doi.getDSpaceObject(), e.getMessage());
					log.error("Exception deactivating doi", e);
				}
				context.uncacheEntity(doi);
			}
		}
		catch (SQLException e) {
			System.err.println("Error in database connection: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	public void deactivate(DOI doiRow) throws IdentifierException {
		DSpaceObject dso = doiRow.getDSpaceObject();
		if (Constants.ITEM != dso.getType()) {
			throw new IllegalArgumentException("Current DSpace supports DOIs for Items only.");
		}
//		try {
			provider.deleteOnline(context, doiRow.getDoi());
//		} catch (SQLException e) {
//			log.error("Error while trying to get data from database", e);
//			if (!quiet) {
//				System.err.println("It wasn't possible to register this identifier: " + dso.getID());
//				e.printStackTrace();
//			}
//		}
		
	}
	
	public void reactivateAll() {
		try {
			List<DOI> dois = doiService.getDOIsByStatus(context, Arrays.asList(ANUDOIIdentifierProvider.TO_BE_ACTIVATED));
			if (count > 0) {
				int listSize = Math.min(dois.size(), count);
				dois = dois.subList(0, listSize);
			}
			if (dois.isEmpty()) {
				System.err.println("There are no objects in the database to be registered.");
			}
			Map<DSpaceObject, String> errorObjects = new HashMap<DSpaceObject, String>();
			for (DOI doi : dois) {
				try {
					reactivate(doi);
				}
				catch(IdentifierException e) {
					errorObjects.put(doi.getDSpaceObject(), e.getMessage());
					log.error("Exception minting doi", e);
				}
				context.uncacheEntity(doi);
			}
		}
		catch (SQLException e) {
			System.err.println("Error in database connection: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	public void reactivate(DOI doiRow) throws IdentifierException {
		DSpaceObject dso = doiRow.getDSpaceObject();
		if (Constants.ITEM != dso.getType()) {
			throw new IllegalArgumentException("Current DSpace supports DOIs for Items only.");
		}
		try {
			provider.activateOnline(context, dso);
		} catch (SQLException e) {
			log.error("Error while trying to get data from database", e);
			if (!quiet) {
				System.err.println("It wasn't possible to register this identifier: " + dso.getID());
				e.printStackTrace();
			}
		}
		
	}
	
	private void setQuiet() {
		this.quiet = true;
	}
	
	private void setCount(Integer count) {
		this.count = count;
	}
	
	private void generateErrorEmail(Map<DSpaceObject, String> errorObjects) {
		StringWriter writer = new StringWriter();
		writer.append("id,handle,dc.date.issued,dc.date.created,error_msg\n");
		for (Entry<DSpaceObject, String> object : errorObjects.entrySet()) {
			DSpaceObject dso = object.getKey();
			String errorMessage = object.getValue();
//			writer.append(Integer.toString(dso.getID()));
			writer.append(dso.getID().toString());
			writer.append(",");
			writer.append(QUOTE);
			writer.append("http://hdl.handle.net/");
			writer.append(dso.getHandle());
			writer.append(QUOTE);
			writer.append(",");
			//TODO find out what the current method for getting individual values are
			writeFirstValue(writer, dso, "dc", "date", "issued");
			writer.append(",");
			writeFirstValue(writer, dso, "dc", "date", "created");
			writer.append(",");
			writer.append(QUOTE);
			writer.append(errorMessage);
			writer.append(QUOTE);
			writer.append("\n");
		}
		
		Email email = new Email();
		email.setSubject("Records where there are issues creating DOI's");
		email.setContent("body", "Please find attached the items that had errors when minting a doi");
		
		email.addRecipient(configurationService.getProperty("mail.helpdesk"));
		InputStream is = new ByteArrayInputStream(writer.toString().getBytes());
		email.addAttachment(is, "doi_exception_report.csv", "text/csv");
		try {
			email.send();
		}
		catch (IOException | MessagingException e) {
			log.error("Exception sending doi failure email");
		}
	}
	
	private void writeFirstValue(StringWriter writer, DSpaceObject dso, String schema, String element, String qualifier) {
		for (MetadataValue value : dso.getMetadata()) {
			MetadataField field = value.getMetadataField();
			if (schema.equals(field.getMetadataSchema().getName()) && element.equals(field.getElement()) && (qualifier.equals(field.getQualifier()))) {
				writer.append(QUOTE);
				writer.append(value.getValue());
				writer.append(QUOTE);
				return;
			}
		}
	}
}
