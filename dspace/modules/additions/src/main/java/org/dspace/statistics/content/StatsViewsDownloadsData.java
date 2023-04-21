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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;

/**
 * Query factory associated with a DSpaceObject. Encapsulates the raw data,
 * independent of rendering.
 * <p>
 * To use:
 * <ol>
 * <li>Instantiate, passing a reference to the interesting DSO.</li>
 * <li>Add a {@link DatasetDSpaceObjectGenerator} for the appropriate object
 * type.</li>
 * <li>Add other generators as required to get the statistic you want.</li>
 * <li>Add {@link org.dspace.statistics.content.filter filters} as
 * required.</li>
 * <li>{@link #createDataset(Context, int)} will run the query and return a
 * result matrix. Subsequent calls skip the query and return the same
 * matrix.</li>
 * </ol>
 *
 * @author kevinvandevelde at atmire.com Date: 23-feb-2009 Time: 12:25:20
 */
public class StatsViewsDownloadsData extends StatisticsData {
	/**
	 * Current DSpaceObject for which to generate the statistics.
	 */
	protected DSpaceObject currentDso;
	protected SolrClient solr;
	protected final HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
	protected final SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();
	protected final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
	protected final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
	protected final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
	protected final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
	protected final ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
			.getConfigurationService();

	/**
	 * Construct a completely uninitialized query.
	 */
	public StatsViewsDownloadsData() {
		super();
	}

	/**
	 * Construct an empty query concerning a given DSpaceObject.
	 *
	 * @param dso the target DSpace object
	 */
	public StatsViewsDownloadsData(DSpaceObject dso) {
		super();
		this.currentDso = dso;
	}

	/**
	 * Construct an unconfigured query around a given DSO and Dataset.
	 *
	 * @param currentDso the target DSpace object
	 * @param dataset    the target dataset
	 */
	public StatsViewsDownloadsData(DSpaceObject currentDso, Dataset dataset) {
		super(dataset);
		this.currentDso = currentDso;
	}

	/**
	 * Construct an unconfigured query around a given Dataset.
	 *
	 * @param dataset the target dataset
	 */
	public StatsViewsDownloadsData(Dataset dataset) {
		super(dataset);
	}

	@Override
	public Dataset createDataset(Context context, int facetMinCount)
			throws SQLException, SolrServerException, ParseException, IOException {
		// Check if we already have one.
		// If we do then give it back.
		if (getDataset() != null) {
			return getDataset();
		}

		///////////////////////////
		// 1. DETERMINE OUR AXIS //
		///////////////////////////
		ArrayList<DatasetQuery> datasetQueries = new ArrayList<>();
		for (int i = 0; i < getDatasetGenerators().size(); i++) {
			DatasetGenerator dataSet = getDatasetGenerators().get(i);
			dataSet.setIncludeTotal(true);
			processAxis(context, dataSet, datasetQueries);
		}

		/////////////////////////
		// 2. DETERMINE VALUES //
		/////////////////////////
		// Check if we need our total

		// Determine our filterQuery
		String filterQuery = "";
		for (int i = 0; i < getFilters().size(); i++) {
			StatisticsFilter filter = getFilters().get(i);

			filterQuery += "(" + filter.toQuery() + ")";
			if (i != (getFilters().size() - 1)) {
				filterQuery += " AND ";
			}
		}
		if (StringUtils.isNotBlank(filterQuery)) {
			filterQuery += " AND ";
		}
		// Only use the view type and make sure old data (where no view type is present)
		// is also supported
		// Solr doesn't explicitly apply boolean logic, so this query cannot be
		// simplified to an OR query
		filterQuery += "-(statistics_type:[* TO *] AND -statistics_type:"
				+ SolrLoggerServiceImpl.StatisticsType.VIEW.text() + ")";

		Dataset dataset = null;

		DatasetQuery firsDataset = datasetQueries.get(0);

		// Do the first query

		ObjectCount topCounts1 = solrLoggerService.queryTotal(firsDataset.getQueries().get(0).getQuery(), filterQuery,
				facetMinCount);
		long totalCount = topCounts1.getCount();

		// Make sure we have a dataSet
		dataset = new Dataset(1, 1);

		if (firsDataset.getQueries().get(0).getQuery().equals("type: 0")) {
			dataset.setColLabel(0, "totalDownloads");
			dataset.setColLabelAttr(0, "totalDownloads", null);
		} else {
			dataset.setColLabel(0, "totalViews");
			dataset.setColLabelAttr(0, "totalViews", null);
		}

		dataset.addValueToMatrix(0, 0, totalCount);

		if (dataset != null) {
			dataset.setRowTitle("Dataset 1");
			dataset.setColTitle("Dataset 2");
		}
		return dataset;
	}

