/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.itemmarking.ItemMarkingExtractor;
import org.dspace.app.itemmarking.ItemMarkingInfo;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.browse.*;
import org.dspace.content.Bitstream;
import org.dspace.content.DCDate;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.Thumbnail;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.sort.SortOption;
import org.dspace.utils.DSpace;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.StringTokenizer;
import org.dspace.content.authority.MetadataAuthorityManager;

/**
 * Tag for display a list of items
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class BrowseListTag extends TagSupport
{
	 /** log4j category */
    private static Logger log = Logger.getLogger(BrowseListTag.class);

    /** Items to display */
    private transient BrowseItem[] items;

    /** Row to highlight, -1 for no row */
    private int highlightRow = -1;

    /** Column to emphasise, identified by metadata field */
    private String emphColumn;

    /** Config value of thumbnail view toggle */
    private static boolean showThumbs;

    /** Config browse/search width and height */
    private static int thumbItemListMaxWidth;

    private static int thumbItemListMaxHeight;

    /** Config browse/search thumbnail link behaviour */
    private static boolean linkToBitstream = false;

    /** Config to include an edit link */
    private boolean linkToEdit = false;

    /** Config to disable cross links */
    private boolean disableCrossLinks = false;

    /** The default fields to be displayed when listing items */
    private static final String DEFAULT_LIST_FIELDS;

    /** The default widths for the columns */
    private static final String DEFAULT_LIST_WIDTHS;

    /** The default field which is bound to the browse by date */
    private static String dateField = "dc.date.issued";

    /** The default field which is bound to the browse by title */
    private static String titleField = "dc.title";

    private static String authorField = "dc.contributor.*";

    private int authorLimit = -1;

    private transient BrowseInfo browseInfo;

    private static final long serialVersionUID = 8091584920304256107L;

    static
    {
        getThumbSettings();

        if (showThumbs)
        {
            DEFAULT_LIST_FIELDS = "thumbnail, dc.date.issued(date), dc.title, dc.contributor.*";
            DEFAULT_LIST_WIDTHS = "*, 130, 60%, 40%";
        }
        else
        {
            DEFAULT_LIST_FIELDS = "dc.date.issued(date), dc.title, dc.contributor.*";
            DEFAULT_LIST_WIDTHS = "130, 60%, 40%";
        }

        // get the date and title fields
        String dateLine = ConfigurationManager.getProperty("webui.browse.index.date");
        if (dateLine != null)
        {
            dateField = dateLine;
        }

        String titleLine = ConfigurationManager.getProperty("webui.browse.index.title");
        if (titleLine != null)
        {
            titleField = titleLine;
        }

        // get the author truncation config
        String authorLine = ConfigurationManager.getProperty("webui.browse.author-field");
        if (authorLine != null)
        {
        	authorField = authorLine;
        }
    }

    public BrowseListTag()
    {
        super();
    }

    public int doStartTag() throws JspException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest hrq = (HttpServletRequest) pageContext.getRequest();

        /* just leave this out now
        boolean emphasiseDate = false;
        boolean emphasiseTitle = false;

        if (emphColumn != null)
        {
            emphasiseDate = emphColumn.equalsIgnoreCase("date");
            emphasiseTitle = emphColumn.equalsIgnoreCase("title");
        }
        */

        // get the elements to display
        String browseListLine  = null;
        String browseWidthLine = null;

        // As different indexes / sort options may require different columns to be displayed
        // try to obtain a custom configuration based for the browse that has been performed
        if (browseInfo != null)
        {
            SortOption so = browseInfo.getSortOption();
            BrowseIndex bix = browseInfo.getBrowseIndex();

            // We have obtained the index that was used for this browse
            if (bix != null)
            {
                // First, try to get a configuration for this browse and sort option combined
                if (so != null && browseListLine == null)
                {
                    browseListLine  = ConfigurationManager.getProperty("webui.itemlist.browse." + bix.getName() + ".sort." + so.getName() + ".columns");
                    browseWidthLine = ConfigurationManager.getProperty("webui.itemlist.browse." + bix.getName() + ".sort." + so.getName() + ".widths");
                }

                // We haven't got a sort option defined, so get one for the index
                // - it may be required later
                if (so == null)
                {
                    so = bix.getSortOption();
                }
            }

            // If no config found, attempt to get one for this sort option
            if (so != null && browseListLine == null)
            {
                browseListLine  = ConfigurationManager.getProperty("webui.itemlist.sort." + so.getName() + ".columns");
                browseWidthLine = ConfigurationManager.getProperty("webui.itemlist.sort." + so.getName() + ".widths");
            }

            // If no config found, attempt to get one for this browse index
            if (bix != null && browseListLine == null)
            {
                browseListLine  = ConfigurationManager.getProperty("webui.itemlist.browse." + bix.getName() + ".columns");
                browseWidthLine = ConfigurationManager.getProperty("webui.itemlist.browse." + bix.getName() + ".widths");
            }

            // If no config found, attempt to get a general one, using the sort name
            if (so != null && browseListLine == null)
            {
                browseListLine  = ConfigurationManager.getProperty("webui.itemlist." + so.getName() + ".columns");
                browseWidthLine = ConfigurationManager.getProperty("webui.itemlist." + so.getName() + ".widths");
            }

            // If no config found, attempt to get a general one, using the index name
            if (bix != null && browseListLine == null)
            {
                browseListLine  = ConfigurationManager.getProperty("webui.itemlist." + bix.getName() + ".columns");
                browseWidthLine = ConfigurationManager.getProperty("webui.itemlist." + bix.getName() + ".widths");
            }
        }

        if (browseListLine == null)
        {
            browseListLine  = ConfigurationManager.getProperty("webui.itemlist.columns");
            browseWidthLine = ConfigurationManager.getProperty("webui.itemlist.widths");
        }

        // Have we read a field configration from dspace.cfg?
        if (browseListLine != null)
        {
            // If thumbnails are disabled, strip out any thumbnail column from the configuration
            if (!showThumbs && browseListLine.contains("thumbnail"))
            {
                // Ensure we haven't got any nulls
                browseListLine  = browseListLine  == null ? "" : browseListLine;
                browseWidthLine = browseWidthLine == null ? "" : browseWidthLine;

                // Tokenize the field and width lines
                StringTokenizer bllt = new StringTokenizer(browseListLine,  ",");
                StringTokenizer bwlt = new StringTokenizer(browseWidthLine, ",");

                StringBuilder newBLLine = new StringBuilder();
                StringBuilder newBWLine = new StringBuilder();
                while (bllt.hasMoreTokens() || bwlt.hasMoreTokens())
                {
                    String browseListTok  = bllt.hasMoreTokens() ? bllt.nextToken() : null;
                    String browseWidthTok = bwlt.hasMoreTokens() ? bwlt.nextToken() : null;

                    // Only use the Field and Width tokens, if the field isn't 'thumbnail'
                    if (browseListTok == null || !browseListTok.trim().equals("thumbnail"))
                    {
                        if (browseListTok != null)
                        {
                            if (newBLLine.length() > 0)
                            {
                                newBLLine.append(",");
                            }

                            newBLLine.append(browseListTok);
                        }

                        if (browseWidthTok != null)
                        {
                            if (newBWLine.length() > 0)
                            {
                                newBWLine.append(",");
                            }

                            newBWLine.append(browseWidthTok);
                        }
                    }
                }

                // Use the newly built configuration file
                browseListLine  = newBLLine.toString();
                browseWidthLine = newBWLine.toString();
            }
        }
        else
        {
            browseListLine  = DEFAULT_LIST_FIELDS;
            browseWidthLine = DEFAULT_LIST_WIDTHS;
        }

        // Arrays used to hold the information we will require when outputting each row
        String[] fieldArr  = browseListLine == null  ? new String[0] : browseListLine.split("\\s*,\\s*");
        String[] widthArr  = browseWidthLine == null ? new String[0] : browseWidthLine.split("\\s*,\\s*");
        boolean isDate[]   = new boolean[fieldArr.length];
        boolean emph[]     = new boolean[fieldArr.length];
        boolean isAuthor[] = new boolean[fieldArr.length];
        boolean viewFull[] = new boolean[fieldArr.length];
        String[] browseType = new String[fieldArr.length];
        String[] cOddOrEven = new String[fieldArr.length];

        try
        {
        	// Get the interlinking configuration too
            CrossLinks cl = new CrossLinks();

            for (int colIdx = 0; colIdx < fieldArr.length; colIdx++)
            {
            	String field = fieldArr[colIdx].toLowerCase().trim();

                // find out if the field is a date
                if (field.indexOf("(date)") > 0)
                {
                    field = field.replaceAll("\\(date\\)", "");
                    isDate[colIdx] = true;
                }
            	
            	// Cache any modifications to field
            	fieldArr[colIdx] = field;
            	
            	if (field.equals(authorField))
            	{
            		isAuthor[colIdx] = true;
            	}

            	// find out if this field needs to link out to other browse views
            	if (cl.hasLink(field))
            	{
            		browseType[colIdx] = cl.getLinkType(field);
            		viewFull[colIdx] = BrowseIndex.getBrowseIndex(browseType[colIdx]).isItemIndex();
            	}
            	
            	// find out if we are emphasising this field
            	if (field.equals(emphColumn))
            	{
            		emph[colIdx] = true;
            	}
            }
            
            for (int i = 0; i < items.length; i++)
            {
            	out.println("<div>");
            	out.println("<div class=\"divline-bold-uni margintop padbottom\"></div>");
            	boolean hasTitleField = false;
            	boolean hasThumbnailField = false;
            	String field = null;
            	for (int colIdx = 0; colIdx < fieldArr.length; colIdx++)
            	{
            		field = fieldArr[colIdx].toLowerCase().trim();
            		if (field.equals(titleField)) {
            			hasTitleField = true;
            		}
            		if (field.equals("thumbnail")) {
            			hasThumbnailField = true;
            		}
            	}
            	String metadata = null;
            	if (showThumbs)
            	{
	            	out.print("<div class=\"left padright padtop\">");
	            	if (hasThumbnailField) {
	            		metadata = getThumbMarkup(hrq, items[i]);
	            		out.print(metadata);
	            	}
	            	out.println("</div>");
            	}
            	if (hasTitleField) 
            	{
            		Metadatum[] metadataArray = items[i].getMetadata("dc", "title", Item.ANY, Item.ANY);
            		String title = "";
            		if (metadataArray.length > 0)
            		{
            			title = Utils.addEntities(metadataArray[0].value);
            		}
            		else
            		{
            			title = "<span style=\"font-style:italic\">("+ LocaleSupport.getLocalizedMessage(pageContext, "itemlist.title.undefined")+")</span>";
            		}
            		if (items[i].isWithdrawn())
            		{
            			metadata = "<span style=\"font-style:italic\">("+title+")</span>";
            		}
            		else
            		{
		                metadata = "<a class=\"nounderline\" href=\"" + hrq.getContextPath() + "/handle/" 
		                		+ items[i].getHandle() + "\">"
		                		+ title
		                		+ "</a>";
            		}
	                out.println("<h3 class='nopadtop margintop padbottom'>"+metadata+"</h3>");
            	}
            	out.println("<table class=\"tbl-row-bdr fullwidth noborder padtop\">");
            	for (int colIdx = 0; colIdx < fieldArr.length; colIdx++)
            	{
                	Metadatum[] metadataArray = null;
            		field = fieldArr[colIdx].toLowerCase().trim();
            		
            		if (!field.equals("thumbnail") && !field.equals(titleField))
            		{
            			out.print("<tr>");
                        String message = "itemlist." + field;
            			out.print("<th class=\"tbl25\">"+LocaleSupport.getLocalizedMessage(pageContext, message)+"</th>");
            			out.print("<td>");
            			
            			String[] tokens = {"", "", ""};
            			int k = 0;
            			StringTokenizer eq = new StringTokenizer(field, ".");
            			while (eq.hasMoreTokens())
            			{
            				tokens[k] = eq.nextToken().toLowerCase().trim();
            				k++;
            			}
            			String schema = tokens[0];
            			String element = tokens[1];
            			String qualifier = tokens[2];
            			
            			if (qualifier.equals("*"))
            			{
            				metadataArray = items[i].getMetadata(schema, element, Item.ANY, Item.ANY);
            			}
            			else if (qualifier.equals(""))
            			{
            				metadataArray = items[i].getMetadata(schema, element, null, Item.ANY);
            			}
            			else
            			{
            				metadataArray = items[i].getMetadata(schema, element, qualifier, Item.ANY);
            			}
            			
            			if (metadataArray == null) {
            				metadataArray = new Metadatum[0];
            			}
            			
            			metadata = "-";
            			if (metadataArray.length > 0)
            			{

                            if (isDate[colIdx])
                            {
                                DCDate dd = new DCDate(metadataArray[0].value);
                                metadata = UIUtil.displayDate(dd, false, false, hrq);
                            }
                            else {
	            				int loopLimit = metadataArray.length;
	            				boolean truncated = false;
	            				//TODO author limits
	            				if (isAuthor[colIdx])
	            				{
	            					int fieldMax = (authorLimit > 0 ? authorLimit : metadataArray.length);
	            					loopLimit = (fieldMax > metadataArray.length ? metadataArray.length : fieldMax);
	            					truncated = (fieldMax < metadataArray.length);
	            				    log.debug("Limiting output of field " + field + " to " + Integer.toString(loopLimit) + " from an original " + Integer.toString(metadataArray.length));
	            				}
	                			StringBuilder sb = new StringBuilder();
	            				for (int j = 0; j < loopLimit; j++)
	            				{
	            					String startLink = "";
	            					String endLink = "";
	            					if (!StringUtils.isEmpty(browseType[colIdx]) && !disableCrossLinks)
	            					{
	                                    String argument;
	                                    String value;
	                                    if (metadataArray[j].authority != null &&
	                                            metadataArray[j].confidence >= MetadataAuthorityManager.getManager()
	                                                .getMinConfidence(metadataArray[j].schema, metadataArray[j].element, metadataArray[j].qualifier))
	                                    {
	                                        argument = "authority";
	                                        value = metadataArray[j].authority;
	                                    }
	                                    else
	                                    {
	                                        argument = "value";
	                                        value = metadataArray[j].value;
	                                    }
	                                    if (viewFull[colIdx])
	                                    {
	                                        argument = "vfocus";
	                                    }
	                                    startLink = "<a href=\"" + hrq.getContextPath() + "/browse?type=" + browseType[colIdx] + "&amp;" +
	                                        argument + "=" + URLEncoder.encode(value,"UTF-8");

	                                    if (metadataArray[j].language != null)
	                                    {
	                                        startLink = startLink + "&amp;" +
	                                            argument + "_lang=" + URLEncoder.encode(metadataArray[j].language, "UTF-8");
	                                    }

	                                    if ("authority".equals(argument))
	                                    {
	                                        startLink += "\" class=\"authority " +browseType[colIdx] + "\">";
	                                    }
	                                    else
	                                    {
	                                        startLink = startLink + "\">";
	                                    }
	                                    endLink = "</a>";
	            					}
	            					sb.append(startLink);
	            					sb.append(Utils.addEntities(metadataArray[j].value));
	            					sb.append(endLink);
	            					if (j < (loopLimit - 1))
	            					{
	            						sb.append("; ");
	            					}
	            				}
	            				if (truncated)
	            				{
	            					String etal = LocaleSupport.getLocalizedMessage(pageContext, "itemlist.et-al");
	            					sb.append(", ").append(etal);
	            				}
	            				metadata = sb.toString();
                            }
            			}
            			out.print(metadata);
            			out.print("</td>");
            			out.println("</tr>");
            		}
            	}
            	out.println("</table>");
            	
            	out.println("</div>");
            }
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        } catch (BrowseException e)
        {
        	throw new JspException(e);
        }

        return SKIP_BODY;
    }

    public BrowseInfo getBrowseInfo()
    {
    	return browseInfo;
    }

    public void setBrowseInfo(BrowseInfo browseInfo)
    {
    	this.browseInfo = browseInfo;
    	setItems(browseInfo.getBrowseItemResults());
    	authorLimit = browseInfo.getEtAl();
    }

    public boolean getLinkToEdit()
    {
        return linkToEdit;
    }

    public void setLinkToEdit(boolean edit)
    {
        this.linkToEdit = edit;
    }

    public boolean getDisableCrossLinks()
    {
        return disableCrossLinks;
    }

    public void setDisableCrossLinks(boolean links)
    {
        this.disableCrossLinks = links;
    }

    /**
     * Get the items to list
     *
     * @return the items
     */
    public BrowseItem[] getItems()
    {
        return (BrowseItem[]) ArrayUtils.clone(items);
    }

    /**
     * Set the items to list
     *
     * @param itemsIn
     *            the items
     */
    public void setItems(BrowseItem[] itemsIn)
    {
        items = (BrowseItem[]) ArrayUtils.clone(itemsIn);
    }

    /**
     * Get the row to highlight - null or -1 for no row
     *
     * @return the row to highlight
     */
    public String getHighlightrow()
    {
        return String.valueOf(highlightRow);
    }

    /**
     * Set the row to highlight
     *
     * @param highlightRowIn
     *            the row to highlight or -1 for no highlight
     */
    public void setHighlightrow(String highlightRowIn)
    {
        if ((highlightRowIn == null) || highlightRowIn.equals(""))
        {
            highlightRow = -1;
        }
        else
        {
            try
            {
                highlightRow = Integer.parseInt(highlightRowIn);
            }
            catch (NumberFormatException nfe)
            {
                highlightRow = -1;
            }
        }
    }

    /**
     * Get the column to emphasise - "title", "date" or null
     *
     * @return the column to emphasise
     */
    public String getEmphcolumn()
    {
        return emphColumn;
    }

    /**
     * Set the column to emphasise - "title", "date" or null
     *
     * @param emphColumnIn
     *            column to emphasise
     */
    public void setEmphcolumn(String emphColumnIn)
    {
        emphColumn = emphColumnIn;
    }

    public void release()
    {
        highlightRow = -1;
        emphColumn = null;
        items = null;
    }

    /* get the required thumbnail config items */
    private static void getThumbSettings()
    {
        showThumbs = ConfigurationManager
                .getBooleanProperty("webui.browse.thumbnail.show");

        if (showThumbs)
        {
            thumbItemListMaxHeight = ConfigurationManager
                    .getIntProperty("webui.browse.thumbnail.maxheight");

            if (thumbItemListMaxHeight == 0)
            {
                thumbItemListMaxHeight = ConfigurationManager
                        .getIntProperty("thumbnail.maxheight");
            }

            thumbItemListMaxWidth = ConfigurationManager
                    .getIntProperty("webui.browse.thumbnail.maxwidth");

            if (thumbItemListMaxWidth == 0)
            {
                thumbItemListMaxWidth = ConfigurationManager
                        .getIntProperty("thumbnail.maxwidth");
            }
        }

        String linkBehaviour = ConfigurationManager
                .getProperty("webui.browse.thumbnail.linkbehaviour");

        if (linkBehaviour != null && linkBehaviour.equals("bitstream"))
        {
            linkToBitstream = true;
        }
    }

    /*
     * Get the (X)HTML width and height attributes. As the browser is being used
     * for scaling, we only scale down otherwise we'll get hideously chunky
     * images. This means the media filter should be run with the maxheight and
     * maxwidth set greater than or equal to the size of the images required in
     * the search/browse
     */
    private String getScalingAttr(HttpServletRequest hrq, Bitstream bitstream)
            throws JspException
    {
        BufferedImage buf;

        try
        {
            Context c = UIUtil.obtainContext(hrq);

            InputStream is = BitstreamStorageManager.retrieve(c, bitstream
                    .getID());

            //AuthorizeManager.authorizeAction(bContext, this, Constants.READ);
            // 	read in bitstream's image
            buf = ImageIO.read(is);
            is.close();
        }
        catch (SQLException sqle)
        {
            throw new JspException(sqle.getMessage(), sqle);
        }
        catch (IOException ioe)
        {
            throw new JspException(ioe.getMessage(), ioe);
        }

        // now get the image dimensions
        float xsize = (float) buf.getWidth(null);
        float ysize = (float) buf.getHeight(null);

        // scale by x first if needed
        if (xsize > (float) thumbItemListMaxWidth)
        {
            // calculate scaling factor so that xsize * scale = new size (max)
            float scaleFactor = (float) thumbItemListMaxWidth / xsize;

            // now reduce x size and y size
            xsize = xsize * scaleFactor;
            ysize = ysize * scaleFactor;
        }

        // scale by y if needed
        if (ysize > (float) thumbItemListMaxHeight)
        {
            float scaleFactor = (float) thumbItemListMaxHeight / ysize;

            // now reduce x size
            // and y size
            xsize = xsize * scaleFactor;
            ysize = ysize * scaleFactor;
        }

        StringBuffer sb = new StringBuffer("width=\"").append(xsize).append(
                "\" height=\"").append(ysize).append("\"");

        return sb.toString();
    }

    /* generate the (X)HTML required to show the thumbnail */
    private String getThumbMarkup(HttpServletRequest hrq, BrowseItem item)
            throws JspException
    {
    	try
    	{
            Context c = UIUtil.obtainContext(hrq);
            Thumbnail thumbnail = ItemService.getThumbnail(c, item.getID(), linkToBitstream);

            if (thumbnail == null)
    		{
    			return "";
    		}
        	StringBuffer thumbFrag = new StringBuffer();

        	if (linkToBitstream)
        	{
        		Bitstream original = thumbnail.getOriginal();
        		String link = hrq.getContextPath() + "/bitstream/" + item.getHandle() + "/" + original.getSequenceID() + "/" +
        						UIUtil.encodeBitstreamName(original.getName(), Constants.DEFAULT_ENCODING);
        		thumbFrag.append("<a target=\"_blank\" href=\"" + link + "\" />");
        	}
        	else
        	{
        		String link = hrq.getContextPath() + "/handle/" + item.getHandle();
        		thumbFrag.append("<a href=\"" + link + "\" />");
        	}

        	Bitstream thumb = thumbnail.getThumb();
        	String img = hrq.getContextPath() + "/retrieve/" + thumb.getID() + "/" +
        				UIUtil.encodeBitstreamName(thumb.getName(), Constants.DEFAULT_ENCODING);
        	String alt = thumb.getName();
            String scAttr = getScalingAttr(hrq, thumb);
            thumbFrag.append("<img src=\"")
                    .append(img)
                    .append("\" alt=\"").append(alt).append("\" ")
                     .append(scAttr)
                     .append("/ class=\"bdr-solid bdr-grey\"></a>");

        	return thumbFrag.toString();
        }
        catch (SQLException sqle)
        {
        	throw new JspException(sqle.getMessage(), sqle);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new JspException("Server does not support DSpace's default encoding. ", e);
        }
    }
}
