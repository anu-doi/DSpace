package org.dspace.statistics.content;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.app.util.Util;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
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
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;

public class StatisticsDataDownload extends StatisticsData {

	protected final HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
	protected final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
	protected final ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
			.getConfigurationService();
	protected final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
	protected final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
	protected final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
	protected final SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();

	private DSpaceObject currentDso;

	public StatisticsDataDownload() {
		super();
	}

	public StatisticsDataDownload(DSpaceObject dso) {
		super();
		this.currentDso = dso;
	}

	public StatisticsDataDownload(DSpaceObject dso, String author) {
		super();
		this.currentDso = dso;
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
			processAxis(context, dataSet, datasetQueries);
		}

		// Now lets determine our values.
		// First check if we have a date facet & if so find it.
		DatasetTimeGenerator dateFacet = null;
		if (getDatasetGenerators().get(0) instanceof DatasetTimeGenerator || (1 < getDatasetGenerators().size()
				&& getDatasetGenerators().get(1) instanceof DatasetTimeGenerator)) {
			if (getDatasetGenerators().get(0) instanceof DatasetTimeGenerator) {
				dateFacet = (DatasetTimeGenerator) getDatasetGenerators().get(0);
			} else {
				dateFacet = (DatasetTimeGenerator) getDatasetGenerators().get(1);
			}
		}

		/////////////////////////
		// 2. DETERMINE VALUES //
		/////////////////////////
		boolean showTotal = false;
		// Check if we need our total
		if ((getDatasetGenerators().get(0) != null && getDatasetGenerators().get(0).isIncludeTotal())
				|| (1 < getDatasetGenerators().size() && getDatasetGenerators().get(1) != null
				&& getDatasetGenerators().get(1).isIncludeTotal())) {
			showTotal = true;
		}

		if (dateFacet != null && dateFacet.getActualStartDate() != null && dateFacet.getActualEndDate() != null) {
			StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
			dateFilter.setStartDate(dateFacet.getActualStartDate());
			dateFilter.setEndDate(dateFacet.getActualEndDate());
			dateFilter.setTypeStr(dateFacet.getDateType());
			addFilters(dateFilter);
		} else if (dateFacet != null && dateFacet.getStartDate() != null && dateFacet.getEndDate() != null) {
			StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
			dateFilter.setStartStr(dateFacet.getStartDate());
			dateFilter.setEndStr(dateFacet.getEndDate());
			dateFilter.setTypeStr(dateFacet.getDateType());
			addFilters(dateFilter);
		}

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

		//	        System.out.println("FILTERQUERY: " + filterQuery);

		// We determine our values on the queries resolved above
		Dataset dataset = null;

