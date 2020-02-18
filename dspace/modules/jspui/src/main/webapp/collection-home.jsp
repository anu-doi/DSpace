<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Collection home JSP
  -
  - Attributes required:
  -    collection  - Collection to render home page for
  -    community   - Community this collection is in
  -    last.submitted.titles - String[], titles of recent submissions
  -    last.submitted.urls   - String[], corresponding URLs
  -    logged.in  - Boolean, true if a user is logged in
  -    subscribed - Boolean, true if user is subscribed to this collection
  -    admin_button - Boolean, show admin 'edit' button
  -    editor_button - Boolean, show collection editor (edit submitters, item mapping) buttons
  -    show.items - Boolean, show item list
  -    browse.info - BrowseInfo, item list
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://www.anu.edu.au/taglib" prefix="anu" %>

<%@ page import="org.dspace.app.webui.components.RecentSubmissions" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="org.dspace.browse.ItemCounter"%>
<%@ page import="org.dspace.content.*"%>
<%@ page import="org.dspace.core.ConfigurationManager"%>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.eperson.Group"     %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="java.net.URLEncoder" %>


<%
    // Retrieve attributes
    Collection collection = (Collection) request.getAttribute("collection");
    Community  community  = (Community) request.getAttribute("community");
    Group      submitters = (Group) request.getAttribute("submitters");

    RecentSubmissions rs = (RecentSubmissions) request.getAttribute("recently.submitted");
    
    boolean loggedIn =
        ((Boolean) request.getAttribute("logged.in")).booleanValue();
    boolean subscribed =
        ((Boolean) request.getAttribute("subscribed")).booleanValue();
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());

    Boolean editor_b      = (Boolean)request.getAttribute("editor_button");
    boolean editor_button = (editor_b == null ? false : editor_b.booleanValue());

    Boolean submit_b      = (Boolean)request.getAttribute("can_submit_button");
    boolean submit_button = (submit_b == null ? false : submit_b.booleanValue());

	// get the browse indices
    BrowseIndex[] bis = BrowseIndex.getBrowseIndices();

    // Put the metadata values into guaranteed non-null variables
    String name = collection.getMetadata("name");
    String intro = collection.getMetadata("introductory_text");
    if (intro == null)
    {
        intro = "";
    }
    String copyright = collection.getMetadata("copyright_text");
    if (copyright == null)
    {
        copyright = "";
    }
    String sidebar = collection.getMetadata("side_bar_text");
    if(sidebar == null)
    {
        sidebar = "";
    }

    String communityName = community.getMetadata("name");
    String communityLink = "/handle/" + community.getHandle();

    Bitstream logo = collection.getLogo();
    
    boolean feedEnabled = ConfigurationManager.getBooleanProperty("webui.feed.enable");
    String feedData = "NONE";
    if (feedEnabled)
    {
        feedData = "coll:" + ConfigurationManager.getProperty("webui.feed.formats");
    }
    
    boolean galleryEnabled = false;
    String colls = ConfigurationManager.getProperty("gallery.show.collections");
    if (colls != null && colls.length() > 0 && colls.contains(collection.getHandle())) {
    	galleryEnabled = true;
    }
    
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));

    Boolean showItems = (Boolean)request.getAttribute("show.items");
    boolean show_items = showItems != null ? showItems.booleanValue() : false;
%>

<%@page import="org.dspace.app.webui.servlet.MyDSpaceServlet"%>
<dspace:layout locbar="commLink" title="<%= name %>" feedData="<%= feedData %>">
<anu:content layout="doublewide">
    <div class="well">
    <div class="row"><div><h1><%= name %>
<%
            if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
            {
%>
                : [<%= ic.getCount(collection) %>]
<%
            }
%>
	</h1>
      </div>
<%  if (logo != null) { %>
        <div class="marginbottom">
        	<img class="img-responsive" alt="Logo" src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" />
        </div>
<% 	} %>
	</div>
	

