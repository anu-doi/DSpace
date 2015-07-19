package org.dspace.statistics.content;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.content.DatasetViewGenerator.IpRange;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;

public class StatisticsDataReferralSources extends StatisticsData {
	private static Logger log = Logger.getLogger(StatisticsDataReferralSources.class);

	private static Properties properties = ConfigurationManager.getProperties("statistics-sources");
    private static List<String> sources = new ArrayList<String>() {{
    	Pattern containsPattern = Pattern.compile("source\\.(.*)\\.contains");
		for (Object key : properties.keySet()) {
			String source = (String) key;
			Matcher matcher = containsPattern.matcher(source);
			if (matcher.matches()) {
				String matched = matcher.group(1);
				add(matched);
			}
		}
    }};
    

    private DSpaceObject currentDso;
    private String currentAuthor;
    

    private String ipRanges = null;
    
	public StatisticsDataReferralSources(DSpaceObject dso)
	{
		super();
		this.currentDso = dso;
	}

	public StatisticsDataReferralSources(DSpaceObject dso, String author)
	{
		super();
		this.currentDso = dso;
		this.currentAuthor = author;
	}
	
	@Override
	public Dataset createDataset(Context context) throws SQLException,
			SolrServerException, IOException, ParseException {

		if (ipRanges == null) {
			String internalIpAddressList = ConfigurationManager.getProperty("ipaddress.internal");
			String[] ipAddressArray = internalIpAddressList.split(", ");
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < ipAddressArray.length; i++) {
				if (i > 0) {
					sb.append(" OR ");
				}
				sb.append("ip:");
				if (!ipAddressArray[i].contains("*")) {
					sb.append("\"");
					sb.append(ipAddressArray[i]);
					sb.append("\"");
				}
				else {
					sb.append(ipAddressArray[i]);
				}
			}
			
			ipRanges = sb.toString();
		}
		
		DatasetTimeGenerator dateFacet = null;
		
		boolean showTotal = false;
		
		for (DatasetGenerator datasetGenerator : getDatasetGenerators()) {
			if (datasetGenerator.isIncludeTotal()) {
				showTotal = true;
			}
			if (datasetGenerator instanceof DatasetTimeGenerator) {
				dateFacet = (DatasetTimeGenerator) datasetGenerator;
			}
		}
		
		if (dateFacet != null && dateFacet.getActualStartDate() != null
			&& dateFacet.getActualEndDate() != null)
		{
			StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
			dateFilter.setStartDate(dateFacet.getActualStartDate());
			dateFilter.setEndDate(dateFacet.getActualEndDate());
			dateFilter.setTypeStr(dateFacet.getDateType());
			addFilters(dateFilter);
		}
		else if (dateFacet != null && dateFacet.getStartDate() != null
		        && dateFacet.getEndDate() != null)
		{
			StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
			dateFilter.setStartStr(dateFacet.getStartDate());
			dateFilter.setEndStr(dateFacet.getEndDate());
			dateFilter.setTypeStr(dateFacet.getDateType());
			addFilters(dateFilter);
		}
		
		String filterQuery = "";
		for (int i = 0; i < getFilters().size(); i++) {
			StatisticsFilter filter = getFilters().get(i);
		
			filterQuery += "(" + filter.toQuery() + ")";
			if(i != (getFilters().size() -1))
			{
				filterQuery += " AND ";
			}
		}
		if(StringUtils.isNotBlank(filterQuery)){
			filterQuery += " AND ";
		}
		
		//Only use the view type and make sure old data (where no view type is present) is also supported
		//Solr doesn't explicitly apply boolean logic, so this query cannot be simplified to an OR query
		filterQuery += "-(statistics_type:[* TO *] AND -statistics_type:" + SolrLogger.StatisticsType.VIEW.text() + ")";

		if (currentAuthor != null && !"".equals(currentAuthor)) {
			filterQuery += " AND author:\""+currentAuthor+"\"";
		}
		
		Dataset dataset = new Dataset(0,0);

		DatasetViewGenerator viewGenerator = null;
		for (DatasetGenerator datasetGenerator : getDatasetGenerators()) {
			if (datasetGenerator instanceof DatasetViewGenerator) {
				viewGenerator = (DatasetViewGenerator)datasetGenerator;
			}
		}
		
