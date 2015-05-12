/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.webui.components.StatisticsBean;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/** 
 * This servlet provides an interface to the statistics reporting for a DSpace
 * repository
 *
 * @author   Richard Jones
 * @version  $Revision$
 */
public class StatisticsServlet extends org.dspace.app.webui.servlet.DSpaceServlet
{
    private static Logger log = Logger.getLogger(StatisticsServlet.class);
	private static String CSV_SEPARATOR = ",";
	
    protected void doDSGet(Context c, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // forward all requests to the post handler
        doDSPost(c, request, response);
    }
    
    protected void doDSPost(Context c, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
		// check to see if the statistics are restricted to administrators
		boolean publicise = ConfigurationManager.getBooleanProperty("report.public");
		
		// determine the navigation bar to be displayed
		String navbar = (!publicise ? "admin" : "default");
		request.setAttribute("navbar", navbar);
		
		// is the user a member of the Administrator (1) group
		boolean admin = Group.isMember(c, 1);
		
		if (publicise || admin)
		{
			showStatistics(c, request, response);
		}
		else
		{
			throw new AuthorizeException();
		}
    }
    
    /**
     * show the default statistics page
     *
     * @param context   current DSpace context
     * @param request   current servlet request object
     * @param response  current servlet response object
     */
    private void showStatistics(Context context, 
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
    	Date startDate = null;
    	Date endDate = null;
    	String filterQuery = null;
    	/*
    	StatisticsBean statsNewItemsType = new StatisticsBean();
    	StatisticsBean statsNewItemsCollection = new StatisticsBean();
    	StatisticsBean statsItemCounts = new StatisticsBean();
    	*/
    	String day = request.getParameter("sDay");
    	if (day != null && !"".equals(day)) {
            String month = request.getParameter("sMonth");
            String year = request.getParameter("sYear");
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day), 0, 0, 0);
            startDate = cal.getTime();

            day = request.getParameter("eDay");
            month = request.getParameter("eMonth");
            year = request.getParameter("eYear");
            cal.set(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
            cal.add(Calendar.DATE, 1);
            endDate = cal.getTime();

    		StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
    		dateFilter.setStartDate(startDate);
    		dateFilter.setEndDate(endDate);

    		filterQuery = "(" + dateFilter.toQuery() +")";
    	}
    	String format = request.getParameter("format");
    	if ("csv".equals(format)) {
    		String section = request.getParameter("section");
    		StatisticsBean exportBean = null;
    		String filename = null;
    		if ("statsNewItemsCollection".equals(section)) {
    			filename = "new-items-by-collection.csv";
    			exportBean = getNewItemsByCollectionBean(context, filterQuery);
    		}
    		else if ("statsNewItemsType".equals(section)) {
    			filename = "new-items-by-type.csv";
    			exportBean = getNewItemsByTypeBean(context, startDate, endDate);
    		}
    		else if ("statsItemCounts".equals(section)) {
    			filename = "item-counts.csv";
    			exportBean = getCommunityListBean(context, endDate);
    		}
    		else {
    			log.error("Unsupported section type: "+section);
    		}
    		if (exportBean != null) {
	    		try {
	
					String[][] matrix = exportBean.getMatrix();
	
					response.setContentType("text/csv); charset=UTF-8");
					response.setHeader("Content-Disposition", "attachment; filename=" + filename);
					PrintWriter out = response.getWriter();
					for (int i = 0; i < matrix.length; i++) {
						String row = exportBean.getRowLabels().get(i) + CSV_SEPARATOR + StringUtils.join(matrix[i], CSV_SEPARATOR);
						out.println(row);
					}
					out.flush();
					out.close();
					return;
	    		}
	    		catch (Exception e) {
	    			log.error("Error exporting to csv", e);
	    		}
    		}
			JSPManager.showInternalError(request, response);
    		return;
    	}

