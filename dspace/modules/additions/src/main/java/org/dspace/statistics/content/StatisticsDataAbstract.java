package org.dspace.statistics.content;

import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.content.DatasetViewGenerator.IpRange;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;
import org.dspace.statistics.util.LocationUtils;

public abstract class StatisticsDataAbstract extends StatisticsData {
    private static Logger log = Logger.getLogger(StatisticsDataAbstract.class);

    private static String ipRanges = null;
    
    private DSpaceObject currentDso;
    private String currentAuthor;
    
    private String filterQuery;
    private DatasetTimeGenerator dateFacet;
    private boolean showTotal = false;
	/*@Override
	public Dataset createDataset(Context context) throws SQLException,
			SolrServerException, IOException, ParseException {
		// TODO Auto-generated method stub
		return null;
	}*/
    
    public StatisticsDataAbstract(DSpaceObject dso) {
    	super();
    	this.currentDso = dso;
    }
    
    public StatisticsDataAbstract(DSpaceObject dso, String author) {
    	super();
    	this.currentDso = dso;
    	this.currentAuthor = author;
    }
	
	protected void loadIpRanges() {
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
	}
	
	protected void generateDefaultFilterQuery(IpRange range) {
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

		filterQuery = "";
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
		
		filterQuery += getIpFilterQueryAddition(range);
	}
	
	protected boolean isShowTotal() {
		return showTotal;
	}
	
	protected String getDefaultFilterQuery(IpRange range) {
		
		if (filterQuery == null) {
			generateDefaultFilterQuery(range);
		}
		return filterQuery;
	}
	
	protected String getIpRanges() {
		return ipRanges;
	}
	
	protected String getIpFilterQueryAddition(IpRange range) {
		if (ipRanges != null) {
			if (range == IpRange.EXTERNAL) {
				return "AND -(" + ipRanges + ")";
			}
			else if (range == IpRange.INTERNAL) {
				return "AND (" + ipRanges + ")";
			}
		}
		return "";
	}
	
	protected ObjectCount[] executeQuery(String query, String filterQuery, String facetField, int max
			, boolean showTotal, Context context) throws SolrServerException {
		if (dateFacet != null) {
			return SolrLogger.queryFacetDate(query, filterQuery, max, dateFacet.getDateType(),
					dateFacet.getStartDate(), dateFacet.getEndDate(), showTotal, context);
		}
		else {
			return SolrLogger.queryFacetField(query, filterQuery, facetField, max, showTotal, null);
		}
	}
	
	protected String getOwningType() {
		if (currentDso != null) {
			int resourceType = currentDso.getType();
			switch (resourceType) {
			case Constants.ITEM:
				return "owningItem";
			case Constants.COLLECTION:
				return "owningColl";
			case Constants.COMMUNITY:
				return "owningComm";
			}
		}
		
		return null;
	}
	
	protected String getOwningType(int type) {
		switch (type) {
		case Constants.ITEM:
			return "owningItem";
		case Constants.COLLECTION:
			return "owningColl";
		case Constants.COMMUNITY:
			return "owningComm";
		}
		return null;
	}
	
    /**
     * Gets the name of the DSO (example for collection: ((Collection) dso).getname();
     * @return the name of the given DSO
     */
    protected String getResultName(String value, String type, int filterType,
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
        else if(filterType >= 0) {
        	if ("id".equals(type) || "owningColl".equals(type) || "owningComm".equals(type)) {
        		try {
	        		int id = Integer.parseInt(value);
	        		DSpaceObject dso = DSpaceObject.find(context, filterType, id);
	        		if (dso != null) {
	        			value = dso.getName();
	        		}
	        		else {
	        			log.info("Unable to set object name for statistics. DSO is null where the resource_type_id is "+filterType+" and resource_id is "+id);
	        		}
        		}
        		catch (NumberFormatException e) {
        			log.error("Unable to determine resource id for"+value);
        		}
        	}
        }
        return value;
    }
}