<div class="panel panel-primary padbottom">
	<form class="anuform" method="get" action="<%= request.getContextPath() %>/advanced-search">
	<fieldset>
	<legend>Search <%= collection.getName() %></legend>
	<div class="panel-body">
		<input type="text" class="margintop marginbottom text tfull"  placeholder="" name="query" id="collectionquery" size="25" />
		<input type="hidden" name="location" value="<%= collection.getHandle() %>" />
		<input class="btn-uni-grad btn-small" type="submit" id="main-query-submit" value="Search">
	</div>
	</fieldset>
	</form>
	<%
		if (bis.length > 0)
		{
	%>
	<form class="anuform" method="get" action="<%= request.getContextPath() %>/handle/<%= collection.getHandle() %>/browse">
		<fieldset>
		<legend>Browse <%= collection.getName() %></legend>
		<div class="panel-body margintop marginbottom">
			<strong>Browse by:</strong>
			<select name="type">
				<%
				for (int i = 0; i < bis.length; i++)
				{
					String key = "browse.menu." + bis[i].getName();
				%>
					<option value="<%= bis[i].getName() %>"><fmt:message key="<%= key %>" /></option>
				<%
				}
				%>
			</select>
			<input type="submit" class="btn-uni-grad btn-small" name="submit_browse" value="Search"/>
		</div>
		</fieldset>
	</form>
	<%
		}
	%>
</div>

<%
	if (StringUtils.isNotBlank(intro)) { %>
	<%= intro %>
<% 	} %>
  </div>
  
  <p class="copyrightText marginbottom"><%= copyright %></p>
  

<% if (show_items)
   {
        BrowseInfo bi = (BrowseInfo) request.getAttribute("browse.info");
        BrowseIndex bix = bi.getBrowseIndex();

        // prepare the next and previous links
        String linkBase = request.getContextPath() + "/handle/" + collection.getHandle();
        
        String next = linkBase;
        String prev = linkBase;
        
        if (bi.hasNextPage())
        {
            next = next + "?offset=" + bi.getNextOffset();
        }
        
        if (bi.hasPrevPage())
        {
            prev = prev + "?offset=" + bi.getPrevOffset();
        }

        String bi_name_key = "browse.menu." + bi.getSortOption().getName();
        String so_name_key = "browse.order." + (bi.isAscending() ? "asc" : "desc");
%>
    <%-- give us the top report on what we are looking at --%>
    <fmt:message var="bi_name" key="<%= bi_name_key %>"/>
    <fmt:message var="so_name" key="<%= so_name_key %>"/>
    <div class="browse_range">
        <fmt:message key="jsp.collection-home.content.range">
            <fmt:param value="${bi_name}"/>
            <fmt:param value="${so_name}"/>
            <fmt:param value="<%= Integer.toString(bi.getStart()) %>"/>
            <fmt:param value="<%= Integer.toString(bi.getFinish()) %>"/>
            <fmt:param value="<%= Integer.toString(bi.getTotal()) %>"/>
        </fmt:message>
    </div>

<%-- output the results using the browselist tag --%>
<%
      if (bix.isMetadataIndex())
      {
%>
      <dspace:browselist browseInfo="<%= bi %>" emphcolumn="<%= bix.getMetadata() %>" />
<%
      }
      else
      {
%>
      <dspace:browselist browseInfo="<%= bi %>" emphcolumn="<%= bix.getSortOption().getMetadata() %>" />
<%
      }
%>

    <%--  do the bottom previous and next page links --%>
    <div class="discovery-result-pagination row container">
	<ul class="pagination pull-right">
<% 
      if (bi.hasPrevPage())
      {
%>
	<li>
      <a href="<%= prev %>"><fmt:message key="browse.full.prev"/></a>
	  </li>
<%
      }
	  else {
%>
	<li class="disabled">
      <a href="<%= prev %>"><fmt:message key="browse.full.prev"/></a>
	  </li>
<%
	  }

      if (bi.hasNextPage())
      {
%>
<li><a href="<%= next %>"><fmt:message key="browse.full.next"/></a>
	  </li>
<%
      }
	  else {
%>
<li class="disabled"><a href="<%= next %>"><fmt:message key="browse.full.next"/></a>
	  </li>
<%
	  }
%>
	</ul>
    </div>

<%
   } // end of if (show_title)
