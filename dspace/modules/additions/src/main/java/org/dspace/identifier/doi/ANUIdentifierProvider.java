package org.dspace.identifier.doi;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.Identifier;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierProvider;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ANUIdentifierProvider extends IdentifierProvider {

    private static final Logger log = LoggerFactory.getLogger(ANUIdentifierProvider.class);
    
	private static final String SELECT_DOI = "SELECT * FROM Doi WHERE resource_type_id = ? " +
			"AND resource_id = ?";
	
	private static final String SELECT_DEACTIVATED = "SELECT * FROM Doi WHERE resource_type_id = ? AND resource_id = ? "
			+ "AND (status = ? OR status = ?)";
    
    
    static final String CFG_PREFIX = "identifier.doi.prefix";
    static final String CFG_NAMESPACE_SEPARATOR = "identifier.doi.namespaceseparator";
    
    public static final Integer TO_BE_REGISTERED = 1;
    public static final Integer TO_BE_RESERVED = 2;
    public static final Integer IS_REGISTERED = 3;
    public static final Integer IS_RESERVED = 4;
    public static final Integer UPDATE_RESERVED = 5;
    public static final Integer UPDATE_REGISTERED = 6;
    public static final Integer UPDATE_BEFORE_REGISTRATION = 7;
    public static final Integer TO_BE_DEACTIVATED = 8;
    public static final Integer DEACTIVATED = 9;
    public static final Integer TO_BE_ACTIVATED = 10;
    
    private static final String DOI_SCHEMA = "local";
    private static final String DOI_ELEMENT = "identifier";
    private static final String DOI_QUALIFIER = "doi";
    
    protected ConfigurationService configurationService;
    protected ANUConnector anuConnector;

    private String PREFIX;
    
    public void setConfigurationService(ConfigurationService configurationService) {
    	this.configurationService = configurationService;
    }
    
    public void setANUConnector(ANUConnector anuConnector) {
    	this.anuConnector = anuConnector;
    }
    
    protected String getPrefix() {
    	if (null == this.PREFIX) {
    		this.PREFIX = this.configurationService.getProperty(CFG_PREFIX);
    		if (null == this.PREFIX) {
    			log.warn("Cannot find DOI prefix in configuration!");
    			throw new RuntimeException("Unable to load DOI prefix from configuration. Cannot "
    					+ "find property " + CFG_PREFIX + ".");
    		}
    	}
    	return this.PREFIX;
    }
    
	@Override
	public boolean supports(Class<? extends Identifier> identifier) {
		return DOI.class.isAssignableFrom(identifier);
	}

	@Override
	public boolean supports(String identifier) {
		try {
			DOI.formatIdentifier(identifier);
		}
		catch (IdentifierException e) {
			return false;
		}
		catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}

	@Override
	public String mint(Context context, DSpaceObject dso) throws IdentifierException {
		// Do nothing
		return null;
	}

	@Override
	public DSpaceObject resolve(Context context, String identifier, String... attributes)
			throws IdentifierNotFoundException, IdentifierNotResolvableException {
		//Do nothing
		return null;
	}

	@Override
	public void reserve(Context context, DSpaceObject dso, String identifier) throws IdentifierException {
		
	}

	@Override
	public void register(Context context, DSpaceObject object, String identifier) throws IdentifierException {
		register(context, object);
	}

	@Override
	public String register(Context context, DSpaceObject item) throws IdentifierException {
		if (!checkForMintValue(context, item)) {
			log.debug("The item {} does not have mint in local.mintdoi so no need to add a doi row", item.getID());
			return null;
		}
		
		try {
			loadOrCreateDOI(context, item);
		}
		catch (SQLException e) {
			log.error("Exception creating or retrieving doi record", e);
		}
		return null;
	}
	
	public void updateMetadata(Context context, DSpaceObject dso, String identifier) throws DOIIdentifierException, SQLException {
		TableRow doiRow = null;
		// may need to do something about this? We don't want to create it if not created I think
		doiRow = loadDOI(context, dso);
		
		if (DEACTIVATED == doiRow.getIntColumn("status") || TO_BE_DEACTIVATED == doiRow.getIntColumn("status")) {
			log.debug("There is no need to update/register doi information for a deactivated record");
			return;
		}
		if (IS_REGISTERED == doiRow.getIntColumn("status")) {
			doiRow.setColumn("status",  UPDATE_REGISTERED);
		}
		else if (IS_RESERVED == doiRow.getIntColumn("status")) {
			doiRow.setColumn("status", UPDATE_RESERVED);
		}
		else if (TO_BE_REGISTERED == doiRow.getIntColumn("status")) {
			doiRow.setColumn("status", UPDATE_BEFORE_REGISTRATION);
		}
		else {
			return;
		}
		DatabaseManager.update(context, doiRow);
	}
	
	private boolean checkForMintValue(Context context, DSpaceObject item) {
		String mintdoi = item.getMetadata("local.mintdoi");
		if ("mint".equals(mintdoi)) {
			return true;
		}
		return false;
	}

	@Override
	public String lookup(Context context, DSpaceObject dso)
			throws IdentifierNotFoundException, IdentifierNotResolvableException {
		String doi = null;
		try {
			doi = getDOIByObject(context, dso);
		}
		catch (SQLException e) {
			throw new RuntimeException("Error retrieving DOI out of database.", e);
		}
		if (null == doi) {
			throw new IdentifierNotFoundException("No DOI for DSpaceObject of type " + dso.getTypeText() + " with ID " + dso.getID() + " found.");
		}
		
		return doi;
	}

	@Override
	public void delete(Context context, DSpaceObject dso) throws IdentifierException {
		delete(context, dso, null);
	}

	@Override
	public void delete(Context context, DSpaceObject dso, String identifier) throws IdentifierException {
		try {
			TableRow doiRow = DatabaseManager.querySingleTable(context, "Doi", SELECT_DOI, dso.getType(), dso.getID());
			
			if (doiRow != null && doiRow.isColumnNull("doi")) {
				DatabaseManager.delete(context, doiRow);
			}
			else if (doiRow != null && !ANUIdentifierProvider.TO_BE_DEACTIVATED.equals(doiRow.getIntColumn("status")) 
					&& !ANUIdentifierProvider.DEACTIVATED.equals(doiRow.getIntColumn("status"))) {
				doiRow.setColumn("status", TO_BE_DEACTIVATED);
				DatabaseManager.update(context, doiRow);
			}
			else {
				log.debug("Status is already set to deactivated/to be deactivated");
			}
		}
		catch (SQLException e) {
			log.error("Exception retrieving sql row", e);
		}
	}
	
	public void reserveOnline(Context context, DSpaceObject dso) throws IdentifierException, IllegalArgumentException, SQLException {
		TableRow doiRow = DatabaseManager.querySingleTable(context, "Doi", SELECT_DOI, dso.getType(), dso.getID());

		if (TO_BE_RESERVED == doiRow.getIntColumn("status")) {
			try {
				String doi = anuConnector.reserveDOI(context, dso);
				log.info("Reserved doi {} for item {}", doi, dso.getID());
				doiRow.setColumn("doi", doi);
				doiRow.setColumn("status", TO_BE_REGISTERED);
				DatabaseManager.update(context, doiRow);
				dso.addMetadata(DOI_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null, doi);
				dso.update();
				context.commit();
			}
			/*catch (DOIIdentifierException e) {
				log.error("Exception minting doi", e);
			}*/
			catch (AuthorizeException e) {
				log.error("Exception updating item {} with the metadata for doi", dso.getID());
			}
		}
	}
	
	public void registerOnline(Context context, DSpaceObject dso) throws IdentifierException, IllegalArgumentException, SQLException {
		TableRow doiRow = DatabaseManager.querySingleTable(context, "Doi", SELECT_DOI, dso.getType(), dso.getID());

		if (TO_BE_REGISTERED == doiRow.getIntColumn("status")) {
			anuConnector.registerDOI(context, dso, doiRow.getStringColumn("doi"));
			doiRow.setColumn("status", IS_REGISTERED);
			DatabaseManager.update(context, doiRow);
			context.commit();
		}
	}
	
	public void updateMetadataOnline(Context context, DSpaceObject dso) 
			throws IdentifierException, SQLException {
		TableRow doiRow = null;
		try {
			doiRow = loadDOI(context, dso);
		}
		catch (SQLException e) {
			log.warn("SQLException while searching a DOI in our db.");
			throw new RuntimeException("Unable to retrieve information about a DOI out of database.", e);
		}
		if (null == doiRow) {
			log.error("Cannot update metadata for item {}: unable to find it in our db.", dso.getID());
			throw new DOIIdentifierException("Unable to find DOI.", DOIIdentifierException.DOI_DOES_NOT_EXIST);
		}
		String doi = doiRow.getStringColumn("doi");
		if (doiRow.getIntColumn("resource_id") != dso.getID() || doiRow.getIntColumn("resource_type_id") != dso.getType()) {
			log.error("Refused to update metadata of DOI {} with the metadata of an object ({}/{} the DOI is not "
					+ "dedicated to.", new String[] {doi, dso.getTypeText(), Integer.toString(dso.getID())});
			throw new DOIIdentifierException("Cannot update DOI metadata: DOI and DSpaceObject does not match!", 
					DOIIdentifierException.MISMATCH);
		}
		if (DEACTIVATED == doiRow.getIntColumn("status") || TO_BE_DEACTIVATED == doiRow.getIntColumn("status")) {
			throw new DOIIdentifierException("You tried to update the metadata of a DOI that is marked as DEACTIVATED."
					, DOIIdentifierException.DOI_IS_DELETED);
		}
		
		anuConnector.updateMetadata(context, dso, doi);
		
		if (UPDATE_REGISTERED == doiRow.getIntColumn("status")) {
			doiRow.setColumn("status", IS_REGISTERED);
		}
		else if (UPDATE_BEFORE_REGISTRATION == doiRow.getIntColumn("status")) {
			doiRow.setColumn("status", TO_BE_REGISTERED);
		}
		else if (UPDATE_RESERVED == doiRow.getIntColumn("status")) {
			doiRow.setColumn("status", IS_RESERVED);
		}
		DatabaseManager.update(context, doiRow);
	}
	
	public void activate(Context context, DSpaceObject dso) throws SQLException {
		try {
			TableRow doiRow = DatabaseManager.querySingleTable(context, "Doi", SELECT_DEACTIVATED, dso.getType(), dso.getID()
					, TO_BE_DEACTIVATED, DEACTIVATED);
			if (doiRow == null) {
				return;
			}
			if (DEACTIVATED == doiRow.getIntColumn("status")) {
				doiRow.setColumn("status", TO_BE_ACTIVATED);
			}
			else if (TO_BE_DEACTIVATED == doiRow.getIntColumn("status")) {
				doiRow.setColumn("status", ANUIdentifierProvider.UPDATE_REGISTERED);
			}
			DatabaseManager.update(context, doiRow);
		}
		catch (SQLException e) {
			log.info("No row found");
		}
	}
	
	public void activateOnline(Context context, DSpaceObject dso) throws IdentifierException, SQLException {
		TableRow doiRow = DatabaseManager.querySingleTable(context, "Doi", SELECT_DOI, dso.getType(), dso.getID());
		if (ANUIdentifierProvider.TO_BE_ACTIVATED.equals(doiRow.getIntColumn("status"))) {
			String doi = doiRow.getStringColumn("doi");

			anuConnector.registerDOI(context, dso, doi);
			doiRow.setColumn("status", ANUIdentifierProvider.IS_REGISTERED);
			DatabaseManager.update(context, doiRow);
		}
	}
	
	public void deactivateOnline(Context context, DSpaceObject dso) throws IdentifierException, SQLException {
		TableRow doiRow = loadDOI(context, dso);
		if (ANUIdentifierProvider.TO_BE_DEACTIVATED.equals(doiRow.getIntColumn("status"))) {
			String doi = doiRow.getStringColumn("doi");
			anuConnector.deleteDOI(context, doi);
			doiRow.setColumn("status", ANUIdentifierProvider.DEACTIVATED);
			DatabaseManager.update(context, doiRow);
		}
		
	}
	
	public static String getDOIByObject(Context context, DSpaceObject dso) throws SQLException {
		TableRow doiRow = DatabaseManager.querySingleTable(context, "Doi", SELECT_DOI, dso.getType(), dso.getID());
		if (null == doiRow) {
			return null;
		}
		if (doiRow.isColumnNull("doi")) {
			return null;
		}
		return DOI.SCHEME + doiRow.getStringColumn("doi");
	}
	
	protected TableRow loadDOI(Context context, DSpaceObject dso) throws SQLException, DOIIdentifierException {
		TableRow doiRow = null;
		
		doiRow = DatabaseManager.querySingleTable(context, "Doi", SELECT_DOI, dso.getType(), dso.getID());
		
		return doiRow;
	}
	
	protected TableRow loadOrCreateDOI(Context context, DSpaceObject dso) 
			throws SQLException, DOIIdentifierException {
		log.debug("In loadOrCreateDOI");
		TableRow doiRow = null;
		
		try {
			doiRow = loadDOI(context, dso);
			log.debug("Table row found");
		}
		catch (SQLException e) {
			doiRow = DatabaseManager.create(context, "Doi");
		}
		if (null == doiRow) {
			doiRow = DatabaseManager.create(context, "Doi");
		}
		doiRow.setColumn("resource_type_id", dso.getType());
		doiRow.setColumn("resource_id", dso.getID());
		doiRow.setColumn("status", TO_BE_RESERVED);
		if (0 == DatabaseManager.update(context, doiRow)) {
			throw new RuntimeException("Cannot save DOI to database for unknown reason.");
		}
		
		return doiRow;
	}
}
