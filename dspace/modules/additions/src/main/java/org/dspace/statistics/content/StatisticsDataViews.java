package org.dspace.statistics.content;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.dspace.statistics.util.LocationUtils;

/**
 * 
 * @author Genevieve Turner
 *
 */
public class StatisticsDataViews extends StatisticsData {
    private static Logger log = Logger.getLogger(StatisticsDataViews.class);
    /** Current DSpaceObject for which to generate the statistics. */
    private DSpaceObject currentDso;
    
    private String ipRanges = null;
    
	public StatisticsDataViews(DSpaceObject dso)
	{
		super();
		this.currentDso = dso;
	}

	@Override
	public Dataset createDataset(Context context) throws SQLException,
			SolrServerException, IOException, ParseException {
		if (getDataset() != null)
		{
			return getDataset();
		}
		if (ipRanges == null) {
			String internalIpAddressList = ConfigurationManager.getProperty("ipaddress.internal");
			String[] ipAddressArray = internalIpAddressList.split(", ");
			StringUtils.join(ipAddressArray, " OR ip:");
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
		//TODO add dataset generators
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
			
			String referrer = "(referrer:*/handle/"+viewGenerator.getHandle()+" OR referrer:*/handle/"+viewGenerator.getHandle()+"?mode=simple)";
			
			
			int nrColumns = 1;
			if (viewGenerator.isShowFullView()) {
				nrColumns++;
			}
			if (viewGenerator.isShowFileDownloads()) {
				nrColumns++;
			}
			Map<Integer, ObjectCount[]> resultsMap = new HashMap<Integer, ObjectCount[]>();
			int colNum = 0;
			// View
			int resourceType = currentDso.getType();
			int resourceId = currentDso.getID();
			
			String owningType = "";
			switch (resourceType) {
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
			
			//String type = viewGenerator.getType() == null ? "id" : viewGenerator.getType();
			String type = viewGenerator.getType() == null ? "statistics_type" : viewGenerator.getType();
			//String viewQuery = "type:"+resourceType+" AND id:"+resourceId;
			String viewQuery = "(type:"+resourceType+" AND id:"+resourceId+") OR ("+owningType+":"+resourceId+" AND (type:2 OR type:3 OR type:4))";
			ObjectCount[] viewResults = null;
			String viewFilterQuery = filterQuery;
			if (viewGenerator.isShowFullView()) {
				viewFilterQuery = viewFilterQuery + " AND NOT "+referrer;
			}
			if (dateFacet != null) {
				viewResults = SolrLogger.queryFacetDate(viewQuery, viewFilterQuery, viewGenerator.getMax(), dateFacet.getDateType(), dateFacet.getStartDate(), dateFacet.getEndDate(), showTotal);
			}
			else {
				viewResults = SolrLogger.queryFacetField(viewQuery, viewFilterQuery, type, viewGenerator.getMax(), showTotal, null);
			}
			resultsMap.put(colNum, viewResults);
			//int viewColNum = colNum;
			
			ObjectCount[] fullViewResults = null;
			if (viewGenerator.isShowFullView()) {
				colNum++;
				String fullViewFilterQuery = filterQuery + " AND " + referrer;
				
				if (dateFacet != null) {
					fullViewResults = SolrLogger.queryFacetDate(viewQuery, fullViewFilterQuery, viewGenerator.getMax(), dateFacet.getDateType(), dateFacet.getStartDate(), dateFacet.getEndDate(), showTotal);
				}
				else {
					fullViewResults = SolrLogger.queryFacetField(viewQuery, fullViewFilterQuery, type, viewGenerator.getMax(), showTotal, null);
				}
				resultsMap.put(colNum, fullViewResults);
			}
			
			ObjectCount[] downloadResults = null;
			// Downloads
			if (viewGenerator.isShowFileDownloads()) {
				colNum++;
				String downloadQuery = "type:0 AND "+owningType+":"+resourceId;
				String downloadType = viewGenerator.getType() == null ? "statistics_type" : viewGenerator.getType();
				
				if (dateFacet != null) {
					downloadResults = SolrLogger.queryFacetDate(downloadQuery, filterQuery, viewGenerator.getMax(), dateFacet.getDateType(), dateFacet.getStartDate(), dateFacet.getEndDate(), showTotal);
				}
				else {
					downloadResults = SolrLogger.queryFacetField(downloadQuery, filterQuery, downloadType, viewGenerator.getMax(), showTotal, null);
				}
				resultsMap.put(colNum, downloadResults);
			}
			
			int maxResults = 1000;
			if (viewGenerator.getMax() > 0) {
				maxResults = viewGenerator.getMax();
			}
			int orderColumn = viewGenerator.getOrderColumn();
			ObjectCount[] orderCount = resultsMap.get(orderColumn);
			List<String> rowLabels = new ArrayList<String>();
			for (int i = 0; i < orderCount.length; i++) {
				rowLabels.add(orderCount[i].getValue());
			}
			for (Entry<Integer, ObjectCount[]> entry : resultsMap.entrySet()) {
				for (ObjectCount count : entry.getValue()) {
					if (!rowLabels.contains(count.getValue())) {
						rowLabels.add(count.getValue());
					}
				}
			}
			if (maxResults > rowLabels.size()) {
				maxResults = rowLabels.size();
			}
			
			dataset = new Dataset(maxResults, nrColumns);
			
			for (int i = 0; i < maxResults; i++) {
				dataset.setRowLabel(i, rowLabels.get(i));
			}
			colNum = 0;
			if (viewGenerator.isShowFullView()) {
				dataset.setColLabel(colNum, "Simple Views");
			}
			else {
				dataset.setColLabel(colNum, "Views");
			}
			if (viewGenerator.isShowFullView()) {
				colNum++;
				dataset.setColLabel(colNum, "Full Views");
			}
			if (viewGenerator.isShowFileDownloads()) {
				colNum++;
				dataset.setColLabel(colNum, "Downloads");
			}
			
			for (Entry<Integer, ObjectCount[]> entry : resultsMap.entrySet()) {
				ObjectCount[] countResults = entry.getValue();
				for (ObjectCount count : countResults) {
					int rowNum = dataset.getRowLabels().indexOf(count.getValue());
					if (rowNum >= 0) {
						dataset.addValueToMatrix(rowNum, entry.getKey(), count.getCount());
					}
				}
			}
			
			for (int i = 0; i < dataset.getRowLabels().size(); i++) {
				String rowLabel = dataset.getRowLabels().get(i);
				dataset.setRowLabel(i, getResultName(rowLabel, type, context));
			}
		}
		
		return dataset;
	}
	

    /**
     * Gets the name of the DSO (example for collection: ((Collection) dso).getname();
     * @return the name of the given DSO
     */
    private String getResultName(String value, String type,
            Context context) throws SQLException
    {
        if("continent".equals(type)){
            value = LocationUtils.getContinentName(value, context
                    .getCurrentLocale());
        }else
        if("countryCode".equals(type)){
            value = LocationUtils.getCountryName(value, context
                    .getCurrentLocale());
        }
        return value;
    }

}
