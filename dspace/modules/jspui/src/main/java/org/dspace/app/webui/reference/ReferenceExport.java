package org.dspace.app.webui.reference;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.ItemIterator;
import org.dspace.core.Constants;
import org.dspace.core.Context;

public abstract class ReferenceExport {
	private static Logger log = Logger.getLogger(ReferenceExport.class);
	
	public abstract String getContentType();
	public abstract String getPostfix();
	public abstract void export(Writer writer) throws IOException;
	
	protected ItemIterator getItemIterator(Context context, DSpaceObject object) throws SQLException {
		ItemIterator iterator = null;
		if (object.getType() == Constants.ITEM)
		{
			List<Integer> item = new ArrayList<Integer>();
			item.add(object.getID());
			iterator = new ItemIterator(context, item);
		}
		else if (object.getType() == Constants.COLLECTION)
		{
			Collection collection = (Collection)object;
			iterator = collection.getAllItems();
		}
		else if (object.getType() == Constants.COMMUNITY)
		{
			Community community = (Community) object;
			Collection[] collections = community.getCollections();
			List<Integer> itemIDs = new ArrayList<Integer>();
			for (Collection collection : collections) {
				ItemIterator items = collection.getAllItems();
				while (items.hasNext()) {
					int id = items.next().getID();
					if (!itemIDs.contains(id)) {
						itemIDs.add(id);
					}
				}
			}
			iterator = new ItemIterator(context, itemIDs);
		}
		return iterator;
	}
}
