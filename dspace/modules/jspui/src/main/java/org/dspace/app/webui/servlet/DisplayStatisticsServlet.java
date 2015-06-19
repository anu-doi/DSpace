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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.webui.components.StatisticsBean;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.DatasetDSpaceObjectGenerator;
import org.dspace.statistics.content.DatasetTimeGenerator;
import org.dspace.statistics.content.DatasetViewGenerator;
import org.dspace.statistics.content.StatisticsDataAccessionedByType;
import org.dspace.statistics.content.StatisticsDataArchive;
import org.dspace.statistics.content.StatisticsDataDownloads;
import org.dspace.statistics.content.StatisticsDataItemCount;
import org.dspace.statistics.content.StatisticsDataReferralSources;
import org.dspace.statistics.content.StatisticsDataTopObject;
import org.dspace.statistics.content.StatisticsDataViews;
import org.dspace.statistics.content.StatisticsListing;
import org.dspace.statistics.content.StatisticsTable;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;



/**
 *
 * 
 * @author Kim Shepherd
 * @version $Revision: 4386 $
 */
public class DisplayStatisticsServlet extends DSpaceServlet
{
	private static final long serialVersionUID = 1L;

	/** log4j logger */
    private static Logger log = Logger.getLogger(DisplayStatisticsServlet.class);

	private static String CSV_SEPARATOR = ",";


    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
		// is the statistics data publically viewable?
		boolean privatereport = ConfigurationManager.getBooleanProperty("usage-statistics", "authorization.admin");

        // is the user a member of the Administrator (1) group?
        boolean admin = Group.isMember(context, 1);