		if (viewGenerator != null) {
			if (ipRanges != null) {
				if (viewGenerator.getIpRange() == IpRange.EXTERNAL) {
					filterQuery += "AND -(" + ipRanges + ")";
				}
				else if (viewGenerator.getIpRange() == IpRange.INTERNAL) {
					filterQuery += "AND (" + ipRanges + ")";
				}
			}
			
			int resourceType = Constants.ITEM;
			int resourceId = -1;
			if (currentDso != null) {
				resourceType = currentDso.getType();
				resourceId = currentDso.getID();
			}
			
			String owningType = "";
			switch(resourceType) {
			case Constants.ITEM:
				owningType = "owningItem";
				break;
			case Constants.COLLECTION:
				owningType = "owningColl";
				break;
			case Constants.COMMUNITY:
				owningType = "owningComm";
				break;
			}
			
			String type = viewGenerator.getType() == null ? "statistics_type" : viewGenerator.getType();
			Map<String, Long[]> viewsMap = new TreeMap<String, Long[]>();
			
			int sourceCount = 0;
			StringBuilder allReferrers = new StringBuilder();
			
			String viewTypeFilter = "";
			String downloadTypeFilter = "";
			if (currentDso != null) {
				if (resourceType == Constants.ITEM) {
					viewTypeFilter = " AND (type:"+resourceType+" AND id:"+resourceId+")";
				}
				else {
					viewTypeFilter = " AND ("+owningType+":"+resourceId+" AND type:2)";
				}
				//viewTypeFilter = " AND ((type:"+resourceType+" AND id:"+resourceId+") OR ("+owningType+":"+resourceId+" AND (type:2 OR type:3 OR type:4)))";
				downloadTypeFilter = owningType+":"+resourceId+" AND type:0";
			}
			else {
				viewTypeFilter = " AND type:2";
				downloadTypeFilter = " AND owningItem:* AND type:0";
			}
			
			int numCols = 1;
			if (viewGenerator.isShowFileDownloads()) {
				numCols++;
			}
			
			for (String source : sources) {
				Long[] counts = new Long[numCols];
				
				String containsStr = "source."+source+".contains";
				String sourceFilters = properties.getProperty(containsStr);
				String sourceTitle = properties.getProperty("source."+source+".title");
				
				String[] sourceReferrers = sourceFilters.split(", ");
				StringBuilder referrerBuilder = new StringBuilder();
				
				referrerBuilder.append("(");
				for (int i = 0; i < sourceReferrers.length; i++, sourceCount++) {
					if (i > 0) {
						referrerBuilder.append(" OR ");
					}
					if (sourceCount > 0) {
						allReferrers.append(" OR ");
					}
					referrerBuilder.append("referrer:*"+sourceReferrers[i]+"*");
					allReferrers.append("referrer:*"+sourceReferrers[i]+"*");
				}
				referrerBuilder.append(")");
				
				String viewQuery = referrerBuilder.toString() + viewTypeFilter;
				
				ObjectCount[] viewResults = SolrLogger.queryFacetField(viewQuery, filterQuery, type, viewGenerator.getMax(), showTotal, null);
				
				if (viewResults != null && viewResults.length > 0) {
					counts[0] = viewResults[0].getCount();
				}
				else {
					counts[0] = 0L;
				}
				
				String downloadQuery = referrerBuilder.toString() + downloadTypeFilter;

				ObjectCount[] downloadResults = SolrLogger.queryFacetField(downloadQuery, filterQuery, type, viewGenerator.getMax(), showTotal, null);

				if (downloadResults != null && downloadResults.length > 0) {
					counts[1] = downloadResults[0].getCount();
				}
				else {
					counts[1] = 0L;
				}
				
				viewsMap.put(sourceTitle, counts);
			}
			
			Long[] counts = new Long[numCols];
			String viewQuery = "-("+allReferrers.toString()+")"+viewTypeFilter;
			ObjectCount[] viewResults = SolrLogger.queryFacetField(viewQuery, filterQuery, type, viewGenerator.getMax(), showTotal, null);
			
			if (viewResults != null && viewResults.length > 0) {
				counts[0] = viewResults[0].getCount();
			}
			else {
				counts[0] = 0L;
			}
			
			String downloadQuery = "-("+allReferrers.toString()+")"+downloadTypeFilter;
			ObjectCount[] downloadResults = SolrLogger.queryFacetField(downloadQuery, filterQuery, type, viewGenerator.getMax(), showTotal, null);

			if (downloadResults != null && downloadResults.length > 0) {
				counts[1] = downloadResults[0].getCount();
			}
			else {
				counts[1] = 0L;
			}
			viewsMap.put("Other", counts);
			
			dataset = new Dataset(viewsMap.size(), numCols);
			dataset.setColLabel(0, "Views");
			dataset.setColLabel(1, "Downloads");
			int i = 0;
			for (Entry<String, Long[]> entry : viewsMap.entrySet()) {
				dataset.setRowLabel(i, entry.getKey());
				for (int j = 0; j < entry.getValue().length; j++) {
					dataset.addValueToMatrix(i, j, entry.getValue()[j]);
				}
				i++;
			}
		}
		
		return dataset;
	}
}
