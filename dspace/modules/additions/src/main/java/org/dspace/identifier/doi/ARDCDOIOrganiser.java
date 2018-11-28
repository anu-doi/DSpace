package org.dspace.identifier.doi;

import java.io.PrintStream;
import java.sql.SQLException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.jena.atlas.logging.Log;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.DOI;
import org.dspace.identifier.IdentifierException;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.utils.DSpace;

public class ARDCDOIOrganiser {
    private static final Logger LOG = Logger.getLogger(ARDCDOIOrganiser.class);
    
	ARDCIdentifierProvider provider;
	private Context context;
	private boolean quiet;
	
	public ARDCDOIOrganiser(Context context, ARDCIdentifierProvider provider) {
		this.context = context;
		this.provider = provider;
		this.quiet = false;
	}
	
	public static void main(String[] args) {
		LOG.debug("Starting DOI organiser");
		
		Context context = null;
		try {
			context = new Context();
		}
		catch (SQLException e) {
			System.err.println("Cannot connect to database: " + e.getMessage());
			System.exit(-1);
		}
		
		context.turnOffAuthorisationSystem();
		
		ARDCDOIOrganiser organiser = new ARDCDOIOrganiser(context, new DSpace().getSingletonService(ARDCIdentifierProvider.class));
		
		runCLI(context, organiser, args);
		
		try {
			context.complete();
		}
		catch (SQLException e) {
			System.err.println("Cannot save changes to database: " + e.getMessage());
			System.exit(-1);
		}
	}

    @SuppressWarnings("static-access")
	public static void runCLI(Context context, ARDCDOIOrganiser organiser, String[] args) {
		Options options = new Options();
		options.addOption("h", "help", false, "Help");
		options.addOption("l", "list", false, "List all objects to be reserved, registered, deleted, or updated");
		options.addOption("m", "mint-all", false, "Perform online registration "
				+ "for all identifiers queued for registration.");
		options.addOption("u", "update-all", false, "Perform online metadata update "
				+ "for all identifiers queued for metadata update.");
		options.addOption("d", "deactivate-all", false, "Perform online deactivation for all idnetifiers queued for deactivation");
		options.addOption("a", "reactivate-all", false, "Perform online re-activation for all idnetifiers queued for re-activation");
//		options.addOption("q", "quiet", false, "Turn the command line output off.");
//		
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
			LOG.fatal(ex);
			System.exit(1);
		}
		
		if (line.hasOption("h") || 0 == line.getOptions().length) {
			helpFormatter.printHelp("\nARDC DOI organiser\n", options);
		}
		
		if (line.hasOption("q")) {
			organiser.setQuiet();
		}
		
