package org.dspace.identifier.doi;

import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.identifier.ANUDOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.utils.DSpace;
import org.dspace.workflow.factory.WorkflowServiceFactory;

public class ANUDOIConsumer implements Consumer {
	/**
	 * log4j logger
	 */
	private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ANUDOIConsumer.class);

	@Override
	public void initialize() throws Exception {
		// nothing to do
		// we can ask spring to give as a properly setuped instance of
		// DOIIdentifierProvider. Doing so we don't have to configure it and
		// can load it in consume method as this is not very expensive.

	}

	// as we use asynchronous metadata update, our updates are not very expensive.
	// so we can do everything in the consume method.
	@Override
	public void consume(Context ctx, Event event) throws Exception {
		if (event.getSubjectType() != Constants.ITEM) {
			log.warn("ANUDOIConsumer should not have been given this kind of "
						 + "subject in an event, skipping: " + event.toString());
			return;
		}
		if (Event.MODIFY == event.getEventType()) {
			modify(ctx, event);
			return;
		}
		if (Event.MODIFY_METADATA != event.getEventType()) {
			log.warn("ANUDOIConsumer should not have been given this kind of "
						 + "event type, skipping: " + event.toString());
			return;
		}

		DSpaceObject dso = event.getSubject(ctx);
		//FIXME
		if (!(dso instanceof Item)) {
			log.debug("ANUDOIConsumer got an event whose subject was not an item, "
						  + "skipping: " + event.toString());
			return;
		}
		Item item = (Item) dso;
		if (item.isWithdrawn()) {
			return;
		}

		if (ContentServiceFactory.getInstance().getWorkspaceItemService().findByItem(ctx, item) != null
			|| WorkflowServiceFactory.getInstance().getWorkflowItemService().findByItem(ctx, item) != null) {
			// ignore workflow and workspace items, DOI will be minted when item is installed
			return;
		}

		ANUDOIIdentifierProvider provider = new DSpace().getSingletonService(
			ANUDOIIdentifierProvider.class);

		String doi = null;
		try {
			doi = provider.lookup(ctx, dso);
			provider.updateMetadata(ctx, dso, doi);
		} catch (IdentifierNotFoundException ex) {
			// nothing to do here, next if clause will stop us from processing
			// items without dois.
//			doi = provider.register(ctx, dso, false);
			//TODO GT see if this needs to do the filter?
			doi = provider.register(ctx, dso);
		} catch (IllegalArgumentException ex) {
			// should not happen, as we got the DOI from the DOIProvider
			log.warn("DOIConsumer caught an IdentifierException.", ex);
		} catch (IdentifierException ex) {
			log.warn("DOIConsumer cannot update metadata for Item with ID "
						 + item.getID() + " and DOI " + doi + ".", ex);
		}
	}
	
	private void modify(Context ctx, Event event) throws Exception {
		ANUDOIIdentifierProvider provider = new DSpace().getSingletonService(ANUDOIIdentifierProvider.class);
		DSpaceObject dso = event.getSubject(ctx);
		if ("WITHDRAW".equals(event.getDetail())) {
			provider.delete(ctx, dso);
			ctx.commit();
		}
		else if ("REINSTATE".equals(event.getDetail())) {
			provider.activate(ctx, dso);
			ctx.commit();
		}
	}

	@Override
	public void end(Context ctx) throws Exception {


	}

	@Override
	public void finish(Context ctx) throws Exception {
		// nothing to do
	}

}
