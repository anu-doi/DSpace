/**
 * 
 */
package au.edu.anu.dspace.app.registerbitstream;

import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * @author Rahul Khanna
 *
 */
public class RegisterBitstream {
	private static final int DEFAULT_ASSETSTORE = 1;
	private static final String ORIGINAL_BUNDLE = "ORIGINAL";

	public static void main(String... args) {
		// parse command line options
		CommandLine line = null;
		Options options = null;
		try {
			CommandLineParser parser = new PosixParser();
			options = createOptions();
			line = parser.parse(options, args, false);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// display help and exit
		if (line.hasOption('h')) {
			HelpFormatter myhelp = new HelpFormatter();
			myhelp.printHelp(RegisterBitstream.class.getName() + "\n", options);
			System.exit(0);
		}

		// lookup ePerson by email address
		String eperson = null;
		if (line.hasOption('e')) {
			eperson = line.getOptionValue('e');
		}
		if (eperson == null) {
			System.err.println("EPerson not specified");
			System.exit(1);
		}
		Context c = null;
		EPerson myEPerson = null;
		try {
			c = new Context();
			myEPerson = findEPerson(c, eperson);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthorizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// authenticate as that person.
		c.setCurrentUser(myEPerson);

		// lookup item
		Item item = null;
		if (line.hasOption('i')) {
			// item ID provided
			int itemId = parseInt(line.getOptionValue('i'));
			try {
				item = Item.find(c, itemId);
			} catch (SQLException e) {
				e.printStackTrace();
				System.exit(1);
			}
		} else if (line.hasOption('w')) {
			// workspace item ID provided
			int workspaceItemId = parseInt(line.getOptionValue('w'));
			WorkspaceItem wItem;
			try {
				wItem = WorkspaceItem.find(c, workspaceItemId);
				item = wItem.getItem();
			} catch (SQLException e) {
				e.printStackTrace();
				System.exit(1);
			}
		} else {
			System.err.println("Item ID or Workspace Item ID must be provided.");
			System.exit(1);
		}

		// lookup bundle if specified, else work with ORIGINAL bundle
		String bundleName;
		if (line.hasOption('b')) {
			bundleName = line.getOptionValue('b');
		} else {
			bundleName = ORIGINAL_BUNDLE;
		}

		Bundle[] bundles;
		Bundle bundle = null;
		try {
			bundles = item.getBundles(bundleName);
			if (bundles.length == 0) {
				// if bundle not found, create it.
				bundle = item.createBundle(bundleName);
			} else {
				// if bundle found, use it
				bundle = bundles[0];
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (AuthorizeException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// use default asset store number 1, if none provided
		int assetStoreNum = line.hasOption('a') ? parseInt(line.getOptionValue('a')) : DEFAULT_ASSETSTORE;

		// bitstream path relative to asset store
		String bitstreamPath = null;
		if (line.hasOption('p')) {
			bitstreamPath = line.getOptionValue('p');
		} else {
			System.out.println("Bitstream path not provided.");
			System.exit(1);
		}

		// description, if provided
		String description = line.hasOption('d') ? line.getOptionValue('d').trim() : "";

		// register bitstream and commit changes
		try {
			String msg = MessageFormat.format("Adding {0} to {1} [{2}] ... ", bitstreamPath, item.getName(),
					item.getID());
			System.out.print(msg);
			registerBitstream(c, bundle, assetStoreNum, bitstreamPath, description);
			item.update();
			c.commit();
			System.out.println("[DONE]");
		} catch (AuthorizeException e) {
			System.out.println("[ERROR]");
			c.abort();
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.out.println("[ERROR]");
			c.abort();
			e.printStackTrace();
			System.exit(1);
		} catch (SQLException e) {
			System.out.println("[ERROR]");
			c.abort();
			e.printStackTrace();
			System.exit(1);
		}

		System.exit(0);
	}

	private static Options createOptions() {
		Options options = new Options();
		options.addOption("e", "eperson", true, "email of eperson doing importing");
		options.addOption("i", "itemid", true, "id of item (for an approved item)");
		options.addOption("w", "workspaceid", true,
				"workspace item id (for an item in workspace that's not yet submitted");
		options.addOption("b", "bundle", true, "name of bundle to add bitstream to");
		options.addOption("a", "assetstore", true, "Number of assetstore");
		options.addOption("p", "path", true, "path to the bitstream relative to the assetstore location");
		options.addOption("d", "description", true, "description");
		options.addOption("h", "help", false, "help");
		return options;
	}

	private static EPerson findEPerson(Context c, String eperson) throws SQLException, AuthorizeException {
		EPerson myEPerson = null;
		if (eperson.indexOf('@') != -1) {
			// @ sign, must be an email
			myEPerson = EPerson.findByEmail(c, eperson);
		} else {
			myEPerson = EPerson.find(c, parseInt(eperson));
		}
		return myEPerson;
	}

	private static void registerBitstream(Context c, Bundle targetBundle, int assetstore, String bitstreamPath,
			String description) throws AuthorizeException, IOException, SQLException {

		Bitstream bs = targetBundle.registerBitstream(assetstore, bitstreamPath);

		int iLastSlash = bitstreamPath.lastIndexOf('/');
		bs.setName(bitstreamPath.substring(iLastSlash + 1));

		BitstreamFormat bf = FormatIdentifier.guessFormat(c, bs);
		bs.setFormat(bf);
		bs.setDescription(description);

		bs.update();
	}
	
	private static int parseInt(String intAsString) {
		return Integer.parseInt(intAsString, 10);
	}
}
