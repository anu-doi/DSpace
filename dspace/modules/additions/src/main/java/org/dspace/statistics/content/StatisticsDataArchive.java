package org.dspace.statistics.content;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;

public class StatisticsDataArchive extends StatisticsData {
	private DSpaceObject currentDso;
	
	public StatisticsDataArchive(DSpaceObject dso) {
		super();
		this.currentDso = dso;
	}
	
	@Override
	public Dataset createDataset(Context context) throws SQLException,
			SolrServerException, IOException, ParseException {
		if(getDataset() != null) {
			return getDataset();
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
		Dataset dataset = new Dataset(0,0);
		
		int resourceType = Constants.ITEM;
		int resourceId = -1;
		if (currentDso != null) {
			resourceType = currentDso.getType();
			resourceId = currentDso.getID();
		}
		String owningType = null;
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
		StringBuilder query = new StringBuilder();
		query.append("workflowStep:ARCHIVE");
		if (currentDso != null) {
			query.append(" AND "+owningType+":"+resourceId);
		}
		
		ObjectCount[] results = SolrLogger.queryFacetField(query.toString(), filterQuery, "owningColl", -1, showTotal, null);
		dataset = new Dataset(results.length, 1);
		dataset.setColLabel(0, "Records");
		for (int i = 0; i < results.length; i++) {
			dataset.setRowLabel(i, getResultName(results[i].getValue(), Constants.COLLECTION, context));
			dataset.addValueToMatrix(i, 0, results[i].getCount());
		}
		
		return dataset;
	}

    private String getResultName(String value, int type,
            Context context) throws SQLException
    {
    	if (type > -1) {
            int dsoId;
            try {
                dsoId = Integer.parseInt(value);
            }catch(Exception e){
                dsoId = -1;
            }
            String name = "Untitled";
            DSpaceObject dso = DSpaceObject.find(context, type, dsoId);
            if (dso != null) {
	            switch(dso.getType()) {
	            case Constants.BITSTREAM:
					Bitstream bit = (Bitstream) dso;
					return bit.getName();
	            case Constants.ITEM:
	                Item item = (Item) dso;
	                name = "untitled";
	                Metadatum[] vals = item.getMetadata("dc", "title", null, Item.ANY);
	                if(vals != null && 0 < vals.length)
	                {
	                    name = vals[0].value;
	                }
	                return name;
	            case Constants.COLLECTION:
	                Collection coll = (Collection) dso;
	                if (coll != null) {
		                name = coll.getName();
		            	return name;
	                }
	            case Constants.COMMUNITY:
	                Community comm = (Community) dso;
	                name = comm.getName();
	                return name;
	            }
            }
    	}
        return value;
    }
	
}
