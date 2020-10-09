package org.dspace.identifier.doi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.MessagingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.DOI;
import org.dspace.identifier.IdentifierException;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ANUDOIOrganiser {
    private static final Logger log = LoggerFactory.getLogger(ANUDOIOrganiser.class);

	ANUIdentifierProvider provider;
	private Context context;
	private boolean quiet;
	private int count;
	
	public ANUDOIOrganiser(Context context, ANUIdentifierProvider provider) {
		this.context = context;
		this.provider = provider;
		this.quiet = false;
	}
	
	public static void main(String[] args) {
		log.debug("Starting DOI organiser");
		
		Context context = null;
		try {
			context = new Context();
		}
		catch (SQLException e) {
			System.err.println("Cannot connect to database: " + e.getMessage());
			System.exit(-1);
		}
		
		context.turnOffAuthorisationSystem();
		
		ANUDOIOrganiser organiser = new ANUDOIOrganiser(context, new DSpace().getSingletonService(ANUIdentifierProvider.class));

		runCLI(context, organiser, args);
		

		try {
			context.complete();
		}
		catch (SQLException e) {
			System.err.println("Cannot save changes to database: " + e.getMessage());
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

		Option registerDoi = OptionBuilder.withArgName("ItemID|handle").withLongOpt("register-doi")
				.hasArgs(1).withDescription("Register a specified identifier. You can specify "
						+ "the identifier by ItemID or Handle").create();
		options.addOption(registerDoi);
		
		Option update = OptionBuilder.withArgName("ItemID|handle")
			.hasArgs(1)
			.withDescription("Update online an object for a given DOI identifier"
			+ " or ItemID or Handle. A DOI identifier or an ItemID or a Handle is needed.\n")
			.withLongOpt("update-doi")
			.create();
		
		options.addOption(update);
		
		CommandLineParser parser = new PosixParser();
		CommandLine line = null;
		HelpFormatter helpFormatter = new HelpFormatter();
		
		try {
			line = parser.parse(options, args);
		}
		catch (ParseException ex) {
			log.error("Exception parsing command line", ex);
			System.exit(1);
		}
		
		if (line.hasOption("h") || 0 == line.getOptions().length) {
			helpFormatter.printHelp("\nANU DOI organiser\n", options);
			return;
		}
		
		if (line.hasOption("q")) {
			organiser.setQuiet();
		}
		if (line.hasOption("c")) {
			String countStr = line.getOptionValue("c");
			organiser.setCount(Integer.parseInt(countStr));
		}
		else {
			helpFormatter.printHelp("\nMissing count option\nANU DOI organiser\n", options);
			return;
		}
		
		if (line.hasOption("l")) {
			organiser.list("registration", null, null, ANUIdentifierProvider.TO_BE_REGISTERED);
			organiser.list("reservation", null, null, ANUIdentifierProvider.TO_BE_RESERVED);
			organiser.list("update",  null, null, ANUIdentifierProvider.UPDATE_BEFORE_REGISTRATION
					, ANUIdentifierProvider.UPDATE_REGISTERED, ANUIdentifierProvider.UPDATE_RESERVED);
			organiser.list("deactivate", null, null, ANUIdentifierProvider.TO_BE_DEACTIVATED);
			organiser.list("reactivate", null, null, ANUIdentifierProvider.TO_BE_ACTIVATED);
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

    public void list(String processName, PrintStream out, PrintStream err, Integer ... status) {

    	String indent = "    ";
    	if (null == out) {
    		out = System.out;
    	}
    	if (null == err) {
    		err = System.err;
    	}

    	TableRowIterator it = this.getDOIsByStatus(count, status);
    	
    	try {
    		if (it.hasNext()) {
    			out.println("DOIs queued for " + processName + ": ");
    		}
    		else {
    			out.println("There are no DOIs queued for " + processName + ".");
    		}
    		while(it.hasNext()) {
    			TableRow doiRow = it.next(context);
    			DSpaceObject dso = DSpaceObject.find(context,  doiRow.getIntColumn("resource_type_id")
    					, doiRow.getIntColumn("resource_id"));
    			out.print(indent + DOI.SCHEME + doiRow.getStringColumn("doi"));
    			if (null != dso) {
    				out.println(" (belongs to item with handle " + dso.getHandle() + ")");
    			}
    			else {
    				out.println(" (cannot determine handle of assigned object)");
    			}
    		}
    		out.println("");
    	}
    	catch (SQLException e) {
    		err.println("Error in database connection: " + e.getMessage());
    		e.printStackTrace(err);
    	}
    	finally {
    		it.close();
    	}
    }
    
    public void reserveAll() {
    	TableRowIterator it = getDOIsByStatus(count, ANUIdentifierProvider.TO_BE_RESERVED);
    	
    	try {

    		Map<DSpaceObject, String> errorObjects = new HashMap<DSpaceObject, String>();
    		if (!it.hasNext()) {
    			System.err.println("There are no objects in the database that could be reserved.");
    		}
    		while (it.hasNext()) {
    			TableRow doiRow = it.next(context);
    			DSpaceObject dso = DSpaceObject.find(context, doiRow.getIntColumn("resource_type_id"), doiRow.getIntColumn("resource_id"));
    			try {
    				reserve(doiRow, dso);
    			}
    			catch (IdentifierException e) {
    				errorObjects.put(dso, e.getMessage());
    				log.error("Exception reserving doi", e);
    			}
    		}
    		if (errorObjects.size() > 0) {
    			generateErrorEmail(errorObjects);
    		}
    	}
    	catch (SQLException e) {
    		System.err.println("Error in database connection: " + e.getMessage());
    		e.printStackTrace(System.err);
    	}
    }
    
    public void registerAll() {
    	TableRowIterator it = getDOIsByStatus(count, ANUIdentifierProvider.TO_BE_REGISTERED);
    	
    	try {

    		Map<DSpaceObject, String> errorObjects = new HashMap<DSpaceObject, String>();
    		if (!it.hasNext()) {
    			System.err.println("There are no objects in the database that could be registered.");
    		}
    		while(it.hasNext()) {
    			TableRow doiRow = it.next(context);
    			DSpaceObject dso = DSpaceObject.find(context, doiRow.getIntColumn("resource_type_id"), doiRow.getIntColumn("resource_id"));
    			try {
    				register(doiRow, dso);
    			}
    			catch (IdentifierException e) {
    				errorObjects.put(dso, e.getMessage());
    				log.error("Exception minting doi", e);
    			}
    		}
    		if (errorObjects.size() > 0) {
    			generateErrorEmail(errorObjects);
    		}
    	}
    	catch (SQLException e) {
    		System.err.println("Error in database connection: " + e.getMessage());
    		e.printStackTrace(System.err);
    	}
    }
    
    public void reserve(String identifier) throws IdentifierException {
    	try {
    		TableRow doiRow = findTableRow(identifier);
    		DSpaceObject dso = DSpaceObject.find(context, doiRow.getIntColumn("resource_type_id"), doiRow.getIntColumn("resource_id"));
    		reserve(doiRow, dso);
    	}
    	catch(IllegalArgumentException e) {
    		log.error("Database table DOI contains a DOI that is not valid " + identifier);
    	}
    	catch (SQLException e) {
    		log.error("Error while trying to get the data from the database", e);
    		if (!quiet) {
    			System.err.println("It wasn't possible to reserve an identifier for " + identifier);
    		}
    	}
    }
    
    public void reserve(TableRow doiRow, DSpaceObject dso) throws SQLException, IdentifierException {
    	if (Constants.ITEM != dso.getType()) {
    		throw new IllegalArgumentException("Current DSpace support DOIs for Items only.");
    	}
    	
    	try {
    		provider.reserveOnline(context, dso);
    	}
    	catch (IllegalArgumentException e) {
    		log.error("Database table DOI contains a DOI that is not valid: ID" + dso.getID());
    	}
    	catch (SQLException e) {
    		log.error("Error while trying to get data from database", e);
    		if (!quiet) {
    			System.err.println("It wasn't possible to register an identifier for " + dso.getID());
    		}
    	}
    }
    
    public void register(String identifier) throws IdentifierException {
		try {
			TableRow doiRow = findTableRow(identifier);
			DSpaceObject dso = DSpaceObject.find(context, doiRow.getIntColumn("resource_type_id"), doiRow.getIntColumn("resource_id"));
			register(doiRow, dso);
		}
		catch (IllegalArgumentException e) {
			log.error("Database table DOI contains a DOI that is not valid " + identifier);
		}
		catch (SQLException e) {
			log.error("Error while trying to get data from database", e);
			if (!quiet) {
				System.err.println("It wasn't possible to register an identifier for " + identifier);
			}
		}
    }

    public void register(TableRow doiRow, DSpaceObject dso) throws SQLException, IdentifierException {
    	if (Constants.ITEM != dso.getType()) {
    		throw new IllegalArgumentException("Current DSpace supports DOIs for Items only.");
    	}
    	
    	try {
    		provider.registerOnline(context, dso);
    	}
    	catch (IllegalArgumentException e) {
    		log.error("Database table DOI contains a DOI that is not valid: ID" + dso.getID());
    	}
    	catch (SQLException e) {
    		log.error("Error while trying to get data from database", e);
    		if (!quiet) {
    			System.err.println("It wasn't possible to register an identifier for " + dso.getID());
    		}
    	}
    }
    
    public void updateAll() {
    	TableRowIterator it = getDOIsByStatus(count, ANUIdentifierProvider.UPDATE_RESERVED, ANUIdentifierProvider.UPDATE_REGISTERED, ANUIdentifierProvider.UPDATE_BEFORE_REGISTRATION);
    	
    	try {
    		if (!it.hasNext()) {
    			System.err.println("There are no objects in the database that are set to be updated.");
    		}
    		while(it.hasNext()) {
    			TableRow doiRow = it.next(context);
    			DSpaceObject dso = DSpaceObject.find(context, doiRow.getIntColumn("resource_type_id"), doiRow.getIntColumn("resource_id"));
    			update(doiRow, dso);
    		}
    	}
    	catch (SQLException e) {
    		System.err.println("Error in database connection: " + e.getMessage());
    		e.printStackTrace(System.err);
    	}
    }
    
    public void update(String identifier) {
		try {
			TableRow doiRow = findTableRow(identifier);
			DSpaceObject dso = DSpaceObject.find(context, doiRow.getIntColumn("resource_type_id"), doiRow.getIntColumn("resource_id"));
			update(doiRow, dso);
		}
		catch (IdentifierException e) {
			log.error("It wasn't possible to update the object with the identifier " + identifier, e);
			
		}
		catch (IllegalArgumentException e) {
			log.error("Database table DOI contains a DOI that is not valid " + identifier);
		}
		catch (SQLException e) {
			log.error("Error while trying to get data from database", e);
			if (!quiet) {
				System.err.println("It wasn't possible to register an identifier for " + identifier);
			}
		}
    }
    
    public void update(TableRow doiRow, DSpaceObject dso) {
    	if (Constants.ITEM != dso.getType()) {
    		throw new IllegalArgumentException("Current DSpace supports DOIs for Items only.");
    	}
    	
    	try {
    		provider.updateMetadataOnline(context, dso);
    	}
    	catch (IdentifierException e) {
    		log.error("It wasn't possible to update an identifier for the object with an id of " + dso.getID(), e);
    		
    	}
    	catch (IllegalArgumentException e) {
    		log.error("Database table DOI contains a DOI that is not valid: ID" + dso.getID());
    	}
    	catch (SQLException e) {
    		log.error("Error while trying to get data from database", e);
    		if (!quiet) {
    			System.err.println("It wasn't possible to update an identifier for " + dso.getID());
    		}
    	}
    }
    
    public void deactivateAll() {
    	TableRowIterator it = getDOIsByStatus(ANUIdentifierProvider.TO_BE_DEACTIVATED);
    	
    	try {
    		if (!it.hasNext()) {
    			System.err.println("There are no objects in the database that are to be deactivated.");
    		}
    		while(it.hasNext()) {
    			TableRow doiRow = it.next(context);
    			DSpaceObject dso = DSpaceObject.find(context, doiRow.getIntColumn("resource_type_id"), doiRow.getIntColumn("resource_id"));
    			deactivate(doiRow, dso);
    		}
    	}
    	catch (SQLException e) {
    		System.err.println("Error in database connection: " + e.getMessage());
    		e.printStackTrace(System.err);
    	}
    }
    
    public void deactivate(TableRow doiRow, DSpaceObject dso) {
    	if (Constants.ITEM != dso.getType()) {
    		throw new IllegalArgumentException("Current DSpace supports DOIs for Items only.");
    	}
    	
    	try {
    		provider.deactivateOnline(context, dso);
    	}
    	catch (IdentifierException e) {
    		log.error("It wasn't possible to deactivate an identifier for the object with an id of " + dso.getID(), e);
    	}
    	catch (IllegalArgumentException e) {
    		log.error("Database table DOI contains a DOI that is not valid: ID" + dso.getID());
    	}
    	catch (SQLException e) {
    		log.error("Error while trying to get data from database", e);
    		if (!quiet) {
    			System.err.println("It wasn't possible to deactivate an identifier for " + dso.getID());
    		}
    	}
    }
    
    public void reactivateAll() {
    	TableRowIterator it = getDOIsByStatus(count, ANUIdentifierProvider.TO_BE_ACTIVATED);
    	
    	try {
    		if (!it.hasNext()) {
    			System.err.println("There are no objects in the database that could are to be reactivated.");
    		}
    		while(it.hasNext()) {
    			TableRow doiRow = it.next(context);
    			DSpaceObject dso = DSpaceObject.find(context, doiRow.getIntColumn("resource_type_id"), doiRow.getIntColumn("resource_id"));
    			reactivate(doiRow, dso);
    		}
    	}
    	catch (SQLException e) {
    		System.err.println("Error in database connection: " + e.getMessage());
    		e.printStackTrace(System.err);
    	}
    }
    
    public void reactivate(TableRow doiRow, DSpaceObject dso) {
    	if (Constants.ITEM != dso.getType()) {
    		throw new IllegalArgumentException("Current DSpace supports DOIs for Items only.");
    	}
    	
    	try {
    		provider.activateOnline(context, dso);
    	}
    	catch (IdentifierException e) {
    		log.error("It wasn't possible to re-activate an identifier for the object with an id of " + dso.getID(), e);
    	}
    	catch (IllegalArgumentException e) {
    		log.error("Database table DOI contains a DOI that is not valid: ID" + dso.getID());
    	}
    	catch (SQLException e) {
    		log.error("Error while trying to get data from database", e);
    		if (!quiet) {
    			System.err.println("It wasn't possible to re-activate an identifier for " + dso.getID());
    		}
    	}
    }
    
    public TableRow findTableRow(String identifier) throws SQLException, IllegalArgumentException
    		, IllegalStateException, IdentifierException {
    	if (null == identifier || identifier.isEmpty()) {
    		throw new IllegalArgumentException("Identifier is null or empty");
    	}
    	
    	String sql = "SELECT * FROM Doi WHERE resource_type_id = ? AND resource_id = ? ";
    	TableRow doiRow = null;
    	
    	// detect if identifier is an ItemID, or handle
    	// try to detect ItemID
    	if (identifier.matches("\\d*")) {
    		Integer itemID = Integer.valueOf(identifier);
    		DSpaceObject dso = Item.find(context, itemID);
    		
    		if (null != dso) {
    			doiRow = DatabaseManager.querySingleTable(context, "Doi", sql, Constants.ITEM, dso.getID());
    			
    			if (null == doiRow) {
    				// should we be creating a doirow if one doesn't exist?
        			throw new IllegalArgumentException("Record is not marked for doi minting");
    			}
    			return doiRow;
    		}
    		else {
    			throw new IllegalStateException("You specified an itemID  that is not stored in our database.");
    		}
    	}
    	
    	DSpaceObject dso = HandleManager.resolveToObject(context, identifier);
    	
    	if (null != dso) {
    		if (dso.getType() != Constants.ITEM) {
    			throw new IllegalArgumentException ("Currently DSpace supports DOIs for Items only. Cannot "
    					+ "process specified handle as it does not identify as an Item.");
    		}
    		doiRow = DatabaseManager.querySingleTable(context, "Doi", sql, Constants.ITEM, dso.getID());
    		
    		if (null == doiRow) {
    			throw new IllegalArgumentException("Record is not marked for doi minting");
    		}
    	}
    	
    	return doiRow;
    }
    
    public TableRowIterator getDOIsByStatus(Integer maxRecords, Integer ... status) {
    	try {
    		String sql = "SELECT * FROM Doi";
    		for (int i = 0; i < status.length; i++) {
    			if (0 == i) {
    				sql += " WHERE ";
    			}
    			else {
    				sql += " OR ";
    			}
    			sql += " status = ?";
    		}
    		if (maxRecords != null) {
    			sql += " limit " + maxRecords;
    		}
    		if (status.length < 1) {
    			return DatabaseManager.queryTable(context, "Doi", sql);
    		}
    		return DatabaseManager.queryTable(context, "Doi", sql, status);
    	}
    	catch (SQLException ex) {
    		log.error("Error while trying to get data from database", ex);
    		throw new RuntimeException("Error while trying to get data from database", ex);
    	}
    }
    
    private void generateErrorEmail(Map<DSpaceObject, String> errorObjects) {
    	String quote = "\"";
		StringWriter writer = new StringWriter();
		writer.append("id,handle,dc.date.issued,dc.date.created,error_msg\n");
		for (Entry<DSpaceObject, String> object : errorObjects.entrySet()) {
			DSpaceObject dso = object.getKey();
			String errorMessage = object.getValue();
			writer.append(Integer.toString(dso.getID()));
			writer.append(",");
			writer.append(quote);
			writer.append("http://hdl.handle.net/");
			writer.append(dso.getHandle());
			writer.append(quote);
			writer.append(",");
			Metadatum[] dateIssued = dso.getMetadata("dc", "date", "issued", null);
			if (dateIssued != null && dateIssued.length > 0) {
				writer.append(quote);
				writer.append(dateIssued[0].value);
				writer.append(quote);
			}
			writer.append(",");
			Metadatum[] dateCreated = dso.getMetadata("dc", "date", "created", null);
			if (dateCreated != null && dateCreated.length > 0) {
				writer.append(quote);
				writer.append(dateCreated[0].value);
				writer.append(quote);
			}
			writer.append(",");
			writer.append(quote);
			writer.append(errorMessage);
			writer.append(quote);
			writer.append("\n");
		}
		
		Email email = new Email();
		email.setSubject("Records where there are issues creating DOI's");
		email.setContent("Please find attached the items that had errors when minting a doi");
		
		email.addRecipient(ConfigurationManager.getProperty("mail.helpdesk"));
		InputStream is = new ByteArrayInputStream(writer.toString().getBytes());
		email.addAttachment(is, "doi_exception_report.csv", "text/csv");
		try {
			email.send();
		}
		catch (IOException | MessagingException e) {
			log.error("Exception sending doi failure email");
		}
    }
	
	private void setQuiet() {
		this.quiet = true;
	}
	
	private void setCount(Integer count) {
		this.count = count;
	}
    
}