%>

  <div>
  
    <%
    	int discovery_panel_cols = 12;
    	int discovery_facet_cols = 12;
    %>
    <%@ include file="discovery/static-sidebar-facet.jsp" %>
  </div>

</anu:content>
<anu:content layout="narrow">
<% if(admin_button || editor_button ) { %>
<fmt:message key="jsp.admintools" var="admintools"/>
<anu:boxheader text="${admintools}" />
<anu:box style="solid">           
<% if( editor_button ) { %>
                <form method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
                  <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                  <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                  <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_EDIT_COLLECTION %>" />
                  <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.general.edit.button"/>" />
                </form>
<% } %>

<% if( admin_button ) { %>
                 <form method="post" action="<%=request.getContextPath()%>/tools/itemmap">
                  <input type="hidden" name="cid" value="<%= collection.getID() %>" />
				  <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.collection-home.item.button"/>" />                  
                </form>
<% if(submitters != null) { %>
		      <form method="get" action="<%=request.getContextPath()%>/tools/group-edit">
		        <input type="hidden" name="group_id" value="<%=submitters.getID()%>" />
		        <input class="btn btn-default col-md-12" type="submit" name="submit_edit" value="<fmt:message key="jsp.collection-home.editsub.button"/>" />
		      </form>
<% } %>
<% if( editor_button || admin_button) { %>
                <form method="post" action="<%=request.getContextPath()%>/mydspace">
                  <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                  <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_EXPORT_ARCHIVE %>" />
                  <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.mydspace.request.export.collection"/>" />
                </form>
               <form method="post" action="<%=request.getContextPath()%>/mydspace">
                 <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                 <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_MIGRATE_ARCHIVE %>" />
                 <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.mydspace.request.export.migratecollection"/>" />
               </form>
               <form method="post" action="<%=request.getContextPath()%>/metadataexport">
                 <input type="hidden" name="handle" value="<%= collection.getHandle() %>" />
                 <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.general.metadataexport.button"/>" />
               </form>
<% } %>
                 
<% } %>
</anu:box>
<%  } %>
<%  if (submit_button)
    { %>
<anu:boxheader text="Submit" />
<anu:box style="solid">
          <form class="form-group" action="<%= request.getContextPath() %>/submit" method="post">
            <input type="hidden" name="collection" value="<%= collection.getID() %>" />
			<input class="btn btn-success col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.collection-home.submit.button"/>" />
          </form>
</anu:box>
<%  } %>
<div class="box-header bg-white bdr-white">
<%
if(feedEnabled)
{
%>
<span class="right"><a href="<%= request.getContextPath() %>/feed/atom_1.0/<%= community.getHandle() %>">
<img src="//style.anu.edu.au/_anu/images/share/rss.png" class="w16px" alt="RSS feed" /></a></span>
<%
}
%>
<span><fmt:message key="jsp.collection-home.recentsub"/></span>
</div>
<anu:box style="solid">
<%
	if (rs != null)
	{
		Item[] items = rs.getRecentSubmissions();
		for (int i = 0; i < items.length; i++)
		{
			Metadatum[] dcv = items[i].getMetadata("dc", "title", null, Item.ANY);
			String displayTitle = "Untitled";
			if (dcv != null)
			{
				if (dcv.length > 0)
				{
					displayTitle = dcv[0].value;
				}
			}
			%><p class="recentItem"><a class="nounderline" href="<%= request.getContextPath() %>/handle/<%= items[i].getHandle() %>"><%= displayTitle %></a></p><%
		}
%>
<%      } %>
</anu:box>


<%
	if (galleryEnabled)
	{
%>
	<p class="center"><a class="btn btn-info" href="<%= request.getContextPath() %>/handle/<%= collection.getHandle() %>/browse/gallery"><fmt:message key="jsp.collection-home.view-as-gallery"/></a></p>
<%
	}
%>

<anu:box backgroundColour="black">
	<p class="center nopadbottom nopadtop"><a class="nounderline" href="<%= request.getContextPath() %>/handle/<%= collection.getHandle() %>/statistics"><span class="large"><fmt:message key="jsp.community-home.display-statistics"/></span></a></p>
</anu:box>
      
    <%= sidebar %>
</anu:content>
</dspace:layout>
