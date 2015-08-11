/**
 * 
 */
package org.dspace.policy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;

/**
 * @author Rahul Khanna
 *
 */
public class CustomPolicyApplier {

	private static final String ANONYMOUS_GROUPNAME = "Anonymous";
	
	private static boolean isDryRun = false;
	private static Context c;
	private static Connection dbConn;
	
	public static void main(String[] args) {
		try {
			initContext();
			for (String arg : args) {
				if (arg.equals("--dry-run")) {
					isDryRun = true;
					System.out.println("Running in dry run mode");
				} else {
					processHandle(arg);
				}
			}
			System.out.println("Finished");
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (AuthorizeException e) {
			e.printStackTrace();
		} finally {
			// cleanly close context
			if (c != null) {
				try {
					c.complete();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// close db connection
			if (dbConn != null) {
				try {
					if (!dbConn.isClosed()) {
						dbConn.close();
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static void processHandle(String handle) throws SQLException, AuthorizeException {
		DSpaceObject resource = HandleManager.resolveToObject(c, handle);
		processResource(resource);
	}
	
	private static void processResource(DSpaceObject resource) throws SQLException, AuthorizeException {
		if (resource.getType() == Constants.COMMUNITY) {
			processCommunity((Community) resource);
		} else if (resource.getType() == Constants.COLLECTION) {
			processCollection((Collection) resource);
		} else if (resource.getType() == Constants.ITEM) {
			processItem((Item) resource);
		}
	}

	private static void processCommunity(Community community) throws SQLException, AuthorizeException {
		for (Collection coll : community.getAllCollections()) {
			processCollection(coll);
		}
	}
	
	private static void processCollection(Collection coll) throws SQLException, AuthorizeException {
		ItemIterator iterator = coll.getItems();
		while (iterator.hasNext()) {
			processItem(iterator.next());
		}
	}
	
	private static void processItem(Item item) throws SQLException, AuthorizeException {

		// item policies
		System.out.format("Checking policies for Item '%s' (ID:%d) (Handle:%s)", item.getName(), item.getID(),
				item.getHandle());
		System.out.println();
		// List<ResourcePolicy> itemPolicies = AuthorizeManager.getPolicies(c,
		// item);
		// for (ResourcePolicy policy : itemPolicies) {
		// printPolicy(policy);
		// }

		Group anonymousGroup = Group.findByName(c, ANONYMOUS_GROUPNAME);

		for (Bundle bundle : item.getBundles()) {
			if (bundle.getName().equals("THUMBNAIL") || bundle.getName().equals("BRANDED_PREVIEW")) {
				// System.out.format("Policies for Bundle '%s' (ID:%d)",
				// bundle.getName(), bundle.getID());
				// System.out.println();

				List<ResourcePolicy> bundlePolicies = AuthorizeManager.getPolicies(c, bundle);
				for (ResourcePolicy bundlePolicy : bundlePolicies) {
					// printPolicy(bundlePolicy);
					if (!bundlePolicy.getGroup().equals(anonymousGroup)) {
						System.out.format(
								"Changing policy id=%d for Bundle '%s'(ID:%d) from Group %s(id:%d) to %s(id:%d)...",
								bundlePolicy.getID(), bundle.getName(), bundle.getID(),
								bundlePolicy.getGroup().getName(), bundlePolicy.getGroupID(), anonymousGroup.getName(),
								anonymousGroup.getID());
						if (!isDryRun) {
							bundlePolicy.setGroup(anonymousGroup);
							bundlePolicy.update();
						}
						System.out.println(" done");
					}
				}

				for (Bitstream bitstream : bundle.getBitstreams()) {
					// System.out.format("Policies for Bitstream '%s' (ID:%d)",
					// bitstream.getName(), bitstream.getID());
					// System.out.println();

					List<ResourcePolicy> bitstreamPolicies = AuthorizeManager.getPolicies(c, bitstream);
					for (ResourcePolicy bsPolicy : bitstreamPolicies) {
						// printPolicy(bsPolicy);
						if (!bsPolicy.getGroup().equals(anonymousGroup)) {
							System.out.format(
									"Changing policy id=%d for Bitstream '%s'(ID:%d) of Bundle '%s'(ID:%d) from Group %s(id:%d) to %s(id:%d)...",
									bsPolicy.getID(), bitstream.getName(), bitstream.getID(), bundle.getName(),
									bundle.getID(), bsPolicy.getGroup().getName(), bsPolicy.getGroupID(),
									anonymousGroup.getName(), anonymousGroup.getID());
							if (!isDryRun) {
								bsPolicy.setGroup(anonymousGroup);
								bsPolicy.update();
							}
							System.out.println(" done");
						}
					}
				}
			}
		}
	}
	
	private static void printPolicy(ResourcePolicy policy) {
		System.out.format("\t%d\t%s\t%d\t%d", policy.getID(), policy.getActionText(), policy.getEPersonID(), policy.getGroupID());
		System.out.println();
	}
	
	private static void initContext() throws SQLException {
		c = new Context();
		// c.turnOffAuthorisationSystem();
		dbConn = c.getDBConnection();
	}

}
