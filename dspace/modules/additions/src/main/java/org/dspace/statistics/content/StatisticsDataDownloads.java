package org.dspace.statistics.content;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.content.DatasetViewGenerator.IpRange;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;

public class StatisticsDataDownloads extends StatisticsData {
    private DSpaceObject currentDso;
    private String currentAuthor;
    
    private String ipRanges = null;
	
	public StatisticsDataDownloads(DSpaceObject dso)
	{
		super();
		this.currentDso = dso;
	}
	
	public StatisticsDataDownloads(DSpaceObject dso, String author)
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
			
			int nrColumns = 2;
			
			String downloadQuery = "type:0 AND owningItem:*";
			if (currentDso != null) {
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
	
	
				downloadQuery = "type:0 AND "+owningType+":"+resourceId;
			}
			String downloadType = viewGenerator.getType() == null ? "id" : viewGenerator.getType();

			ObjectCount[] downloadResults = null;
			if (dateFacet != null) {
				downloadResults = SolrLogger.queryFacetDate(downloadQuery, filterQuery, viewGenerator.getMax(),
						dateFacet.getDateType(), dateFacet.getStartDate(), dateFacet.getEndDate(), showTotal, context);
			}
			else {
				downloadResults = SolrLogger.queryFacetField(downloadQuery, filterQuery, downloadType,
						viewGenerator.getMax(), showTotal, null);
			}
			
			dataset = new Dataset(downloadResults.length, nrColumns);
			dataset.setColLabel(0, "Handle");
			dataset.setColLabel(1, "Downloads");
			for (int i = 0; i < downloadResults.length; i++) {
				ObjectCount count = downloadResults[i];
				int dsoId = Integer.parseInt(count.getValue());
				try {
					dsoId = Integer.parseInt(count.getValue());
				}
				catch (Exception e) {
					dsoId = -1;
				}
				if (dsoId != -1) {
					DSpaceObject dso = DSpaceObject.find(context, Constants.BITSTREAM, dsoId);
					
					if (dso != null) {
						DSpaceObject parentObject = dso.getParentObject();
						dataset.setRowLabel(i, dso.getName());
						if (parentObject != null) {
							dataset.addValueToMatrix(i, 0, parentObject.getHandle());
						}
					}
					else {
						dataset.setRowLabel(i, Integer.toString(dsoId));
					}
					dataset.addValueToMatrix(i, 1, count.getCount());
					
				}
			}
		}
		
		return dataset;
	}
}
