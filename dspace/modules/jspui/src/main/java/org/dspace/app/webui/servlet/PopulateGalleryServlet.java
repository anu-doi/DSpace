/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;

import org.dspace.app.webui.components.GalleryItem;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.core.Context;
import org.dspace.browse.BrowserScope;
import org.dspace.browse.BrowseIndex;
import org.dspace.sort.SortOption;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseInfo;
import org.dspace.sort.SortException;
import org.dspace.browse.BrowseException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.LogManager;
import org.dspace.content.Item;
import org.dspace.content.Bundle;
import org.dspace.content.Bitstream;
import java.net.URLEncoder;


/**
 * Servlet to populate Browse results as gallery.
 * 
 * @author Osama Alkadi (osama.alkadi@anu.edu.au)
 * @since 1.8.2
 * 
 */
public class PopulateGalleryServlet extends DSpaceServlet
{
   
	private static final long serialVersionUID = 1L;
	/** log4j category */
    private static Logger log = Logger.getLogger(PopulateGalleryServlet.class);
    
	protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, SQLException, IOException
    {
		try
		{
	
			// First, get all the stuff out of the request that we might need
            String type = request.getParameter("type");
            String order = request.getParameter("order");
            String value = request.getParameter("value");
            String authority = request.getParameter("authority");
            int resultsperpage = UIUtil.getIntParameter(request, "rpp");
            int sortBy = UIUtil.getIntParameter(request, "sort_by");
            
            //additional ones, uncomment and use as required.
           // String valueLang = request.getParameter("value_lang");
           // String month = request.getParameter("month");
           // String year = request.getParameter("year");
           // String startsWith = request.getParameter("starts_with");
           // String valueFocus = request.getParameter("vfocus");
           // String valueFocusLang = request.getParameter("vfocus_lang");
            
           // int focus = UIUtil.getIntParameter(request, "focus");
            int offset = UIUtil.getIntParameter(request, "offset");
           
            int etAl = UIUtil.getIntParameter(request, "etal");

            // get the community or collection location for the browse request
            Collection collection = null;
            Community community = null;
            collection = UIUtil.getCollectionLocation(request);
            if (collection == null)
            {
                community = UIUtil.getCommunityLocation(request);
            }


            // process the input, performing some inline validation
            BrowseIndex bi = null;
            if (type != null && !"".equals(type))
            {
                bi = BrowseIndex.getBrowseIndex(type);
            }

            if (bi == null)
            {
                if (sortBy > 0)
                {
                    bi = BrowseIndex.getBrowseIndex(SortOption.getSortOption(sortBy));
                }
                else
                {
                    bi = BrowseIndex.getBrowseIndex(SortOption.getDefaultSortOption());
                }
            }

            // If we don't have a sort column
            if (bi != null && sortBy == -1)
            {
                // Get the default one
                SortOption so = bi.getSortOption();
                if (so != null)
                {
                    sortBy = so.getNumber();
                }
            }
            else if (bi != null && bi.isItemIndex() && !bi.isInternalIndex())
            {
                // If a default sort option is specified by the index, but it isn't
                // the same as sort option requested, attempt to find an index that
                // is configured to use that sort by default
                // This is so that we can then highlight the correct option in the navigation
                SortOption bso = bi.getSortOption();
                SortOption so = SortOption.getSortOption(sortBy);
                if ( bso != null && bso.equals(so))
                {
                    BrowseIndex newBi = BrowseIndex.getBrowseIndex(so);
                    if (newBi != null)
                    {
                        bi   = newBi;
                        type = bi.getName();
                    }
                }
            }

            if (order == null && bi != null)
            {
                order = bi.getDefaultOrder();
            }
            
            
            
            // performe some parameter validation
            
            // If the offset is invalid, reset to 0
            if (offset < 0)
            {
                offset = 0;
            }
            
            // if no resultsperpage set, default to 10
            if (resultsperpage < 0)
            {
                resultsperpage = 10;
            }
            
            // figure out the setting for author list truncation
            if (etAl == -1)     // there is no limit, or the UI says to use the default
            {
                int limitLine = 3; ConfigurationManager.getIntProperty("webui.browse.author-limit");
                if (limitLine != 0)
                {
                    etAl = limitLine;
                }
            }
            else  // if the user has set a limit
            {
                if (etAl == 0)  // 0 is the user setting for unlimited
                {
                    etAl = -1;  // but -1 is the application setting for unlimited
                }
            }
 
            // determine which level of the browse we are at: 0 for top, 1 for second
            int level = 0;
            if (value != null || authority != null)
            {
                level = 1;
            }
 
            // if sortBy is still not set, set it to 0, which is default to use the primary index value
            if (sortBy == -1)
            {
                sortBy = 0;
            }
            String pageHeader = "";
            
            // log the request
            String comHandle = "n/a";
            if (community != null)
            {
                comHandle = community.getHandle();
                pageHeader = "\"" + community.getMetadata("name") + "\"";
                
            }
            String colHandle = "n/a";
            if (collection != null)
            {
                colHandle = collection.getHandle();
                pageHeader = "\"" + collection.getMetadata("name") + "\"";
            }

            String arguments = "type=" + type + ",order=" + order + ",value=" + value +
            					",rpp=" + resultsperpage + ",sort_by=" + sortBy +
            					",community=" + comHandle + ",collection=" + colHandle + ",level=" + level + ",etal=" + etAl;

            log.info(LogManager.getHeader(context, "browse", arguments));
            
			// prep our engine and scope
			BrowseEngine be = new BrowseEngine(context);
			BrowserScope bs = new BrowserScope(context);
			//BrowseIndex bi = BrowseIndex.getItemBrowseIndex();

			// fill in the scope with the relevant param
			bs.setBrowseIndex(bi);
			bs.setOrder(order);
			bs.setResultsPerPage(resultsperpage);
			bs.setSortBy(sortBy);
			bs.setEtAl(etAl);
			bs.setOffset(offset);
	        bs.setFilterValue(value != null?value:authority);
	        bs.setBrowseLevel(level);
	        bs.setAuthorityValue(authority);

	        String linkBase = ConfigurationManager.getProperty("dspace.url")+ "/";
	        
			  // assign the scope of either Community or Collection if necessary
            if (community != null)
            {
                bs.setBrowseContainer(community);
                linkBase = linkBase + "handle/" + community.getHandle() + "/";
            }
            else if (collection != null)
            {
                bs.setBrowseContainer(collection);
                linkBase = linkBase + "handle/" + collection.getHandle() + "/";
            }
            //sort by type 
            for (SortOption so : SortOption.getSortOptions())
            {
                if (so.getName().equals(type))
                {
                    bs.setSortBy(so.getNumber());
                }
            }
			
            //limited browse
			BrowseInfo results = be.browse(bs);

			//store browse results in an array 
			Item[] items = results.getItemResults(context);

			List<GalleryItem> galleryItems = new ArrayList<GalleryItem>();
			
			 //go over each item
			for (int i = 0; i < items.length; i++)
    		{
				
				GalleryItem gi = new GalleryItem();
				
				String url;
				Item item = items[i];
				//print title
				log.debug("item: " + item.getName());
    			
				Bundle[] bundles = item.getBundles("BRANDED_PREVIEW");
				if (bundles != null && bundles.length > 0) {
					//only want the one branded preview 
	     			Bundle previewBundle = bundles[0];
	     			
	     			//only want the one bitstream in that bundle
	    			Bitstream previewBitstream = previewBundle.getBitstreams()[0];
	    			url = ConfigurationManager.getProperty("dspace.url") + "/retrieve/" + previewBitstream.getID() + "/" + previewBitstream.getName();
	    			log.debug("URL: " + url); 
	    			
	    			gi.setUrl(url);
	    			gi.setTitle(item.getName());
	    			gi.setHandle(ConfigurationManager.getProperty("dspace.url") + "/handle/"+ item.getHandle());
	    			galleryItems.add(gi);
				}
    		}

			int start = results.getStart();
			int finish = results.getFinish();
			int total = results.getTotal();
			
			
			 //String urlFragment = "browse/gallery";
			 String sharedLink = linkBase + "browse/gallery?";
			 String backLink = linkBase +"browse?";
			 
			 String queryParam = "type=" + type + "&amp;sort_by=" + sortBy + "&amp;order=" + order + "&amp;rpp=" + resultsperpage
				      + "&amp;etal=" + etAl;
			 
			if(bi.getName()!=null)
	
				
				sharedLink += queryParam;
				backLink += queryParam;
				
				String next = sharedLink;
				String prev = sharedLink;
				
				if (results.hasNextPage()){
					next = next + "&amp;offset=" + results.getNextOffset();
				}
				
				if (results.hasPrevPage())
			    {
			        prev = prev + "&amp;offset=" + results.getPrevOffset();
			    }

			// Don't create a previous page link if this is the first page
	      //  if (results.isFirst())
	       // {
	        //    return null;
	       // }

			request.setAttribute("galleryItems", galleryItems);
			request.setAttribute("type", type);
			request.setAttribute("pageHeader", pageHeader);
			request.setAttribute("next", next);
			request.setAttribute("prev", prev);
			request.setAttribute("back", backLink);
			request.setAttribute("start", start);
			request.setAttribute("finish", finish);
			request.setAttribute("total", total);
			JSPManager.showJSP(request, response, "/browse/gallery.jsp");
			return;
			
		}
		
        catch (SortException se)
        {
            log.error("caught exception: ", se);
            throw new ServletException (se);
        }
		catch (BrowseException ex)
    	{
			log.error("caught exception: ", ex);
    		throw new ServletException(ex);
    	}
		catch (ArrayIndexOutOfBoundsException e)
		{
			  JSPManager.showIntegrityError(request, response);
			System.out.println("There are no entries in the index");
			 System.out.println("Error: \n - " + e.getMessage());
            log.error("caught exception: ", e);

		}

    }

}
