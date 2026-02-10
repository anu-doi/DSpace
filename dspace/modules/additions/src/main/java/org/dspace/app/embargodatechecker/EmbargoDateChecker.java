/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.embargodatechecker;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.apache.commons.cli.ParseException;
import org.dspace.app.util.DSpaceObjectUtilsImpl;
import org.dspace.app.util.service.DSpaceObjectUtils;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

/**
 * Implementation of {@link DSpaceRunnable} to get the Embargo
 * lift/start dates of requested Collection or Community UUID or the whole
 * site between range of dates or specific duration 
 * (number of days) via CSV file.
 *
 * @author Akshay Karthik
 *
 */
public class EmbargoDateChecker extends DSpaceRunnable<EmbargoDateCheckerScriptConfiguration> {

	private DSpaceObjectUtils dSpaceObjectUtils;

	private ItemService itemService;

	protected Context context;

	protected EPersonService ePersonService;

	private boolean help = false;

	protected String eperson = null;

	private String uuid;

	private String duration;

	private String rangeStartDate;

	private String rangeEndDate;

	protected CommunityService communityService;

	private LocalDate durationTime;
	
	private LocalDate startDateTemp;

	private Date startDate;
	
	private Date endDate;
	
	private boolean isValid;
	
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	String dateRegexPattern = "\\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])";

	private String filename = null;

	private static final String EXPORT_CSV = "exportCSV";

	ArrayList<ArrayList<String>> matrix = new ArrayList<>();

	protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance()
			.getBitstreamFormatService();

