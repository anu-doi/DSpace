package org.dspace.app.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

public class ArchiveItems {
    private static Logger log = Logger.getLogger(ArchiveItems.class);
    
	public void ArchiveItems() {
		
	}
	
	private void load(String filename, String email) {
		try {
			Context context = new Context();
			EPerson eperson = EPerson.findByEmail(context, email);
			if (eperson == null) {
				System.out.print("Unable to find provided person");
				return;
			}
			log.info("User found: " + eperson.getID() + ", " + eperson.getFirstName() + " " + eperson.getLastName());
			context.setCurrentUser(eperson);
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			String line = null;
			try {
				while (( line = br.readLine()) != null) {
					int id = Integer.parseInt(line.trim());
					log.info("Attempting to archive item " + id);
					
					DSpaceObject object = DSpaceObject.find(context, Constants.ITEM, id);
					Item item = (Item) object;
					WorkflowItem wfi = WorkflowItem.findByItem(context, item);
					if (wfi != null) {
						int i = 0;
						boolean archived = false;
//						while (!archived && i < 5) {
							log.info("Workflow Item state: " + wfi.getState());
							WorkflowManager.claim(context, wfi, eperson);
							log.info("Workflow Item state post claim: " + wfi.getState());
//							archived = WorkflowManager.advance(context, wfi, eperson, true, true);
				            WorkflowManager.advance(context, wfi, eperson);
							log.info("Workflow Item state post advance: " + wfi.getState());
							
				            String handle = HandleManager.findHandle(context, item);

				            if (handle != null) {
				            	System.out.println("Handle created for item " + item.getID() + " was " + handle);
				            }
				            else {
				            	System.out.println("No handle created for item " + item.getID());
				            }
				            context.commit();
				            
							i++;
//						}
					}
					//tODO notify of not run for archive
				}
			}
			finally {
				br.close();
				context.complete();
			}
		}
		catch (IOException e) {
			//TODO better error handling
			e.printStackTrace();
		}
		catch (AuthorizeException e) {
			e.printStackTrace();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		CommandLineParser parser = new PosixParser();
		
		Options options = new Options();
		
		options.addOption("h","help",false,"Help");
		options.addOption("f","file", true, "The name of the file with the item ids to archive");
		options.addOption("e","eperson",true,"email of eperson doing importing");
		
		CommandLine line = parser.parse(options, args);
		
		if (line.hasOption('h')) {
			printHelp(options, 0);
		}
		String filename = null;
		String email = null;
		if (line.hasOption('f')) {
			filename = line.getOptionValue('f');
		}
		if (line.hasOption('e')) {
			email = line.getOptionValue('e');
		}
		if (filename != null && email != null) {
			ArchiveItems archiveItems = new ArchiveItems();
			archiveItems.load(filename, email);
		}
		else {
			printHelp(options, 0);
		}
		
		//options.addOption("")
	}
	
    private static void printHelp(Options options, int exitCode)
    {
        // print the help message
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("StatisticsImporter\n", options);
        System.exit(exitCode);
    }
}