		if (line.hasOption("l")) {
			organiser.list("registration", null, null, ARDCIdentifierProvider.TO_BE_REGISTERED);
			organiser.list("reservation", null, null, ARDCIdentifierProvider.TO_BE_RESERVED);
			organiser.list("update",  null, null, ARDCIdentifierProvider.UPDATE_BEFORE_REGISTRATION
					, ARDCIdentifierProvider.UPDATE_REGISTERED, ARDCIdentifierProvider.UPDATE_RESERVED);
			organiser.list("deactivate", null, null, ARDCIdentifierProvider.TO_BE_DEACTIVATED);
			organiser.list("reactivate", null, null, ARDCIdentifierProvider.TO_BE_ACTIVATED);
			
		}
		if (line.hasOption("m")) {
			organiser.mintAll();
		}
		if (line.hasOption("register-doi")) {
			String identifier = line.getOptionValue("register-doi");
			if (null == identifier) {
				helpFormatter.printHelp("\nDOI organiser\n", options);
			}
			organiser.mint(identifier);
		}
		if (line.hasOption("u")) {
			organiser.updateAll();
		}
		if (line.hasOption("update-doi")) {
			String identifier = line.getOptionValue("update-doi");
			if (null == identifier) {
				helpFormatter.printHelp("\nDOI organiser\n", options);
			}
			organiser.update(identifier);
		}
		if (line.hasOption("d")) {
			organiser.deactivateAll();
		}
		if (line.hasOption("a")) {
			organiser.reactivateAll();
		}
//		if (line.hasOption(opt))
	}
    
    public void list(String processName, PrintStream out, PrintStream err, Integer ... status) {
    	String indent = "    ";
    	if (null == out) {
    		out = System.out;
    	}
    	if (null == err) {
    		err = System.err;
    	}
    	
    	TableRowIterator it = this.getDOIsByStatus(status);
    	
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
    
    public TableRowIterator getDOIsByStatus(Integer ... status) {
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
    		if (status.length < 1) {
    			return DatabaseManager.queryTable(context, "Doi", sql);
    		}
    		return DatabaseManager.queryTable(context, "Doi", sql, status);
    	}
    	catch (SQLException ex) {
    		LOG.error("Error while trying to get data from database", ex);
    		throw new RuntimeException("Error while trying to get data from database", ex);
    	}
//    	return null;
    }
    
    public void mintAll() {
    	TableRowIterator it = getDOIsByStatus(ARDCIdentifierProvider.TO_BE_REGISTERED);
    	
    	try {
    		if (!it.hasNext()) {
    			System.err.println("There are no objects in the database that could be registered.");
    		}
    		while(it.hasNext()) {
    			TableRow doiRow = it.next(context);
    			DSpaceObject dso = DSpaceObject.find(context, doiRow.getIntColumn("resource_type_id"), doiRow.getIntColumn("resource_id"));
    			mint(doiRow, dso);
    		}
    	}
    	catch (SQLException e) {
    		System.err.println("Error in database connection: " + e.getMessage());
    		e.printStackTrace(System.err);
    	}
    }
    
    public void mint(String identifier) {
//    	register-doi
    	
		try {
			TableRow doiRow = findTableRow(identifier);
			DSpaceObject dso = DSpaceObject.find(context, doiRow.getIntColumn("resource_type_id"), doiRow.getIntColumn("resource_id"));
			mint(doiRow, dso);
		}
		catch (IdentifierException e) {
			LOG.error("It wasn't possible to register an identifier for the object with the identifier " + identifier, e);
			
		}
		catch (IllegalArgumentException e) {
			LOG.error("Database table DOI contains a DOI that is not valid " + identifier);
		}
		catch (SQLException e) {
			LOG.error("Error while trying to get data from database", e);
			if (!quiet) {
				System.err.println("It wasn't possible to register an identifier for " + identifier);
			}
		}
    }
    
    public void mint(TableRow doiRow, DSpaceObject dso) throws SQLException {
    	if (Constants.ITEM != dso.getType()) {
    		throw new IllegalArgumentException("Current DSpace supports DOIs for Items only.");
    	}
    	
    	try {
    		provider.registerOnline(context, dso);
    	}
    	catch (IdentifierException e) {
    		LOG.error("It wasn't possible to register an identifier for the object with an id of " + dso.getID(), e);
    		
    	}
    	catch (IllegalArgumentException e) {
    		LOG.error("Database table DOI contains a DOI that is not valid: ID" + dso.getID());
    	}
    	catch (SQLException e) {
    		LOG.error("Error while trying to get data from database", e);
    		if (!quiet) {
    			System.err.println("It wasn't possible to register an identifier for " + dso.getID());
    		}
    	}
    }
    

    public void updateAll() {
    	TableRowIterator it = getDOIsByStatus(ARDCIdentifierProvider.UPDATE_REGISTERED);
    	
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
    	LOG.info("In update(String)");
		try {
			TableRow doiRow = findTableRow(identifier);
			DSpaceObject dso = DSpaceObject.find(context, doiRow.getIntColumn("resource_type_id"), doiRow.getIntColumn("resource_id"));
			update(doiRow, dso);
		}
		catch (IdentifierException e) {
			LOG.error("It wasn't possible to update the object with the identifier " + identifier, e);
			
		}
		catch (IllegalArgumentException e) {
			LOG.error("Database table DOI contains a DOI that is not valid " + identifier);
		}
		catch (SQLException e) {
			LOG.error("Error while trying to get data from database", e);
			if (!quiet) {
				System.err.println("It wasn't possible to register an identifier for " + identifier);
			}
		}
    }
    
    public void update(TableRow doiRow, DSpaceObject dso) {
    	LOG.info("In update(TableRow, DSpaceObject");
    	if (Constants.ITEM != dso.getType()) {
    		throw new IllegalArgumentException("Current DSpace supports DOIs for Items only.");
    	}
    	
    	try {
    		provider.updateMetadataOnline(context, dso);
    	}
    	catch (IdentifierException e) {
    		LOG.error("It wasn't possible to update an identifier for the object with an id of " + dso.getID(), e);
    		
    	}
    	catch (IllegalArgumentException e) {
    		LOG.error("Database table DOI contains a DOI that is not valid: ID" + dso.getID());
    	}
    	catch (SQLException e) {
    		LOG.error("Error while trying to get data from database", e);
    		if (!quiet) {
    			System.err.println("It wasn't possible to update an identifier for " + dso.getID());
    		}
    	}
    }
    
    public void deactivateAll() {
    	TableRowIterator it = getDOIsByStatus(ARDCIdentifierProvider.TO_BE_DEACTIVATED);
    	
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
    	LOG.info("In deactivate(TableRow, DSpaceObject");
    	if (Constants.ITEM != dso.getType()) {
    		throw new IllegalArgumentException("Current DSpace supports DOIs for Items only.");
    	}
    	
    	try {
    		provider.deactivateOnline(context, dso);
    	}
    	catch (IdentifierException e) {
    		LOG.error("It wasn't possible to deactivate an identifier for the object with an id of " + dso.getID(), e);
    		
    	}
    	catch (IllegalArgumentException e) {
    		LOG.error("Database table DOI contains a DOI that is not valid: ID" + dso.getID());
    	}
    	catch (SQLException e) {
    		LOG.error("Error while trying to get data from database", e);
    		if (!quiet) {
    			System.err.println("It wasn't possible to deactivate an identifier for " + dso.getID());
    		}
    	}
    }
    
    public void reactivateAll() {
    	TableRowIterator it = getDOIsByStatus(ARDCIdentifierProvider.TO_BE_ACTIVATED);
    	
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
    		LOG.error("It wasn't possible to re-activate an identifier for the object with an id of " + dso.getID(), e);
    	}
    	catch (IllegalArgumentException e) {
    		LOG.error("Database table DOI contains a DOI that is not valid: ID" + dso.getID());
    	}
    	catch (SQLException e) {
    		LOG.error("Error while trying to get data from database", e);
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
    
    
	private void setQuiet() {
		this.quiet = true;
	}
}
