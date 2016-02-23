package org.dspace.statistics.content;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.ObjectCount;

public class StatisticsDataTopObject extends StatisticsDataAbstract {
	private DSpaceObject currentDso;
	
    private static Logger log = Logger.getLogger(StatisticsDataTopObject.class);
	
	public StatisticsDataTopObject(DSpaceObject dso) {
		super(dso);
		this.currentDso = dso;
	}
	
	public StatisticsDataTopObject(DSpaceObject dso, String author) {
		super(dso, author);
		this.currentDso = dso;
	}

	@Override
	public Dataset createDataset(Context context) throws SQLException,
			SolrServerException, IOException, ParseException {
		if (getDataset() != null) {
			return getDataset();
		}
		
		loadIpRanges();
		
		Dataset dataset = new Dataset(0,0);
		
		DatasetViewGenerator viewGenerator = null;
		for (DatasetGenerator datasetGenerator : getDatasetGenerators()) {
			if (datasetGenerator instanceof DatasetViewGenerator) {
				viewGenerator = (DatasetViewGenerator)datasetGenerator;
			}
		}
		if (viewGenerator != null) {
			String viewFilter = getDefaultFilterQuery(viewGenerator.getIpRange());
			
			
			/*if (viewGenerator.getFilterType() > 0) {
				viewFilter += " AND type:"+viewGenerator.getFilterType();
			}*/
			
			int nrColumns = 2;
			if (viewGenerator.isShowFileDownloads()) {
				nrColumns++;
			}
			
			Map<Integer, ObjectCount[]> resultsMap = new HashMap<Integer, ObjectCount[]>();
			int colNum = 0;
			
			int resourceType = Constants.ITEM;
			int resourceId = -1;
			if (currentDso != null) {
				resourceType = currentDso.getType();
				resourceId = currentDso.getID();
			}
			
			String owningType = getOwningType();
			String type = "";
			if (viewGenerator.getFilterType() == Constants.ITEM) {
				type = "id";
			}
			else {
				type = getOwningType(viewGenerator.getFilterType());
			}
			//String downloadType = getOwningType(viewGenerator.getFilterType());
			//String type = viewGenerator.getType() == null ? "statistics_type" : viewGenerator.getType();
			
			String viewQuery = "";
			if (currentDso != null) {
				if (resourceType == Constants.ITEM) {
					viewQuery = "(type:"+resourceType+" AND id:"+resourceId+")";
				}
				else {
					viewQuery = "("+owningType+":"+resourceId+" AND type:2)";
				}
				//viewQuery = "(type:"+resourceType+" AND id:"+resourceId+") OR ("+owningType+":"+resourceId+" AND (type:2 OR type:3 OR type:4))";
			}
			else {
				viewQuery = "type:2";
			}
			ObjectCount[] viewResults = executeQuery(viewQuery, viewFilter, type, viewGenerator.getMax(),
					isShowTotal(), context);
			resultsMap.put(colNum,  viewResults);
			
			if (viewGenerator.isShowFileDownloads()) {
				String downloadFilter = getDefaultFilterQuery(viewGenerator.getIpRange());
				colNum++;
				String downloadQuery = "";
				if (currentDso != null) {
					downloadQuery = "type:0 AND "+owningType+":"+resourceId;
				}
				else {
					downloadQuery = "type:0 AND owningItem:*";
				}
				String downloadType = getOwningType(viewGenerator.getFilterType());
				//String downloadType = viewGenerator.getType() == null ? "statistics_type" : viewGenerator.getType();
				
				ObjectCount[] downloadResults = executeQuery(downloadQuery, downloadFilter, downloadType,
						viewGenerator.getMax(), isShowTotal(), context);

				log.info("Number of download query results: "+downloadResults.length);
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
			dataset.setColLabel(colNum, "Handle");
			colNum++;
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
						dataset.addValueToMatrix(rowNum, entry.getKey() + 1, count.getCount());
					}
				}
			}
			
			for (int i = 0; i < dataset.getRowLabels().size(); i++) {
				String rowLabel = dataset.getRowLabels().get(i);
				String handle = getHandle(rowLabel, viewGenerator.getFilterType(), context);
				dataset.addValueToMatrix(i, 0, handle);
				dataset.setRowLabel(i, getResultName(rowLabel, type, viewGenerator.getFilterType(), context));
			}
		}
		
		return dataset;
	}

	private String getHandle(String value, int filterType,
	        Context context) throws SQLException
	{
		if (filterType >= 2) {
			int id = Integer.parseInt(value);
			DSpaceObject dso = DSpaceObject.find(context, filterType, id);
			if (dso != null) {
				return dso.getHandle();
			}
		}
		return null;
	}
	
}