	protected void processAxis(Context context, DatasetGenerator datasetGenerator, List<DatasetQuery> queries)
			throws SQLException {
		if (datasetGenerator instanceof DatasetDSpaceObjectGenerator) {
			DatasetDSpaceObjectGenerator dspaceObjAxis = (DatasetDSpaceObjectGenerator) datasetGenerator;
			// Get the types involved
			List<DSORepresentation> dsoRepresentations = dspaceObjAxis.getDsoRepresentations();
			for (int i = 0; i < dsoRepresentations.size(); i++) {
				DatasetQuery datasetQuery = new DatasetQuery();
				Integer dsoType = dsoRepresentations.get(i).getType();
				boolean separate = dsoRepresentations.get(i).getSeparate();
				Integer dsoLength = dsoRepresentations.get(i).getNameLength();
				// Check if our type is our current object
				if (currentDso != null && dsoType == currentDso.getType()) {
					Query query = new Query();
					query.setDso(currentDso, currentDso.getType(), dsoLength);
					datasetQuery.addQuery(query);
				} else {
					// TODO: only do this for bitstreams from an item
					Query query = new Query();
					if (currentDso != null && separate && dsoType == Constants.BITSTREAM) {
						// CURRENTLY THIS IS ONLY POSSIBLE FOR AN ITEM ! ! ! ! ! ! !
						// We need to get the separate bitstreams from our item and make a query for
						// each of them
						Item item = (Item) currentDso;
						for (int j = 0; j < item.getBundles().size(); j++) {
							Bundle bundle = item.getBundles().get(j);
							for (int k = 0; k < bundle.getBitstreams().size(); k++) {
								Bitstream bitstream = bundle.getBitstreams().get(k);
								if (!bitstream.getFormat(context).isInternal()) {
									// Add a separate query for each bitstream
									query.setDso(bitstream, bitstream.getType(), dsoLength);
								}
							}
						}
					} else {
						// We have something else than our current object.
						// So we need some kind of children from it, so put this in our query
						query.setOwningDso(currentDso);
						query.setDsoLength(dsoLength);

						String title = "";
						switch (dsoType) {
						case Constants.BITSTREAM:
							title = "Files";
							break;
						case Constants.ITEM:
							title = "Items";
							break;
						case Constants.COLLECTION:
							title = "Collections";
							break;
						case Constants.COMMUNITY:
							title = "Communities";
							break;
						default:
							break;
						}
						datasetQuery.setName(title);
						// Put the type in so we only get the children of the type specified
						query.setDsoType(dsoType);
					}
					datasetQuery.addQuery(query);
				}
				datasetQuery.setFacetField("id");
				datasetQuery.setMax(dsoRepresentations.get(i).getMax());

				queries.add(datasetQuery);

			}
		} else if (datasetGenerator instanceof DatasetTypeGenerator) {
			DatasetTypeGenerator typeAxis = (DatasetTypeGenerator) datasetGenerator;
			DatasetQuery datasetQuery = new DatasetQuery();

			// First make sure our query is in order
			Query query = new Query();
			if (currentDso != null) {
				query.setDso(currentDso, currentDso.getType());
			}
			datasetQuery.addQuery(query);

			// Then add the rest
			datasetQuery.setMax(typeAxis.getMax());
			datasetQuery.setFacetField(typeAxis.getType());
			datasetQuery.setName(typeAxis.getType());

			queries.add(datasetQuery);
		}
	}

	public static class DatasetQuery {
		private String name;
		private int max;
		private String facetField;
		private final List<Query> queries;

		public DatasetQuery() {
			queries = new ArrayList<>();
		}

		public int getMax() {
			return max;
		}

		public void setMax(int max) {
			this.max = max;
		}

		public void addQuery(Query q) {
			queries.add(q);
		}

		public List<Query> getQueries() {
			return queries;
		}

		public String getFacetField() {
			return facetField;
		}

		public void setFacetField(String facetField) {
			this.facetField = facetField;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public class Query {
		private int dsoType;
		private DSpaceObject dso;
		private int dsoLength;
		private DSpaceObject owningDso;

		public Query() {
			dso = null;
			dsoType = -1;
			dso = null;
			owningDso = null;
		}

		public void setOwningDso(DSpaceObject owningDso) {
			this.owningDso = owningDso;
		}

		public void setDso(DSpaceObject dso, int dsoType) {
			this.dso = dso;
			this.dsoType = dsoType;
		}

		public void setDso(DSpaceObject dso, int dsoType, int length) {
			this.dsoType = dsoType;
			this.dso = dso;
		}

		public void setDsoType(int dsoType) {
			this.dsoType = dsoType;
		}

		public int getDsoLength() {
			return dsoLength;
		}

		public void setDsoLength(int dsoLength) {
			this.dsoLength = dsoLength;
		}

		public int getDsoType() {
			return dsoType;
		}

		public DSpaceObject getDso() {
			return dso;
		}

		public String getQuery() {
			// Time to construct our query
			String query = "";
			// Check (& add if needed) the dsoType
			if (dsoType != -1) {
				query += "type: " + dsoType;
			}

			// Check (& add if needed) the dsoId
			if (dso != null) {
				query += (query.equals("") ? "" : " AND ");

				// DS-3602: For clarity, adding "id:" to the right hand side of the search
				// In the solr schema, "id" has been declared as the defaultSearchField so the
				// field name is optional
				if (dso instanceof DSpaceObjectLegacySupport) {
					query += " (id:" + dso.getID() + " OR id:" + ((DSpaceObjectLegacySupport) dso).getLegacyId() + ")";
				} else {
					query += "id:" + dso.getID();
				}
			}

			if (owningDso != null && currentDso != null) {
				query += (query.equals("") ? "" : " AND ");

				String owningStr = "";
				switch (currentDso.getType()) {
				case Constants.ITEM:
					owningStr = "owningItem";
					break;
				case Constants.COLLECTION:
					owningStr = "owningColl";
					break;
				case Constants.COMMUNITY:
					owningStr = "owningComm";
					break;
				default:
					break;
				}
				if (currentDso instanceof DSpaceObjectLegacySupport) {
					owningStr = "(" + owningStr + ":" + currentDso.getID() + " OR " + owningStr + ":"
							+ ((DSpaceObjectLegacySupport) currentDso).getLegacyId() + ")";
				} else {
					owningStr += ":" + currentDso.getID();
				}

				query += owningStr;
			}

			if (query.equals("")) {
				query = "*:*";
			}

			return query;
		}
	}

}
