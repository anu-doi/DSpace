package org.dspace.identifier.doi;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.utils.DSpace;

public class ARDCConsumer implements Consumer {
	
	private static Logger log = Logger.getLogger(ARDCConsumer.class);

	@Override
	public void initialize() throws Exception {
		// Do nothing
	}

	@Override
	public void consume(Context ctx, Event event) throws Exception {
		if (event.getSubjectType() != Constants.ITEM) {
			log.warn("ARDCConsumer should not have been given this kind of subject in an event, skipping: " + event.toString());
		}
		if (Event.MODIFY == event.getEventType()) {
			modify(ctx, event);
			return;
		}
		if (Event.MODIFY_METADATA != event.getEventType()) {
			log.warn("ARDCConsumer should not have been given this kind of event type, skipping: " + event.toString());
		}
		
		DSpaceObject dso = event.getSubject(ctx);
		if (!(dso instanceof Item)) {
			log.warn("ARDCConsumer got an event whose subject was not an item, skipping: " + event.toString());
			return;
		}
		Item item = (Item) dso;
		if (!item.isArchived() || item.isWithdrawn()) {
			log.debug("Item has yet to be archived, or it has been withdrawn so not attempting to add");
			return;
		}
		
		String mintdoi = item.getMetadata("local.mintdoi");
		if (!"mint".equals(mintdoi)) {
			log.debug("Mint doi is either empty or not equal to 'mint' so not proceeding further");
			return;
		}
		
		ARDCIdentifierProvider provider = new DSpace().getSingletonService(ARDCIdentifierProvider.class);
		String doi = null;
		
		try {
			doi = provider.lookup(ctx, dso);
			provider.updateMetadata(ctx, dso, doi);
		}
		catch (IdentifierNotFoundException ex) {
			// Should we be creating a row here if it has the appropriate metadata?
			log.warn("No doi found for the object so setting it up to mint");
			doi = provider.register(ctx, dso);
		}
	}
	
	private void modify(Context ctx, Event event)  throws Exception {
		ARDCIdentifierProvider provider = new DSpace().getSingletonService(ARDCIdentifierProvider.class);
		DSpaceObject dso = event.getSubject(ctx);
		if (!(dso instanceof Item)) {
			log.warn("ARDCConsumer got an event whose subject was not an item, skipping: " + event.toString());
			return;
		}
		if ("WITHDRAW".equals(event.getDetail())) {
			log.info("Withdraw to do");
			provider.delete(ctx, dso);
			ctx.commit();
		}
		else if ("REINSTATE".equals(event.getDetail())) {
			log.info("Resinstate to do");
			provider.activate(ctx, dso);
			ctx.commit();
		}
	}

	@Override
	public void end(Context ctx) throws Exception {
		// Do nothing
	}

	@Override
	public void finish(Context ctx) throws Exception {
		// Do nothing
	}
	
}
