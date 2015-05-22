package org.dspace.statistics.content;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverResult.FacetResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters.SORT;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.SolrLogger;

public class StatisticsDataItemCount extends StatisticsData {
    private static Logger log = Logger.getLogger(StatisticsDataItemCount.class);
    
	private DSpaceObject currentDso;
	private Date asAtDate;
	
	public StatisticsDataItemCount(DSpaceObject dso) {
		this.currentDso = dso;
	}
	
	public StatisticsDataItemCount(DSpaceObject dso, Date endDate) {
		this.currentDso = dso;
		this.asAtDate = endDate;
	}

	@Override
	public Dataset createDataset(Context context) throws SQLException,
			SolrServerException, IOException, ParseException {
		Dataset dataset = new Dataset(0,0);
		
		DiscoverQuery query = new DiscoverQuery();
		if (asAtDate != null) {
			SimpleDateFormat formatter = new SimpleDateFormat(SolrLogger.DATE_FORMAT_8601);
			String endDate = formatter.format(asAtDate); 
			query.setQuery("search.resourcetype:2 AND dc.date.accessioned_dt:[* TO "+endDate+"]");
		}
		else {
			query.setQuery("search.resourcetype:2");
		}
		query.setFacetMinCount(1);
		
		DiscoverFacetField collectionFacet = new DiscoverFacetField("location.coll",DiscoveryConfigurationParameters.TYPE_STANDARD,-1,SORT.COUNT);
		query.addFacetField(collectionFacet);
		DiscoverFacetField communityFacet = new DiscoverFacetField("location.comm",DiscoveryConfigurationParameters.TYPE_STANDARD,-1,SORT.COUNT);
		query.addFacetField(communityFacet);
		
		Map<String, Long> listingResults = new HashMap<String, Long>();
		try {
			DiscoverResult results = null;
			if (currentDso != null) {
				results = SearchUtils.getSearchService().search(context, currentDso, query);
			}
			else {
				results = SearchUtils.getSearchService().search(context, query);
			}
			List<FacetResult> collectionResults = results.getFacetResult("location.coll");
			
			for (FacetResult result : collectionResults) {
				listingResults.put(result.getDisplayedValue(), result.getCount());
			}

			List<FacetResult> communityResults = results.getFacetResult("location.comm");
			for (FacetResult result : communityResults) {
				listingResults.put(result.getDisplayedValue(), result.getCount());
			}
			
			List<String> names = getNamesToShow(context);
			Map<String, Long> counts = new TreeMap<String, Long>();
			for (String name : names) {
				Long value = listingResults.get(name);
				if (value != null) {
					counts.put(name, value);
				}
				else {
					counts.put(name, 0L);
				}
			}
			
			int i = 0;
			dataset = new Dataset(counts.size(), 1);
			for (Entry<String, Long> entry : counts.entrySet()) {
				dataset.setRowLabel(i, entry.getKey());
				dataset.addValueToMatrix(i, 0, entry.getValue());
				i++;
			}
		}
		catch(SearchServiceException e) {
			log.error("Error for item counts", e);
		}
		
		return dataset;
	}
	
	private List<String> getNamesToShow(Context context) {
		List<String> names = new ArrayList<String>();
		
		if (currentDso == null) {
			try {
				Community[] communities = Community.findAll(context);
				
				for (Community community : communities) {
					names.add(community.getName());
				}
				
				Collection[] collections = Collection.findAll(context);
				
				for (Collection collection : collections) {
					names.add(collection.getName());
				}
			}
			catch (SQLException e) {
				log.error("Error obtaining communities and collections");
			}
		}
		else if (currentDso.getType() == Constants.COLLECTION) {
			Collection collection = (Collection) currentDso;
			addNamesToShow(collection, names);
		}
		else if (currentDso.getType() == Constants.COMMUNITY) {
			Community community = (Community) currentDso;
			addNamesToShow(community, names);
		}
		
		return names;
	}
	
	private void addNamesToShow(Community community, List<String> names) {
		names.add(community.getName());
		try {
			for (Community subCommunity : community.getSubcommunities()) {
				addNamesToShow(subCommunity, names);
			}
			for (Collection collection : community.getCollections()) {
				addNamesToShow(collection, names);
			}
		}
		catch (SQLException e) {
			log.error("Unable retrieve either sub communities or collections");
		}
	}
	
	private void addNamesToShow(Collection collection, List<String> names) {
		names.add(collection.getName());
	}

}