        if (!privatereport || admin)
        {
            displayStatistics(context, request, response);
        }
        else
        {
            throw new AuthorizeException();
        }
    }

    protected void displayStatistics(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {

        DSpaceObject dso = null;
        String handle = request.getParameter("handle");

        if("".equals(handle) || handle == null)
        {
            // We didn't get passed a handle parameter.
            // That means we're looking at /handle/*/*/statistics
            // with handle injected as attribute from HandleServlet
            handle = (String) request.getAttribute("handle");

        }

        if(handle != null)
        {
                dso = HandleManager.resolveToObject(context, handle);
        }

        String format = request.getParameter("format");
        
        boolean isItem = false;
        if (dso != null && dso.getType() == Constants.ITEM) {
        	isItem = true;
        }
        
        Date startDate = null;
        Date endDate = null;

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
        }
        
        String limit = request.getParameter("limit");
        int maxRows = 10;
        try {
        	if (limit != null) {
        		maxRows = Integer.parseInt(limit);
        	}
        }
        catch (Exception e) {
        	log.debug("Error parsing the limit value \""+limit+"\"");
        }
        
        String orderGeo = request.getParameter("orderGeo");
        int orderColumn = 0;
        try {
        	orderColumn = Integer.parseInt(orderGeo);
        }
        catch (Exception e) {
        	log.info("Error parsing the geographical order value \""+orderGeo+"\"");
        }
        
        String ipRanges = request.getParameter("ipRanges");
        String author = request.getParameter("author");
        
        if ("csv".equals(format)) {
            String section = request.getParameter("section");
            StatisticsBean exportBean = getExportBean(context, section, dso, author, startDate, endDate, ipRanges, maxRows, orderColumn);
			if (exportBean == null) {
				return;
			}
            
            try {
				String[][] matrix = exportBean.getMatrix();

				response.setContentType("text/csv); charset=UTF-8");
				String filename = "placeholder";
				if(handle != null) {
					filename = handle.replaceAll("/", "-") + "-" + exportBean.getName().toLowerCase().replaceAll(" ", "-") + ".csv";
				}
				else {
					filename = exportBean.getName().toLowerCase().replaceAll(" ", "-") + ".csv";
				}
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
			JSPManager.showInternalError(request, response);
			return;
        }
        StatisticsBean statsVisits = null;
        StatisticsBean statsMonthlyVisits = null;
        StatisticsBean statsCountryVisits =  null;
        StatisticsBean statsTopAuthors =  null;
        StatisticsBean statsTopDownloads =   null;
        StatisticsBean statsTopItems =  null;
        StatisticsBean statsTopCollections =  null;
        StatisticsBean statsReferralSources =   null;
        StatisticsBean statsNewByCollection =   null;
        StatisticsBean statsNewByType =   null;
        StatisticsBean statsItemCount = null;
        
        statsVisits = getVisits(context, dso, startDate, endDate, ipRanges, author);
        statsMonthlyVisits = getMonthlyVisits(context, dso, startDate, endDate, ipRanges, author);
        statsCountryVisits = getCountryVisits(context, dso, startDate, endDate, ipRanges, author, maxRows, orderColumn);
        if (StringUtils.isEmpty(author) && !(dso != null && dso.getType() == Constants.ITEM)) {
        	statsTopAuthors =  getTopAuthors(context, dso, startDate, endDate, ipRanges, maxRows, orderColumn);
        }
        statsTopDownloads =  getTopDownloadsStatisticsBean(context, dso, startDate, endDate, ipRanges, author);
        if (!(dso != null && dso.getType() == Constants.ITEM)) {
        	statsTopItems =  getTopItemsStatisticsBean(context, dso, startDate, endDate, ipRanges, author, maxRows, orderColumn);
        }
        
        if (StringUtils.isEmpty(author) && !(dso != null && dso.getType() == Constants.ITEM) && !(dso != null && dso.getType() == Constants.COLLECTION)) {
        	statsTopCollections =  getTopCollectionsStatisticsBean(context, dso, startDate, endDate, ipRanges, maxRows, orderColumn);
        }
        statsReferralSources =  getReferralSources(context, dso, startDate, endDate, ipRanges, author);
        if (author == null && (dso == null || dso.getType() == Constants.COMMUNITY || dso.getType() == Constants.COLLECTION)) {
        	statsNewByCollection = getNewItemsByCollection(context, dso, startDate, endDate);
        	statsNewByType = getNewItemsByType(context, dso, startDate, endDate);
        	statsItemCount = getItemCounts(context, dso, endDate);
        }
        
        request.setAttribute("handle", handle);
        if (dso != null) {
        	request.setAttribute("title", dso.getName());
        }
        else if (author != null && !"".equals(author)) {
        	request.setAttribute("title", author);
        }
        else {
        	request.setAttribute("title", "Whole of Repository");
        }
        request.setAttribute("statsVisits", statsVisits);
        request.setAttribute("statsMonthlyVisits", statsMonthlyVisits);
        request.setAttribute("statsCountryVisits",statsCountryVisits);
        request.setAttribute("statsTopAuthors", statsTopAuthors);
        request.setAttribute("statsTopDownloads", statsTopDownloads);
        request.setAttribute("statsTopItems", statsTopItems);
        request.setAttribute("statsTopCollections", statsTopCollections);
        request.setAttribute("statsReferralSources", statsReferralSources);
        request.setAttribute("statsNewByCollection", statsNewByCollection);
        request.setAttribute("statsNewByType", statsNewByType);
        request.setAttribute("statsItemCount", statsItemCount);
        request.setAttribute("isItem", isItem);

        JSPManager.showJSP(request, response, "display-statistics.jsp");
        
    }
    
    private StatisticsBean getExportBean(Context context, String section, DSpaceObject dso, String author
    		, Date startDate, Date endDate, String ipRanges, int maxRows, int orderColumn) {
    	StatisticsBean exportBean = null;
		if ("statsMonthlyVisits".equals(section)) {
			exportBean = getMonthlyVisits(context, dso, startDate, endDate, ipRanges, author);
		}
		else if ("statsCountryVisits".equals(section)) {
			exportBean = getCountryVisits(context, dso, startDate, endDate, ipRanges, author, maxRows, orderColumn);
		}
		else if ("statsTopDownloads".equals(section)) {
			exportBean = getTopDownloadsStatisticsBean(context, dso, startDate, endDate, ipRanges, author);
		}
		else if ("statsReferralSources".equals(section)) {
			exportBean = getReferralSources(context, dso, startDate, endDate, ipRanges, author);
		}
		else if ("statsTopAuthors".equals(section)) {
			exportBean = getTopAuthors(context, dso, startDate, endDate, ipRanges, maxRows, orderColumn);
		}
		else if ("statsTopDownloads".equals(section)) {
			exportBean = getTopDownloadsStatisticsBean(context, dso, startDate, endDate, ipRanges, author);
		}
		else if ("statsTopItems".equals(section)) {
			exportBean = getTopItemsStatisticsBean(context, dso, startDate, endDate, ipRanges, author, maxRows, orderColumn);
		}
		else if ("statsTopCollections".equals(section)) {
			exportBean = getTopCollectionsStatisticsBean(context, dso, startDate, endDate, ipRanges, maxRows, orderColumn);
		}
		else if ("statsReferralSources".equals(section)) {
			exportBean = getReferralSources(context, dso, startDate, endDate, ipRanges, author);
		}
		else if ("statsNewByCollection".equals(section)) {
			exportBean = getNewItemsByCollection(context, dso, startDate, endDate);
		}
		else if ("statsNewByType".equals(section)) {
			exportBean = getNewItemsByType(context, dso, startDate, endDate);
		}
		else if ("statsItemCount".equals(section)) {
			exportBean = getItemCounts(context, dso, endDate);
		}
    	
		return exportBean;
    }
    
    private StatisticsBean getTopDownloadsStatisticsBean(Context context, DSpaceObject dso, Date startDate, Date endDate
    		, String ipRanges, String author) {
    	StatisticsBean statsBean = new StatisticsBean();
    	
		try 
		{
			StatisticsListing statisticsTable = new StatisticsListing(new StatisticsDataDownloads(dso, author));
			
			statisticsTable.setTitle("File Downloads");
			statisticsTable.setId("tab1");
		
			if (startDate != null) {
				StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
				dateFilter.setStartDate(startDate);
				dateFilter.setEndDate(endDate);
				statisticsTable.addFilter(dateFilter);
			}
		    
			DatasetViewGenerator typeAxis = new DatasetViewGenerator();
			typeAxis.setMax(10);
			typeAxis.setIpRange(ipRanges);
			statisticsTable.addDatasetGenerator(typeAxis);
			
			Dataset dataset = statisticsTable.getDataset(context);
			
			dataset = statisticsTable.getDataset();
			
			if (dataset == null)
			{
				dataset = statisticsTable.getDataset(context);
			}
			
			if (dataset != null)
			{
				String[][] matrix = dataset.getMatrix();
				List<String> colLabels = dataset.getColLabels();
				List<String> rowLabels = dataset.getRowLabels();
				
				statsBean.setMatrix(matrix);
				statsBean.setColLabels(colLabels);
				statsBean.setRowLabels(rowLabels);
				
				statsBean.setName("Top Downloads");
			}
		}
		catch (Exception e)
		{
			log.error(
				"Error occured while creating statistics for dso with ID: "
				+ dso.getID() + " and type " + dso.getType()
				+ " and handle: " + dso.getHandle(), e);
		}
    	return statsBean;
    }
    
    private StatisticsBean getMonthlyVisits(Context context, DSpaceObject dso, Date startDate, Date endDate, String ipRanges, String author) {
    	StatisticsBean statsBean = new StatisticsBean();

		try
		{
			StatisticsTable statisticsTable = new StatisticsTable(new StatisticsDataViews(dso, author));
			
			statisticsTable.setTitle("Total Visits Per Month");
			statisticsTable.setId("tab1");
			
			DatasetTimeGenerator timeAxis = new DatasetTimeGenerator();
			timeAxis.setDateInterval("month", "-12", "+1");
			timeAxis.setIncludeTotal(true);
			statisticsTable.addDatasetGenerator(timeAxis);
			
			DatasetViewGenerator dsoAxis = new DatasetViewGenerator();
			dsoAxis.setIncludeTotal(Boolean.TRUE);
			dsoAxis.setShowFileDownloads(Boolean.TRUE);
			dsoAxis.setMax(-1);
			dsoAxis.setIpRange(ipRanges);
			statisticsTable.addDatasetGenerator(dsoAxis);
			
			Dataset dataset = statisticsTable.getDataset(context);
			
			dataset = statisticsTable.getDataset();
			
			if (dataset == null)
			{
				dataset = statisticsTable.getDataset(context);
			}
			
			if (dataset != null)
			{
				String[][] matrix = dataset.getMatrix();
				List<String> colLabels = dataset.getColLabels();
				List<String> rowLabels = dataset.getRowLabels();
				
				statsBean.setMatrix(matrix);
				statsBean.setColLabels(colLabels);
				statsBean.setRowLabels(rowLabels);
				
				statsBean.setName("Monthly Visits");
			}
		} catch (Exception e)
		{
			log.error(
				"Error occured while creating statistics for dso with ID: "
					+ dso.getID() + " and type " + dso.getType()
					+ " and handle: " + dso.getHandle(), e);
		}
    	return statsBean;
    }
    
    private StatisticsBean getVisits(Context context, DSpaceObject dso, Date startDate, Date endDate, String ipRanges, String author) {
    	StatisticsBean statsBean = new StatisticsBean();

		try
		{
			StatisticsListing statListing = new StatisticsListing(
			                                new StatisticsDataViews(dso, author));
			
			statListing.setTitle("Total Visits");
			statListing.setId("list1");
			
			DatasetViewGenerator dsoAxis = new DatasetViewGenerator();
			dsoAxis.setIncludeTotal(Boolean.FALSE);
			dsoAxis.setShowFileDownloads(Boolean.TRUE);
			dsoAxis.setMax(-1);
			dsoAxis.setIpRange(ipRanges);
			statListing.addDatasetGenerator(dsoAxis);
			
			if (startDate != null) {
				StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
				dateFilter.setStartDate(startDate);
				dateFilter.setEndDate(endDate);
				statListing.addFilter(dateFilter);
			}
			
			Dataset dataset = statListing.getDataset(context);
			
			dataset = statListing.getDataset();
			
			if (dataset == null)
			{
				dataset = statListing.getDataset(context);
			}
			
			if (dataset != null)
			{
				String[][] matrix = dataset.getMatrix();
				List<String> colLabels = dataset.getColLabels();
				List<String> rowLabels = dataset.getRowLabels();
				
				statsBean.setMatrix(matrix);
				statsBean.setColLabels(colLabels);
				statsBean.setRowLabels(rowLabels);
				
				statsBean.setName("OVerall Statistics");
			}
		} catch (Exception e)
		{
			log.error(
				"Error occured while creating statistics for dso with ID: "
					+ dso.getID() + " and type " + dso.getType()
					+ " and handle: " + dso.getHandle(), e);
		}
    	return statsBean;
    }
    
    private StatisticsBean getCountryVisits(Context context, DSpaceObject dso, Date startDate, Date endDate, String ipRanges, String author, int maxRows, int orderColumn) {
    	StatisticsBean statsBean = new StatisticsBean();

		try
		{
		
		StatisticsListing statisticsTable = new StatisticsListing(new StatisticsDataViews(dso, author));
		
		statisticsTable.setTitle("Top country views");
		statisticsTable.setId("tab1");
		
		DatasetViewGenerator typeAxis = new DatasetViewGenerator();
		typeAxis.setType("countryCode");
		typeAxis.setShowFileDownloads(Boolean.TRUE);
		typeAxis.setMax(maxRows);
		typeAxis.setIpRange(ipRanges);
		typeAxis.setOrderColumn(orderColumn);
		statisticsTable.addDatasetGenerator(typeAxis);
		
		if (startDate != null) {
			StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
			dateFilter.setStartDate(startDate);
			dateFilter.setEndDate(endDate);
			statisticsTable.addFilter(dateFilter);
		}
		
		Dataset dataset = statisticsTable.getDataset(context);
		
		dataset = statisticsTable.getDataset();
		
		if (dataset == null)
		{
			dataset = statisticsTable.getDataset(context);
		}
		
		if (dataset != null)
		{
			String[][] matrix = dataset.getMatrix();
			List<String> colLabels = dataset.getColLabels();
			List<String> rowLabels = dataset.getRowLabels();
			
			statsBean.setMatrix(matrix);
			statsBean.setColLabels(colLabels);
			statsBean.setRowLabels(rowLabels);
			
			statsBean.setName("Geographic Statistics");
		}
		}
		catch (Exception e)
		{
			log.error(
				"Error occured while creating statistics for dso with ID: "
					+ dso.getID() + " and type " + dso.getType()
					+ " and handle: " + dso.getHandle(), e);
		}
    	return statsBean;
    }
    

    private StatisticsBean getReferralSources(Context context, DSpaceObject dso, Date startDate, Date endDate, String ipRanges, String author) {
		StatisticsBean statsBean = new StatisticsBean();
		try
		{
			StatisticsListing statisticsTable = new StatisticsListing(new StatisticsDataReferralSources(dso, author));

			statisticsTable.setTitle("Referral Sources");
			statisticsTable.setId("tab1");
			
			DatasetViewGenerator typeAxis = new DatasetViewGenerator();
			typeAxis.setShowFileDownloads(Boolean.TRUE);
			typeAxis.setIpRange(ipRanges);
			typeAxis.setMax(-1);
			statisticsTable.addDatasetGenerator(typeAxis);
			
			if (startDate != null) {
				StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
				dateFilter.setStartDate(startDate);
				dateFilter.setEndDate(endDate);
				statisticsTable.addFilter(dateFilter);
			}
			
			Dataset dataset = statisticsTable.getDataset(context);
			
			dataset = statisticsTable.getDataset();
			
			if (dataset == null)
			{
				dataset = statisticsTable.getDataset(context);
			}
			
			if (dataset != null)
			{
				String[][] matrix = dataset.getMatrix();
				List<String> colLabels = dataset.getColLabels();
				List<String> rowLabels = dataset.getRowLabels();
				
				statsBean.setMatrix(matrix);
				statsBean.setColLabels(colLabels);
				statsBean.setRowLabels(rowLabels);
				
				statsBean.setName("Referral Sources");
			}
		}
		catch (Exception e)
		{
			log.error(
				"Error occured while creating statistics for dso with ID: "
					+ dso.getID() + " and type " + dso.getType()
					+ " and handle: " + dso.getHandle(), e);
		}
		return statsBean;
    }
    
    private StatisticsBean getTopItemsStatisticsBean(Context context, DSpaceObject dso, Date startDate, Date endDate, String ipRanges, String author, int maxRows, int orderColumn) {
    	StatisticsBean statsBean = new StatisticsBean();
    	log.info("Start retrieving top collections");

		try
		{
			StatisticsListing statisticsTable = new StatisticsListing(new StatisticsDataTopObject(dso, author));
			
			statisticsTable.setTitle("Top country views");
			statisticsTable.setId("tab1");
			
			DatasetViewGenerator typeAxis = new DatasetViewGenerator();
			typeAxis.setType("id");
			typeAxis.setShowFileDownloads(Boolean.TRUE);
			typeAxis.setMax(maxRows);
			typeAxis.setIpRange(ipRanges);
			typeAxis.setFilterType(Constants.ITEM);
			typeAxis.setOrderColumn(orderColumn);
			statisticsTable.addDatasetGenerator(typeAxis);
			
			if (startDate != null) {
				StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
				dateFilter.setStartDate(startDate);
				dateFilter.setEndDate(endDate);
				statisticsTable.addFilter(dateFilter);
			}
			
			Dataset dataset = statisticsTable.getDataset(context);
			
			dataset = statisticsTable.getDataset();
			
			if (dataset == null)
			{
				dataset = statisticsTable.getDataset(context);
			}
			
			if (dataset != null)
			{
				String[][] matrix = dataset.getMatrix();
				List<String> colLabels = dataset.getColLabels();
				List<String> rowLabels = dataset.getRowLabels();
				
				statsBean.setMatrix(matrix);
				statsBean.setColLabels(colLabels);
				statsBean.setRowLabels(rowLabels);
				
				statsBean.setName("Top Items");
			}
		}
		catch (Exception e)
		{
			log.error(
				"Error occured while creating statistics for dso with ID: "
					+ dso.getID() + " and type " + dso.getType()
					+ " and handle: " + dso.getHandle(), e);
		}
    	log.info("End retrieving top collections");
    	
		return statsBean;
    }
    
    private StatisticsBean getTopCollectionsStatisticsBean(Context context, DSpaceObject dso, Date startDate, Date endDate, String ipRanges, int maxRows, int orderColumn) {
    	StatisticsBean statsBean = new StatisticsBean();
    	log.info("Start retrieving top collections");
		try
		{
			StatisticsListing statisticsTable = new StatisticsListing(new StatisticsDataTopObject(dso));
			
			statisticsTable.setTitle("Top collection views");
			statisticsTable.setId("tab1");
			
			DatasetViewGenerator typeAxis = new DatasetViewGenerator();
			typeAxis.setType("id");
			typeAxis.setShowFileDownloads(Boolean.TRUE);
			typeAxis.setMax(maxRows);
			typeAxis.setIpRange(ipRanges);
			typeAxis.setFilterType(Constants.COLLECTION);
			typeAxis.setOrderColumn(orderColumn);
			statisticsTable.addDatasetGenerator(typeAxis);
			
			if (startDate != null) {
				StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
				dateFilter.setStartDate(startDate);
				dateFilter.setEndDate(endDate);
				statisticsTable.addFilter(dateFilter);
			}
			
			Dataset dataset = statisticsTable.getDataset(context);
			
			dataset = statisticsTable.getDataset();
			
			if (dataset == null)
			{
				dataset = statisticsTable.getDataset(context);
			}
			
			if (dataset != null)
			{
				String[][] matrix = dataset.getMatrix();
				List<String> colLabels = dataset.getColLabels();
				List<String> rowLabels = dataset.getRowLabels();
				
				statsBean.setMatrix(matrix);
				statsBean.setColLabels(colLabels);
				statsBean.setRowLabels(rowLabels);
				
				statsBean.setName("Top Collections");
			}
		}
		catch (Exception e)
		{
			log.error(
				"Error occured while creating statistics for dso with ID: "
					+ dso.getID() + " and type " + dso.getType()
					+ " and handle: " + dso.getHandle(), e);
		}
    	log.info("End retrieving top collections");
    	
		return statsBean;
    }
    
    private StatisticsBean getTopAuthors(Context context, DSpaceObject dso, Date startDate, Date endDate, String ipRanges, int maxRows, int orderColumn) {
    	StatisticsBean statsBean = new StatisticsBean();

		try
		{
			StatisticsListing statisticsTable = new StatisticsListing(new StatisticsDataViews(dso));
			
			statisticsTable.setTitle("Top country views");
			statisticsTable.setId("tab1");
			
			DatasetViewGenerator typeAxis = new DatasetViewGenerator();
			typeAxis.setType("author");
			typeAxis.setShowFileDownloads(Boolean.TRUE);
			typeAxis.setMax(maxRows);
			typeAxis.setIpRange(ipRanges);
			typeAxis.setOrderColumn(orderColumn);
			statisticsTable.addDatasetGenerator(typeAxis);
			
			if (startDate != null) {
				StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
				dateFilter.setStartDate(startDate);
				dateFilter.setEndDate(endDate);
				statisticsTable.addFilter(dateFilter);
			}
			
			Dataset dataset = statisticsTable.getDataset(context);
			
			dataset = statisticsTable.getDataset();
			
			if (dataset == null)
			{
				dataset = statisticsTable.getDataset(context);
			}
			
			if (dataset != null)
			{
				String[][] matrix = dataset.getMatrix();
				List<String> colLabels = dataset.getColLabels();
				List<String> rowLabels = dataset.getRowLabels();
				
				statsBean.setMatrix(matrix);
				statsBean.setColLabels(colLabels);
				statsBean.setRowLabels(rowLabels);
				
				statsBean.setName("Top Authors");
			}
		}
		catch (Exception e)
		{
			log.error(
				"Error occured while creating statistics for dso with ID: "
					+ dso.getID() + " and type " + dso.getType()
					+ " and handle: " + dso.getHandle(), e);
		}
    	return statsBean;
    }
    
    private StatisticsBean getNewItemsByCollection(Context context, DSpaceObject dso, Date startDate, Date endDate) {
    	StatisticsBean statsBean = new StatisticsBean();

		try
		{
			StatisticsListing statisticsTable = new StatisticsListing(new StatisticsDataArchive(dso));

			DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
			//dsoAxis.addDsoChild(type, max, separate, nameLength);
			statisticsTable.addDatasetGenerator(dsoAxis);
			
			if (startDate != null) {
				StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
				dateFilter.setStartDate(startDate);
				dateFilter.setEndDate(endDate);
				statisticsTable.addFilter(dateFilter);
			}
			
			Dataset dataset = statisticsTable.getDataset(context);
			
			dataset = statisticsTable.getDataset();
			
			if (dataset == null)
			{
				dataset = statisticsTable.getDataset(context);
			}
			
			if (dataset != null)
			{
				String[][] matrix = dataset.getMatrix();
				List<String> colLabels = dataset.getColLabels();
				List<String> rowLabels = dataset.getRowLabels();
				
				statsBean.setMatrix(matrix);
				statsBean.setColLabels(colLabels);
				statsBean.setRowLabels(rowLabels);
				
				statsBean.setName("New Items By Collection");
			}
		}
		catch (Exception e)
		{
			log.error(
				"Error occured while creating statistics for dso with ID: "
					+ dso.getID() + " and type " + dso.getType()
					+ " and handle: " + dso.getHandle(), e);
		}
    	return statsBean;
    }

    private StatisticsBean getNewItemsByType(Context context, DSpaceObject dso, Date startDate, Date endDate) {
    	StatisticsBean statsBean = new StatisticsBean();

		try
		{
			StatisticsListing statisticsTable = new StatisticsListing(new StatisticsDataAccessionedByType(dso, startDate, endDate));
			
			Dataset dataset = statisticsTable.getDataset(context);
			
			dataset = statisticsTable.getDataset();
			
			if (dataset == null)
			{
				dataset = statisticsTable.getDataset(context);
			}
			
			if (dataset != null)
			{
				String[][] matrix = dataset.getMatrix();
				List<String> colLabels = dataset.getColLabels();
				List<String> rowLabels = dataset.getRowLabels();
				
				statsBean.setMatrix(matrix);
				statsBean.setColLabels(colLabels);
				statsBean.setRowLabels(rowLabels);
				
				statsBean.setName("New Items By Type");
			}
		}
		catch (Exception e)
		{
			log.error(
				"Error occured while creating statistics for dso with ID: "
					+ dso.getID() + " and type " + dso.getType()
					+ " and handle: " + dso.getHandle(), e);
		}
    	return statsBean;
    }

    private StatisticsBean getItemCounts(Context context, DSpaceObject dso, Date endDate) {
    	StatisticsBean statsBean = new StatisticsBean();

		try
		{
			StatisticsListing statisticsTable = new StatisticsListing(new StatisticsDataItemCount(dso, endDate));
			
			Dataset dataset = statisticsTable.getDataset(context);
			
			dataset = statisticsTable.getDataset();
			
			if (dataset == null)
			{
				dataset = statisticsTable.getDataset(context);
			}
			
			if (dataset != null)
			{
				String[][] matrix = dataset.getMatrix();
				List<String> colLabels = dataset.getColLabels();
				List<String> rowLabels = dataset.getRowLabels();
				
				statsBean.setMatrix(matrix);
				statsBean.setColLabels(colLabels);
				statsBean.setRowLabels(rowLabels);
				
				statsBean.setName("Item Counts");
			}
		}
		catch (Exception e)
		{
			log.error(
				"Error occured while creating statistics for dso with ID: "
					+ dso.getID() + " and type " + dso.getType()
					+ " and handle: " + dso.getHandle(), e);
		}
    	return statsBean;
    }

}