		// Run over our queries.
		// First how many queries do we have ?
		if (dateFacet != null) {
			// So do all the queries and THEN do the date facet
			for (int i = 0; i < datasetQueries.size(); i++) {
				DatasetQuery dataSetQuery = datasetQueries.get(i);
				if (dataSetQuery.getQueries().size() != 1) {
					// TODO: do this
				} else {
					String query = dataSetQuery.getQueries().get(0).getQuery();
					if (dataSetQuery.getMax() == -1) {
						// We are asking from our current query all the visits faceted by date
						ObjectCount[] results = solrLoggerService.queryFacetDate(query, filterQuery,
								dataSetQuery.getMax(), dateFacet.getDateType(), dateFacet.getStartDate(),
								dateFacet.getEndDate(), showTotal, context, facetMinCount);
						dataset = new Dataset(3, results.length);
						// Now that we have our results put em in a matrix
						for (int j = 0; j < results.length; j++) {
							ObjectCount count = results[j];
							String dsoId = count.getValue();
							Bitstream bit = bitstreamService.findByIdOrLegacyId(context, dsoId);
							if (bit != null) {
								Item owningItem = null;
								List<Bundle> bunds = bit.getBundles();
								if (0 < bunds.size() && 0 < bunds.get(0).getItems().size()) {
									owningItem = bunds.get(0).getItems().get(0);
								}
								if (owningItem != null) {
									dataset.setColLabel(j, results[j].getValue());
									dataset.addValueToMatrix(0, j, results[j].getCount());
									dataset.addValueToMatrix(1, j, owningItem.getName());
								}
							}

						}
						// TODO: change this !
						// Now add the column label
						dataset.setRowLabel(0, getResultName(dataSetQuery.getName(), dataSetQuery, context));
						dataset.setRowLabelAttr(0, getAttributes(dataSetQuery.getName(), dataSetQuery, context));
					} else {
						// We need to get the max objects and the next part of the query on them (next
						// part beeing
						// the datasettimequery
						ObjectCount[] maxObjectCounts = solrLoggerService.queryFacetField(query, filterQuery,
								dataSetQuery.getFacetField(), dataSetQuery.getMax(), false, null, facetMinCount);
						for (int j = 0; j < maxObjectCounts.length; j++) {
							ObjectCount firstCount = maxObjectCounts[j];
							String newQuery = dataSetQuery.getFacetField() + ": "
									+ ClientUtils.escapeQueryChars(firstCount.getValue()) + " AND " + query;
							ObjectCount[] maxDateFacetCounts = solrLoggerService.queryFacetDate(newQuery, filterQuery,
									dataSetQuery.getMax(), dateFacet.getDateType(), dateFacet.getStartDate(),
									dateFacet.getEndDate(), showTotal, context, facetMinCount);
							// Make sure we have a dataSet
							if (dataset == null) {
								dataset = new Dataset(maxObjectCounts.length, maxDateFacetCounts.length);
							}

							// TODO: this is a very dirty fix change this ! ! ! ! ! !
							dataset.setRowLabel(j, getResultName(firstCount.getValue(), dataSetQuery, context));
							dataset.setRowLabelAttr(j, getAttributes(firstCount.getValue(), dataSetQuery, context));

							for (int k = 0; k < maxDateFacetCounts.length; k++) {
								ObjectCount objectCount = maxDateFacetCounts[k];
								// No need to add this many times
								if (j == 0) {
									dataset.setColLabel(k, objectCount.getValue());
								}
								dataset.addValueToMatrix(j, k, objectCount.getCount());

								String dsoId = objectCount.getValue();
								Bitstream bit = bitstreamService.findByIdOrLegacyId(context, dsoId);
								if (bit != null) {
									Item owningItem = null;
									List<Bundle> bunds = bit.getBundles();
									if (0 < bunds.size() && 0 < bunds.get(0).getItems().size()) {
										owningItem = bunds.get(0).getItems().get(0);
									}
									if (owningItem != null) {
										dataset.addValueToMatrix(1, k, owningItem.getName());
									}
								}

							}
						}
						if (dataset != null && !(getDatasetGenerators().get(0) instanceof DatasetTimeGenerator)) {
							dataset.flipRowCols();
						}
					}
				}
			}
		} else {
			DatasetQuery firsDataset = datasetQueries.get(0);
			// Do the first query
			ObjectCount[] topCounts1 = queryFacetField(firsDataset, firsDataset.getQueries().get(0).getQuery(),
					filterQuery, facetMinCount);
			// Check if we have more queries that need to be done
			if (datasetQueries.size() == 2) {
				DatasetQuery secondDataSet = datasetQueries.get(1);
				// Now do the second one
				ObjectCount[] topCounts2 = queryFacetField(secondDataSet, secondDataSet.getQueries().get(0).getQuery(),
						filterQuery, facetMinCount);
				// Now that have results for both of them lets do x.y queries
				List<String> facetQueries = new ArrayList<>();
				for (ObjectCount count2 : topCounts2) {
					String facetQuery = secondDataSet.getFacetField() + ":"
							+ ClientUtils.escapeQueryChars(count2.getValue());
					// Check if we also have a type present (if so this should be put into the
					// query)
					if ("id".equals(secondDataSet.getFacetField())
							&& secondDataSet.getQueries().get(0).getDsoType() != -1) {
						facetQuery += " AND type:" + secondDataSet.getQueries().get(0).getDsoType();
					}

					facetQueries.add(facetQuery);
				}
				for (int i = 0; i < topCounts1.length; i++) {
					ObjectCount count1 = topCounts1[i];

					// Make sure we have a dataSet
					if (dataset == null) {
						dataset = new Dataset(topCounts2.length, topCounts1.length);
					}
					dataset.setColLabel(i, getResultName(count1.getValue(), firsDataset, context));
					dataset.setColLabelAttr(i, getAttributes(count1.getValue(), firsDataset, context));

					String query = firsDataset.getFacetField() + ":" + ClientUtils.escapeQueryChars(count1.getValue());
					// Check if we also have a type present (if so this should be put into the
					// query)
					if ("id".equals(firsDataset.getFacetField())
							&& firsDataset.getQueries().get(0).getDsoType() != -1) {
						query += " AND type:" + firsDataset.getQueries().get(0).getDsoType();
					}

					Map<String, Integer> facetResult = solrLoggerService.queryFacetQuery(query, filterQuery,
							facetQueries, facetMinCount);

					// TODO: the show total
					// No need to add this many times
					// TODO: dit vervangen door te displayen value
					for (int j = 0; j < topCounts2.length; j++) {
						ObjectCount count2 = topCounts2[j];
						String dsoId = count2.getValue();
						if (i == 0) {
							dataset.setRowLabel(j, getResultName(count2.getValue(), secondDataSet, context));
							dataset.setRowLabelAttr(j, getAttributes(count2.getValue(), secondDataSet, context));

						}
						// Get our value the value is the same as the query
						String facetQuery = secondDataSet.getFacetField() + ":"
								+ ClientUtils.escapeQueryChars(count2.getValue());
						// Check if we also have a type present (if so this should be put into the query
						if ("id".equals(secondDataSet.getFacetField())
								&& secondDataSet.getQueries().get(0).getDsoType() != -1) {
							facetQuery += " AND type:" + secondDataSet.getQueries().get(0).getDsoType();
						}
						dataset.addValueToMatrix(j, i, facetResult.get(facetQuery));
						Bitstream bit = bitstreamService.findByIdOrLegacyId(context, dsoId);
						if (bit != null) {
							Item owningItem = null;
							List<Bundle> bunds = bit.getBundles();
							if (0 < bunds.size() && 0 < bunds.get(0).getItems().size()) {
								owningItem = bunds.get(0).getItems().get(0);
							}
							if (owningItem != null) {
								dataset.addValueToMatrix(1, i, owningItem.getName());
							}
						}
						// We got our query so now get the value

					}
				}
			} else {
				// Make sure we have a dataSet
				dataset = new Dataset(3, topCounts1.length);
				for (int i = 0; i < topCounts1.length; i++) {
					ObjectCount count = topCounts1[i];
					String dsoId = count.getValue();
					dataset.setColLabel(i, getResultName(count.getValue(), firsDataset, context));
					dataset.setColLabelAttr(i, getAttributes(count.getValue(), firsDataset, context));
					dataset.addValueToMatrix(0, i, count.getCount());
					Bitstream bit = bitstreamService.findByIdOrLegacyId(context, dsoId);
					if (bit != null) {
						Item owningItem = null;
						List<Bundle> bunds = bit.getBundles();
						if (0 < bunds.size() && 0 < bunds.get(0).getItems().size()) {
							owningItem = bunds.get(0).getItems().get(0);
						}
						if (owningItem != null) {
							dataset.addValueToMatrix(1, i, owningItem.getName());
						}
					}
				}
			}

		}
		if (dataset != null) {
			dataset.setRowTitle("Dataset 1");
			dataset.setColTitle("Dataset 2");
		} else {
			dataset = new Dataset(0, 0);
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

	protected String getResultName(String value, DatasetQuery datasetQuery, Context context) throws SQLException {

		Query query = datasetQuery.getQueries().get(0);
		// TODO: CHANGE & THROW AWAY THIS ENTIRE METHOD
		// Check if int
		String dsoId;
		// DS 3602: Until all legacy stats records have been upgraded to using UUID,
		// duplicate reports may be presented for each DSO. A note will be appended when
		// reporting legacy counts.
		String legacyNote = "";
		int dsoLength = query.getDsoLength();
		try {
			dsoId = UUID.fromString(value).toString();
		} catch (Exception e) {
			try {
				// Legacy identifier support
				dsoId = String.valueOf(Integer.parseInt(value));
				legacyNote = "(legacy)";
			} catch (NumberFormatException e1) {
				dsoId = null;
			}
		}
		if (dsoId == null && query.getDso() != null && value == null) {
			dsoId = query.getDso().getID().toString();
		}

		if (dsoId != null && query.getDsoType() != -1) {
			switch (query.getDsoType()) {
			case Constants.BITSTREAM:
				Bitstream bit = bitstreamService.findByIdOrLegacyId(context, dsoId);
				if (bit == null) {
					break;
				}
				return bit.getName() + legacyNote;
			case Constants.ITEM:
				Item item = itemService.findByIdOrLegacyId(context, dsoId);
				if (item == null) {
					break;
				}
				String name = "untitled";
				List<MetadataValue> vals = itemService.getMetadata(item, "dc", "title", null, Item.ANY);
				if (vals != null && 0 < vals.size()) {
					name = vals.get(0).getValue();
				}
				if (dsoLength != -1 && name.length() > dsoLength) {
					// Cut it off at the first space
					int firstSpace = name.indexOf(' ', dsoLength);
					if (firstSpace != -1) {
						name = name.substring(0, firstSpace) + " ...";
					}
				}

				return name + legacyNote;

			case Constants.COLLECTION:
				Collection coll = collectionService.findByIdOrLegacyId(context, dsoId);
				if (coll == null) {
					break;
				}
				name = coll.getName();

				if (dsoLength != -1 && name.length() > dsoLength) {
					// Cut it off at the first space
					int firstSpace = name.indexOf(' ', dsoLength);
					if (firstSpace != -1) {
						name = name.substring(0, firstSpace) + " ...";
					}
				}
				return name + legacyNote;

			case Constants.COMMUNITY:
				Community comm = communityService.findByIdOrLegacyId(context, dsoId);
				if (comm == null) {
					break;
				}
				name = comm.getName();

				if (dsoLength != -1 && name.length() > dsoLength) {
					// Cut it off at the first space
					int firstSpace = name.indexOf(' ', dsoLength);
					if (firstSpace != -1) {
						name = name.substring(0, firstSpace) + " ...";
					}
				}
				return name + legacyNote;
			default:
				break;
			}
		}
		return value;
	}

	protected Map<String, String> getAttributes(String value, DatasetQuery datasetQuery, Context context)
			throws SQLException {
		HashMap<String, String> attrs = new HashMap<>();
		Query query = datasetQuery.getQueries().get(0);
		// TODO: CHANGE & THROW AWAY THIS ENTIRE METHOD
		// Check if int
		String dsoId;
		try {
			dsoId = UUID.fromString(value).toString();
		} catch (Exception e) {
			try {
				// Legacy identifier support
				dsoId = String.valueOf(Integer.parseInt(value));
			} catch (NumberFormatException e1) {
				dsoId = null;
			}
		}
		if (dsoId == null && query.getDso() != null && value == null) {
			dsoId = query.getDso().getID().toString();
		}

		if (dsoId != null && query.dsoType != -1) {
			// Store the UUID of the DSO as an attribute. Needed in particular for Bitstream
			// download usage reports,
			// as the Bitstream itself won't be available when converting points to their
			// REST representation
			attrs.put("id", dsoId);

			switch (query.dsoType) {
			case Constants.BITSTREAM:
				Bitstream bit = bitstreamService.findByIdOrLegacyId(context, dsoId);
				if (bit == null) {
					break;
				}

				// Get our owning item
				Item owningItem = null;
				List<Bundle> bunds = bit.getBundles();
				if (0 < bunds.size() && 0 < bunds.get(0).getItems().size()) {
					owningItem = bunds.get(0).getItems().get(0);
				}

				// If possible reference this bitstream via a handle, however this may
				// be null if a handle has not yet been assigned. In this case reference the
				// item its internal id. In the last case where the bitstream is not associated
				// with an item (such as a community logo) then reference the bitstreamID
				// directly.
				String identifier;
				if (owningItem != null && owningItem.getHandle() != null) {
					identifier = "handle/" + owningItem.getHandle();
				} else if (owningItem != null) {
					identifier = "item/" + owningItem.getID();
				} else {
					identifier = "id/" + bit.getID();
				}

				String url = configurationService.getProperty("dspace.ui.url") + "/bitstream/" + identifier + "/";

				// If we can put the pretty name of the bitstream on the end of the URL
				try {
					if (bit.getName() != null) {
						url += Util.encodeBitstreamName(bit.getName(), "UTF-8");
					}
				} catch (UnsupportedEncodingException uee) {
					// Just ignore it: we don't have to have a pretty
					// name on the end of the URL because the sequence id will
					// locate it. However it means that links in this file might
					// not work....
				}

				url += "?sequence=" + bit.getSequenceID();
				attrs.put("item", owningItem.getName());

				attrs.put("url", url);
				break;

			case Constants.ITEM:
				Item item = itemService.findByIdOrLegacyId(context, dsoId);
				if (item == null || item.getHandle() == null) {
					break;
				}

				attrs.put("url", handleService.resolveToURL(context, item.getHandle()));
				break;

			case Constants.COLLECTION:
				Collection coll = collectionService.findByIdOrLegacyId(context, dsoId);
				if (coll == null || coll.getHandle() == null) {
					break;
				}

				attrs.put("url", handleService.resolveToURL(context, coll.getHandle()));
				break;

			case Constants.COMMUNITY:
				Community comm = communityService.findByIdOrLegacyId(context, dsoId);
				if (comm == null || comm.getHandle() == null) {
					break;
				}

				attrs.put("url", handleService.resolveToURL(context, comm.getHandle()));
				break;
			default:
				break;
			}
		}
		return attrs;
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
					System.out.println("I am here in if dspacelegacy + "+query);
				} else {
					query += "id:" + dso.getID();
					System.out.println("I am here in else dspacelegacy + "+query);
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
				System.out.println("The owning string is : "+owningStr);
				if (currentDso instanceof DSpaceObjectLegacySupport) {
					owningStr = "(" + owningStr + ":" + currentDso.getID() + " OR " + owningStr + ":"
							+ ((DSpaceObjectLegacySupport) currentDso).getLegacyId() + ")";
					System.out.println("The owningStr DSpaceObjectLegacySupport : "+owningStr);
				} else {
					owningStr += ":" + currentDso.getID();
					System.out.println("The owningStr not DSpaceObjectLegacySupport : "+owningStr);
				}

				query += owningStr;
			}

			if (query.equals("")) {
				query = "*:*";
			}
			return query;
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

	protected ObjectCount[] queryFacetField(DatasetQuery dataset, String query, String filterQuery, int facetMinCount)
			throws SolrServerException, IOException {
		String facetType = dataset.getFacetField() == null ? "id" : dataset.getFacetField();
		return solrLoggerService.queryFacetField(query, filterQuery, facetType, dataset.getMax(), false, null,
				facetMinCount);
	}

}