    	StatisticsBean statsNewItemsType = getNewItemsByTypeBean(context, startDate, endDate);
    	StatisticsBean statsNewItemsCollection = getNewItemsByCollectionBean(context, filterQuery);
    	StatisticsBean statsItemCounts = getCommunityListBean(context, endDate);
    	
    	request.setAttribute("statsNewItemsCollection", statsNewItemsCollection);
    	request.setAttribute("statsNewItemsType", statsNewItemsType);
    	request.setAttribute("statsItemCounts", statsItemCounts);
		
		JSPManager.showJSP(request, response, "statistics/report.jsp");
    }
    
    private StatisticsBean getNewItemsByTypeBean(Context context, Date startDate, Date endDate) {
    	StatisticsBean statsBean = new StatisticsBean();
    	try {
    		Dataset dataset = getDatasetNewItemsByType(context, startDate, endDate);
    		if (dataset != null) {
    			String[][] matrix = dataset.getMatrix();
    			List<String> colLabels = dataset.getColLabels();
    			List<String> rowLabels = dataset.getRowLabels();
    			
    			statsBean.setMatrix(matrix);
    			statsBean.setColLabels(colLabels);
    			statsBean.setRowLabels(rowLabels);
    		}
    	}
    	catch (Exception e) {
    		log.error("Error retrieving new items by collection", e);
    	}
    	return statsBean;
    }
    
    private Dataset getDatasetNewItemsByType(Context context, Date startDate, Date endDate) throws SQLException,
		SolrServerException, IOException, ParseException {
    	Dataset dataset = null;
    	Object[] params = new Object[0];
		if (startDate != null && endDate != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			params = new Object[] {sdf.format(startDate), sdf.format(endDate)};
		}
		
		TableRowIterator tableRowIterator = DatabaseManager.query(context, getTypeQuery(startDate, endDate, false), params);
		List<TableRow> tableRowList = tableRowIterator.toList();
		dataset = new Dataset(tableRowList.size() + 1, 1);
		//dataset.
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
		if (!isTotal) {
			query.append(" group by mv1.text_value");
		}
		query.append(" order by Type");
		
		return query.toString();
	}
	
	private StatisticsBean getNewItemsByCollectionBean(Context context, String filterQuery) {
		StatisticsBean statsBean = new StatisticsBean();

    	try {
    		Dataset dataset = getDatasetNewItemsByCollection(context, filterQuery);
    		if (dataset != null) {
    			String[][] matrix = dataset.getMatrix();
    			List<String> colLabels = dataset.getColLabels();
    			List<String> rowLabels = dataset.getRowLabels();
    			
    			statsBean.setMatrix(matrix);
    			statsBean.setColLabels(colLabels);
    			statsBean.setRowLabels(rowLabels);
    		}
    	}
    	catch (Exception e) {
    		log.error("Error retrieving new items by type", e);
    	}
    	
		return statsBean;
	}
	
	private Dataset getDatasetNewItemsByCollection(Context context, String filterQuery) throws SQLException,
		SolrServerException, IOException, ParseException {
		ObjectCount[] results = null;
		results = SolrLogger.queryFacetField("workflowStep:ARCHIVE", filterQuery, "owningColl", -1, true, null);
		Dataset dataset = new Dataset(results.length, 1);
	
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
	                DCValue[] vals = item.getMetadata("dc", "title", null, Item.ANY);
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
    
    private StatisticsBean getCommunityListBean(Context context, Date endDate) {
    	StatisticsBean statsBean = new StatisticsBean();
    	try {
    		Dataset dataset = getDatasetCommunityList(context, endDate);
    		if (dataset != null) {
    			String[][] matrix = dataset.getMatrix();
    			List<String> colLabels = dataset.getColLabels();
    			List<String> rowLabels = dataset.getRowLabels();
    			
    			statsBean.setMatrix(matrix);
    			statsBean.setColLabels(colLabels);
    			statsBean.setRowLabels(rowLabels);
    		}
    	}
    	catch (Exception e) {
    		log.error("Error retrieving collection and community counts", e);
    	}
    	return statsBean;
    }
    
    private Dataset getDatasetCommunityList(Context context, Date endDate) throws SQLException {
		Dataset dataset = null;
		
		Map<String, Long> collectionCounts = getCollectionCounts(context, endDate);
		
		Community[] topCommunities = Community.findAllTop(context);
		
		Map<String, Long> countMap = new TreeMap<String, Long>();
		for (Community community : topCommunities) {
			//community.
			getItemCount(community, collectionCounts, countMap);
		}
		
		dataset = new Dataset(countMap.size(), 1);
		Iterator<Entry<String, Long>> it = countMap.entrySet().iterator();
		for (int i = 0; it.hasNext(); i++) {
			Entry<String, Long> entry = it.next();
			dataset.setRowLabel(i, entry.getKey());
			dataset.addValueToMatrix(i, 0, entry.getValue());
		}
		
		return dataset;
	}
    
	private Long getItemCount(Community community, Map<String, Long> collectionCounts, Map<String, Long> countMap) throws SQLException {
		Long count = new Long(0);
		Collection[] collections = community.getCollections();
		for (Collection collection : collections) {
			Long collCount = getItemCount(collection, collectionCounts, countMap);
			count = count + collCount;
		}
		Community[] subCommunities = community.getSubcommunities();
		for (Community subCommunity : subCommunities) {
			Long commCount = getItemCount(subCommunity, collectionCounts, countMap);
			count = count + commCount;
		}
		countMap.put(community.getName(), count);
		return count;
	}
    
	private Long getItemCount(Collection collection, Map<String, Long> collectionCounts, Map<String, Long> countMap) throws SQLException {
		Long count = collectionCounts.get(collection.getName());
		if (count == null) {
			count = new Long(0);
		}
		countMap.put(collection.getName(), count);
		return count;
	}
	
	private Map<String, Long> getCollectionCounts(Context context, Date endDate) throws SQLException {
		Map<String, Long> collectionCounts = new HashMap<String, Long>();
		
		Object[] params = new Object[0];
		if (endDate != null) {
			//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			
			params = new Object[] {sdf.format(endDate)};
			//params = new Object[] {"2015-04-17T00:00:00Z"};
		}
	
		TableRowIterator tableRowIterator = DatabaseManager.query(context, getCountQuery(endDate), params);
		
		while (tableRowIterator.hasNext()) {
			TableRow tableRow = tableRowIterator.next();
			collectionCounts.put(tableRow.getStringColumn("name"), tableRow.getLongColumn("count"));
		}
		
		return collectionCounts;
	}
    
	private String getCountQuery(Date endDate) {
		StringBuilder query = new StringBuilder();
		query.append("select c.name as name, count(i.item_id) as count");
		query.append(" from collection c, item i");
		query.append(" where i.owning_collection = c.collection_id");
		query.append(" and i.in_archive = '1'");
		query.append(" and i.withdrawn = '0'");
		query.append(" and i.discoverable = '1'");
		if (endDate != null) {
			query.append(" and exists (select 1");
			query.append(" from metadataschemaregistry msr");
			query.append(" , metadatafieldregistry mfr");
			query.append(" , metadatavalue mv");
			query.append(" where msr.short_id = 'dc'");
			query.append(" and mfr.metadata_schema_id = msr.metadata_schema_id");
			query.append(" and mfr.element = 'date'");
			query.append(" and mfr.qualifier = 'accessioned'");
			query.append(" and mv.item_id = i.item_id");
			query.append(" and mv.metadata_field_id = mfr.metadata_field_id");
			query.append(" and mv.text_value < ?)");
		}
		query.append(" group by c.name");
		query.append(" order by c.name;");
		return query.toString();
	}
}
