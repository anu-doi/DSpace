package org.dspace.statistics.content;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.statistics.Dataset;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class StatisticsDataAccessionedByType extends StatisticsData {
    private static Logger log = Logger.getLogger(StatisticsDataAccessionedByType.class);
    
	private DSpaceObject currentDso;
	private Date startDate;
	private Date endDate;
	
	public StatisticsDataAccessionedByType(DSpaceObject dso, Date startDate, Date endDate) {
		super();
		this.currentDso = dso;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	@Override
	public Dataset createDataset(Context context) throws SQLException,
			SolrServerException, IOException, ParseException {
		if (getDataset() != null) {
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
		
		Dataset dataset = null;
    	Object[] params = new Object[0];
    	if (startDate != null && endDate != null) {
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    		if (currentDso != null && currentDso.getType() == Constants.COLLECTION) {
    			params = new Object[] {sdf.format(startDate), sdf.format(endDate), currentDso.getID()};
    		}
    		else {
    			params = new Object[] {sdf.format(startDate), sdf.format(endDate)};
    		}
    	}
    	else if (currentDso != null && currentDso.getType() == Constants.COLLECTION) {
    		params = new Object[] {currentDso.getID()};
    	}
    	
    	TableRowIterator tableRowIterator = DatabaseManager.query(context, getTypeQuery(startDate, endDate, false), params);
    	List<TableRow> tableRowList = tableRowIterator.toList();
    	dataset = new Dataset(tableRowList.size() + 1, 1);
    	dataset.setColLabel(0, "Records");
		for (int i = 0; i < tableRowList.size(); i++) {
			TableRow tableRow = tableRowList.get(i);
			dataset.setRowLabel(i, tableRow.getStringColumn("Type"));
			dataset.addValueToMatrix(i, 0, tableRow.getLongColumn("Count"));
		}
		tableRowIterator = DatabaseManager.query(context, getTypeQuery(startDate, endDate, true), params);
		tableRowList = tableRowIterator.toList();
		
		TableRow tableRow = tableRowList.get(0);
		dataset.setRowLabel(dataset.getNbRows()-1, tableRow.getStringColumn("Type"));
		dataset.addValueToMatrix(dataset.getNbRows()-1, 0, tableRow.getLongColumn("Count"));
		
		
		return dataset;
	}
    
	private String getTypeQuery(Date startDate, Date endDate, boolean isTotal) {

		StringBuilder query = new StringBuilder();
		if (isTotal) {
			query.append("select 'Total' as Type, count(mv1.text_value) as Count");
		}
		else {
			query.append("select mv1.text_value as Type, count(mv1.text_value) as Count");
		}
		query.append(" from metadataschemaregistry msr");
		query.append(" , metadatafieldregistry mfr1");
		query.append(" , metadatavalue mv1");
		query.append(" , metadatafieldregistry mfr2");
		query.append(" , metadatavalue mv2");
		/*if (resourceType == Constants.COLLECTION || resourceType == Constants.COMMUNITY) {
			query.append(" , item i");
		}*/
		query.append(" where msr.short_id = 'dc'");
		query.append(" and mfr1.metadata_schema_id = msr.metadata_schema_id");
		query.append(" and mfr1.element = 'type'");
		query.append(" and mfr1.qualifier is null");
		query.append(" and mv1.metadata_field_id = mfr1.metadata_field_id");
		query.append(" and mfr2.metadata_schema_id = msr.metadata_schema_id");
		query.append(" and mfr2.element = 'date'");
		query.append(" and mfr2.qualifier = 'accessioned'");
		query.append(" and mv2.item_id = mv1.item_id");
		query.append(" and mv2.metadata_field_id = mfr2.metadata_field_id");
		if (startDate != null && endDate != null) {
			query.append(" and mv2.text_value > ?");
			query.append(" and mv2.text_value < ?");
		}
		if (currentDso != null) {
			if (currentDso.getType() == Constants.COLLECTION) {
				//query.append(" and exists (select 1 from collection2item c2i where c2i.collection_id = ? and c2i.item_id = mv1.item_id)");
				query.append(" and exists (select 1 from item i where i.owning_collection = ? and i.item_id = mv1.item_id)");
			}
			else if (currentDso.getType() == Constants.COMMUNITY) {
				Community community = (Community) currentDso;
				try {
					Set<Integer> collectionIds = getCollectionIDs(community);
					//query.append(" and exists (select 1 from collection2item c2i where c2i.collection_id in (");
					query.append(" and exists (select 1 from item i where i.owning_collection in (");
					boolean isStart = true;
					for (Integer id : collectionIds) {
						if (!isStart) {
							query.append(",");
						}
						else {
							isStart = false;
						}
						query.append(id);
					}
					//query.append(") and c2i.item_id = mv1.item_id)");
					query.append(") and i.item_id = mv1.item_id)");
				}
				catch (SQLException e) {
					log.error("Unable to retrieve collection ids for Community '"+community.getName()+"'");
				}
			}
		}
		if (!isTotal) {
			query.append(" group by mv1.text_value");
		}
		query.append(" order by Type");
		
		log.debug("Query String: "+query.toString());
		
		return query.toString();
	}
	
	private Set<Integer> getCollectionIDs(Community community) throws SQLException {
		Set<Integer> ids = new HashSet<Integer>();
		Collection[] allCollections = community.getAllCollections();
		for (Collection iColl : allCollections) {
			ids.add(iColl.getID());
		}
		return ids;
	}
	
}
