/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.Constants;
import org.dspace.core.DBConnection;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;
import org.dspace.utils.DSpace;
import org.hibernate.Session;

/**
 * RemoveBitstreamEmbargo is a task that removes the start and end date of
 * bitstream and bundle of THUMBNAIL and BRANDED_PREVIEW for an item, collection
 * or community for it's passed object.
 *
 * @
 */
@Distributive
public class RemoveBitstreamEmbargo extends AbstractCurationTask {
	// map of formats to occurrences
	protected LinkedHashMap<String, String> fmtTable = new LinkedHashMap<String, String>();
	protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance()
			.getBitstreamFormatService();
	// Logger

	private boolean changed = false;

	protected int status = Curator.CURATE_UNSET;

	protected Curator curator = new Curator();

	private DBConnection dbConnection = new DSpace().getServiceManager()
													.getServiceByName(null, DBConnection.class);

	protected Session getHibernateSession() throws SQLException {
		return ((Session) dbConnection.getSession());
	}

	public void evict(Item item) throws SQLException {
		getHibernateSession().evict(item);
	}

	public void flush() throws SQLException {
		getHibernateSession().flush();
	}

	/**
	 * Perform the curation task upon passed DSO
	 * This doesn't include uncache entity as in 
	 * {@link AbstractCurationTask#distribute(DSpaceObject)}
	 * This allows manual eviction of objects 
	 *
	 * @param dso the DSpace object
	 * @throws IOException if IO error
	 */

	@Override
	public int perform(DSpaceObject dso) throws IOException {
		fmtTable.clear();
		String message = new String();

		try {
			// perform task on this current object
			performObject(dso);

			// next, we'll try to distribute to all child objects, based on container type
			int type = dso.getType();
			if (Constants.COLLECTION == type) {
				Iterator<Item> iter = itemService.findByCollection(Curator.curationContext(), (Collection) dso);
				while (iter.hasNext()) {
					Item item = iter.next();
					performObject(item);
					evict(item);
				}
			} else if (Constants.COMMUNITY == type) {
				Community comm = (Community) dso;
				for (Community subcomm : comm.getSubcommunities()) {
					perform(subcomm);
				}
				for (Collection coll : comm.getCollections()) {
					perform(coll);
				}
			} else if (Constants.SITE == type) {
				List<Community> topComm = communityService.findAllTop(Curator.curationContext());
				for (Community comm : topComm) {
					perform(comm);
				}
			}
			status = Curator.CURATE_SUCCESS;
		} catch (SQLException sqlE) {
			logMessage(sqlE.getMessage());
			status = Curator.CURATE_ERROR;
		}catch (Exception e) {
			logMessage(e.getMessage());
			status = Curator.CURATE_ERROR;
		}
		message = null;
		logMessage(message);
		return status;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see AbstractCurationTask#performItem(Item)
	 */
	@Override
	protected void performItem(Item item) throws SQLException {
		for (Bundle bundle : item.getBundles()) {
			if ("BRANDED_PREVIEW".equals(bundle.getName()) || "THUMBNAIL".equals(bundle.getName())) {
				for (ResourcePolicy rp : bundle.getResourcePolicies()) {
					checkAndSetDates(rp);
					if (changed) {
						fmtTable.put(rp.getID().toString(), item.getHandle());
						changed = false;
					}
				}
				for (Bitstream bs : bundle.getBitstreams()) {
					for (ResourcePolicy rp : bs.getResourcePolicies()) {
						checkAndSetDates(rp);
						if (changed) {
							fmtTable.put(rp.getID().toString(), item.getHandle());
							changed = false;
						}
					}
				}
			}
		}
		flush();
	}

	protected void checkAndSetDates(ResourcePolicy rp) {
		if (rp.getStartDate() != null) {
			rp.setStartDate(null);
			changed = true;
		}
		if (rp.getEndDate() != null) {
			rp.setEndDate(null);
			changed = true;
		}

	}

	private void logMessage(String msg) throws IOException {
		// TODO Auto-generated method stub
		try {
			if (msg == null) {
				StringBuilder message = new StringBuilder();
				for (String fmt : fmtTable.keySet()) {
					message.append("\n").append("Updated the resource policy ").append(fmt).append(" of the handle ")
					.append(fmtTable.get(fmt)).append("\n");
					report(message.toString());
					setResult(message.toString());
				}
			} else {
				report(msg.toString());
				setResult(msg.toString());
			}
		} catch (Exception e) {
			throw new IOException(e.getMessage(), e);
		}
	}
}