	@Override
	public void setup() throws ParseException {

		this.ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
		this.communityService = ContentServiceFactory.getInstance().getCommunityService();
		this.itemService = ContentServiceFactory.getInstance().getItemService();
		

		context = new Context();
		
		help = commandLine.hasOption('h');
		try {
			uuid = commandLine.hasOption('a') ? (ContentServiceFactory.getInstance().getSiteService().findSite(context).getID().toString()) : (commandLine.hasOption('u') ? commandLine.getOptionValue('u') : null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		filename = commandLine.hasOption('f')? commandLine.getOptionValue('f') : (uuid.toString() + ".csv");
		
		ArrayList<String> columnNames = new ArrayList<>();
		columnNames.add("Item UUID");
		columnNames.add("Item Handle");
		columnNames.add("Item Title");
		columnNames.add("Lift date");
		matrix.add(columnNames);
	}

	@Override
	public void internalRun() throws Exception {

		if (help) {
			printHelp();
			return;
		}

		context.turnOffAuthorisationSystem();
		isValid = validate(context);
        try {
            context.setCurrentUser(ePersonService.find(context, this.getEpersonIdentifier()));
        } catch (SQLException e) {
            handler.handleException(e);
        }
		
		if (!isValid) {
			throw new IllegalArgumentException("Either '-d' duration or both start date '-s' and end date '-e' must be provided.");
		} else {
			if (commandLine.hasOption('d')) {
				duration = commandLine.getOptionValue('d');
				startDateTemp = LocalDate.now(DateTimeZone.forID("Australia/Sydney"));
				startDate = startDateTemp.toDate();

				durationTime = startDateTemp.plusDays(Integer.parseInt(this.duration) + 1);
				endDate = durationTime.toDate();

			} else {
				rangeStartDate = commandLine.getOptionValue('s');
				rangeEndDate = commandLine.getOptionValue('e');

				if(rangeStartDate.matches(dateRegexPattern)) {
					startDate = (LocalDate.parse(rangeStartDate)).toDate();
				} else {
					handler.handleException("Invalid date format. Start date '-s' format should follow yyyy-MM-dd. For example, 1995-01-12.");
				}
				
				if(rangeEndDate.matches(dateRegexPattern)) {
					endDate = (LocalDate.parse(rangeEndDate)).toDate();
				} else {
					handler.handleException("Invalid date format. End date '-e' format should follow yyyy-MM-dd. For example, 1995-01-12.");
				}
				
				if(startDate.after(endDate)) {
					handler.handleException("Start date should be before end date. Check the dates");
				}
				duration = null;
			}
		}

		try {
			dSpaceObjectUtils = new DSpace().getServiceManager().getServiceByName(DSpaceObjectUtilsImpl.class.getName(),
					DSpaceObjectUtilsImpl.class);
			getEmbargoDatesOfBitstream(context, uuid, startDate, endDate);

		} catch (Exception e) {
			handler.handleException(e);
			context.abort();
		}

		if(matrix.size() > 1) {
			handler.writeFilestream(context, filename, exportAsCSV(matrix), EXPORT_CSV);
		}

		context.restoreAuthSystemState();
		context.complete();

	}

	private boolean validate(Context context2) throws ParseException {
		// TODO Auto-generated method stub
		boolean durBool = commandLine.hasOption('d');
		boolean sdBool = commandLine.hasOption('s');
		boolean edBool = commandLine.hasOption('e');

		if (durBool) {
			if (!sdBool && !edBool) {
				isValid = true;
			}
		} else {
			if (sdBool && edBool) {
				isValid = true;
			}
		}
		return isValid;
	}

	private void getEmbargoDatesOfBitstream(Context context, String uuid, Date startDate, Date endDate)
			throws SQLException, IOException {

		DSpaceObject dso = dSpaceObjectUtils.findDSpaceObject(context, UUID.fromString(uuid));
		perform(context, dso, startDate, endDate);

	}

	public void perform(Context context, DSpaceObject dso, Date startDate, Date endDate) throws IOException {
		try {
			// perform task on this current object
			performObject(dso, startDate, endDate);

			// next, we'll try to distribute to all child objects, based on container type
			int type = dso.getType();
			if (Constants.COLLECTION == type) {
				Iterator<Item> iter = itemService.findByCollection(context, (Collection) dso);
				while (iter.hasNext()) {
					Item item = iter.next();
					performObject(item, startDate, endDate);
				}
			} else if (Constants.COMMUNITY == type) {
				Community comm = (Community) dso;
				for (Community subcomm : comm.getSubcommunities()) {
					perform(context, subcomm, startDate, endDate);
				}
				for (Collection coll : comm.getCollections()) {
					perform(context, coll, startDate, endDate);
				}
			} else if (Constants.SITE == type) {
				List<Community> topComm = communityService.findAllTop(context);
				for (Community comm : topComm) {
					perform(context, comm, startDate, endDate);
				}
			}

		} catch (SQLException sqlE) {
			logExceptionMessage(sqlE.getMessage());
		} catch (Exception e) {
			logExceptionMessage(e.getMessage());
		}
	}

	protected void performObject(DSpaceObject dso, Date startDate, Date endDate)
			throws SQLException, IOException, java.text.ParseException {
		// By default this method only performs tasks on Items
		// (You should override this method if you want to perform task on all objects)
		if (dso.getType() == Constants.ITEM) {
			performItem((Item) dso, startDate, endDate);
		}
	}

	protected void performItem(Item item, Date startDate, Date endDate)
			throws SQLException, IOException, java.text.ParseException {

		for (Bundle bundle : item.getBundles()) {
			if ("ORIGINAL".equals(bundle.getName())) {
				for (Bitstream bs : bundle.getBitstreams()) {
					for (ResourcePolicy rp : bs.getResourcePolicies()) {
						findAndCheckStartDates(rp, item, startDate, endDate);
					}
				}
			}
		}
	}

	protected void findAndCheckStartDates(ResourcePolicy rp, Item item, Date startDate, Date endDate)
			throws IOException, java.text.ParseException {

		if (rp.getStartDate() != null) {
			// Date calculator
			if ((rp.getStartDate().after(startDate) || rp.getStartDate().equals(startDate))
					&& (rp.getStartDate().before(endDate) || rp.getStartDate().equals(endDate))) {
				logMessage(null, rp.getStartDate().toString(), item);
				matrix.add(new ArrayList<String>(Arrays.asList(item.getID().toString(), item.getHandle().toString(),
						"\"" + item.getName() + "\"", rp.getStartDate().toString())));
			}
		}
	}

	/**
	 * Set the eperson in the context
	 *
	 * @param context the context
	 * @throws SQLException if database error
	 */
	protected void setEPerson(Context context) throws SQLException {
		EPerson myEPerson = ePersonService.find(context, this.getEpersonIdentifier());

		if (myEPerson == null) {
			handler.logError("EPerson cannot be found: " + this.getEpersonIdentifier());
			throw new UnsupportedOperationException("EPerson cannot be found: " + this.getEpersonIdentifier());
		}
		context.setCurrentUser(myEPerson);
	}

	private void logMessage(String msg, String endDate, Item item) throws IOException {
		// TODO Auto-generated method stub
		try {
			StringBuilder message = new StringBuilder();
			message.append("\n").append("The end date for the item ").append(item.getName()).append(" (")
					.append(item.getHandle()).append(")").append(" is ").append(endDate);
			handler.logInfo(message.toString());
		} catch (Exception e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public InputStream exportAsCSV(ArrayList<ArrayList<String>> exportMatrix) throws IOException {
		StringBuilder array = new StringBuilder();

		for (ArrayList<String> arrayList : exportMatrix) {
			for (int i = 0; i < arrayList.size(); i++) {
				String innerArray = arrayList.get(i);
				array.append(innerArray);
				if (i < arrayList.size() - 1) {
					array.append(",");

				}
			}
			array.append("\n");
		}

		byte[] bytes = array.toString().getBytes(StandardCharsets.UTF_8);

		InputStream inputStream = new ByteArrayInputStream(bytes);
		inputStream.close();
		return inputStream;
	}

	private void logExceptionMessage(String message) {
		// TODO Auto-generated method stub
		handler.logInfo(message.toString());
	}

	@Override
	public EmbargoDateCheckerScriptConfiguration getScriptConfiguration() {
		return new DSpace().getServiceManager().getServiceByName("embargo-date-checker",
				EmbargoDateCheckerScriptConfiguration.class);
	}

}
