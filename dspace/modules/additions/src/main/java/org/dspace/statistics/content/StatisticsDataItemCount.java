/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.content;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;

import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchUtils;
import org.dspace.statistics.Dataset;
import org.springframework.beans.factory.annotation.Autowired;

public class StatisticsDataItemCount extends StatisticsData {
	/**
	 * Current DSpaceObject for which to generate the statistics.
	 */
	private DSpaceObject currentDso;

	protected SolrClient solr;
	protected Date asAtDate;
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	private static final Logger log = LogManager.getLogger();

	// @Autowired
	protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

	@Autowired
	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

	/**
	 * Construct a completely uninitialized query.
	 */
	public StatisticsDataItemCount() {
		super();
	}

	/**
	 * Construct an empty query concerning a given DSpaceObject.
	 *
	 * @param dso the target DSpace object
	 */
	public StatisticsDataItemCount(DSpaceObject dso) {
		super();
		this.currentDso = dso;
	}

	/**
	 * Construct an unconfigured query around a given DSO and Dataset.
	 *
	 * @param currentDso the target DSpace object
	 * @param dataset    the target dataset
	 */
	public StatisticsDataItemCount(DSpaceObject dso, Date endDate) {
		this.currentDso = dso;
		this.asAtDate = endDate;
	}

	/**
	 * Construct an unconfigured query around a given Dataset.
	 *
	 * @param dataset the target dataset
	 */
	public StatisticsDataItemCount(Dataset dataset) {
		super(dataset);
	}

	@Override
	public Dataset createDataset(Context context, int facetMinCount)
			throws SQLException, SolrServerException, ParseException, IOException {

		Dataset dataset = new Dataset(1, 1);

		DiscoverQuery discoveryQuery = new DiscoverQuery();

		if (asAtDate != null) {
			SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT, context.getCurrentLocale());
			String endDate = formatter.format(asAtDate);
			discoveryQuery.setQuery("search.resourcetype:Item AND dc.date.accessioned_dt:[* TO " + endDate + "]");
		} else {
			discoveryQuery.setQuery("search.resourcetype:Item");
		}
		discoveryQuery.setFacetMinCount(1);

		try {
			DiscoverResult results = null;
			if (currentDso.getType() == 4) {
				discoveryQuery
						.setQuery(discoveryQuery.getQuery() + " AND location.comm:" + currentDso.getID().toString());
			}
			if (currentDso.getType() == 3) {
				discoveryQuery
						.setQuery(discoveryQuery.getQuery() + " AND location.coll:" + currentDso.getID().toString());
			}

			results = SearchUtils.getSearchService().search(context, discoveryQuery);
			dataset.setColLabel(0, currentDso.getName());
			dataset.setRowLabel(0, currentDso.getName());
			dataset.addValueToMatrix(0, 0, results.getTotalSearchResults());

		} catch (Exception e) {
			log.error("Error in creating dataset for total items count", e.getLocalizedMessage());
		}

		return dataset;
	}
}