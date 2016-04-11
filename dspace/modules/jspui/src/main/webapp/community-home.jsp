<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Community home JSP
  -
  - Attributes required:
  -    community             - Community to render home page for
  -    collections           - array of Collections in this community
  -    subcommunities        - array of Sub-communities in this community
  -    last.submitted.titles - String[] of titles of recently submitted items
  -    last.submitted.urls   - String[] of URLs of recently submitted items
  -    admin_button - Boolean, show admin 'edit' button
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://www.anu.edu.au/taglib" prefix="anu" %>

<%@ page import="org.dspace.app.webui.components.RecentSubmissions" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.ItemCounter" %>
<%@ page import="org.dspace.content.*" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="org.apache.commons.lang.StringUtils"%>

<%
    // Retrieve attributes
    Community community = (Community) request.getAttribute( "community" );
    Collection[] collections =
        (Collection[]) request.getAttribute("collections");
    Community[] subcommunities =
        (Community[]) request.getAttribute("subcommunities");
    
    RecentSubmissions rs = (RecentSubmissions) request.getAttribute("recently.submitted");
    
    Boolean editor_b = (Boolean)request.getAttribute("editor_button");
    boolean editor_button = (editor_b == null ? false : editor_b.booleanValue());
    Boolean add_b = (Boolean)request.getAttribute("add_button");
    boolean add_button = (add_b == null ? false : add_b.booleanValue());
    Boolean remove_b = (Boolean)request.getAttribute("remove_button");
    boolean remove_button = (remove_b == null ? false : remove_b.booleanValue());

	// get the browse indices
    BrowseIndex[] bis = BrowseIndex.getBrowseIndices();

    // Put the metadata values into guaranteed non-null variables
    String name = community.getMetadata("name");
    String intro = community.getMetadata("introductory_text");
    String copyright = community.getMetadata("copyright_text");
    String sidebar = community.getMetadata("side_bar_text");
    Bitstream logo = community.getLogo();
    
    boolean feedEnabled = ConfigurationManager.getBooleanProperty("webui.feed.enable");
    String feedData = "NONE";
    if (feedEnabled)
    {
        feedData = "comm:" + ConfigurationManager.getProperty("webui.feed.formats");
    }
    
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));
%>

<%@page import="org.dspace.app.webui.servlet.MyDSpaceServlet"%>
<dspace:layout locbar="commLink" title="<%= name %>" feedData="<%= feedData %>">
<anu:content layout="doublewide">
<div class="well">
<div class="row">
	<div>
        <h1><%= name %>
        <%
            if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
            {
%>
                : [<%= ic.getCount(community) %>]
<%
            }
%>
        </h1>
	</div>
<%  if (logo != null) { %>
     <div class="marginbottom">
     	<img class="img-responsive" alt="Logo" src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" />
     </div> 
<% } %>
 </div>
 

<div class="panel panel-primary padbottom">
	<form class="anuform" method="get" action="<%= request.getContextPath() %>/advanced-search">
	<fieldset>
	<legend>Search <%= community.getName() %></legend>
	<div class="panel-body">
		<input type="text" class="margintop marginbottom text tfull"  placeholder="" name="query" id="collectionquery" size="25" />
		<input type="hidden" name="location" value="<%= community.getHandle() %>" />
		<input class="btn-uni-grad btn-small" type="submit" id="main-query-submit" value="Search">
	</div>
	</fieldset>
	</form>
	<%
		if (bis.length > 0)
		{
	%>
	<form class="anuform" method="get" action="<%= request.getContextPath() %>/handle/<%= community.getHandle() %>/browse">
		<fieldset>
		<legend>Browse <%= community.getName() %></legend>
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

<% if (StringUtils.isNotBlank(intro)) { %>
  <%= intro %>
<% } %>
</div>
<p class="copyrightText"><%= copyright %></p>
	<% if (sidebar != null && !"".equals(sidebar)) { %>
	<div class="row">
	<div class="col-md-4 marginbottom">
    	<%= sidebar %>
	</div>
</div>	
	<% } %>

<%!
	public void listCommunities(Community[] communities,  JspWriter out, HttpServletRequest request, ItemCounter ic, boolean topLevel, boolean removeButton) throws IOException, SQLException
	{
		if (communities.length > 0)
		{
			if (!topLevel) {
				out.println("<ul class=\"linklist\">");
			}
			for (Community comm : communities) {
				
				
				if (topLevel) {
						out.println("<h3 class=\"nounderline margintop\">");
					out.println("<a href=\""+request.getContextPath()+"/handle/"+comm.getHandle()+"\">"+comm.getName()+"</a>");
					if (ConfigurationManager.getBooleanProperty("webui.strengths.show")) {
						try {
							out.println("<span class=\"badge\">"+ic.getCount(comm)+"</span>");
						}
						catch (Exception e) {
							System.out.println("Exception getting item count for collection: "+comm.getID());
						}
					}
					if (removeButton) {
						out.println("<form class=\"btn-group\" method=\"post\" action=\""+request.getContextPath()+"/tools/edit-communities\">");
						out.println("<input type=\"hidden\" name=\"parent_community_id\" value=\""+comm.getParentCommunity()+"\" />");
						out.println("<input type=\"hidden\" name=\"community_id\" value=\""+comm.getID()+"\" />");
						out.println("<input type=\"hidden\" name=\"action\" value=\""+EditCommunitiesServlet.START_DELETE_COMMUNITY+"\" />");
						out.println("<button type=\"submit\" class=\"btn btn-xs btn-danger\"><span class=\"glyphicon glyphicon-trash\"></span></button>");
						out.println("</form>");
					}
						out.println("</h3>");
				}
				else {
					out.println("<li>");
					out.println("<a href=\""+request.getContextPath()+"/handle/"+comm.getHandle()+"\">"+comm.getName()+"</a>");
				
					if (removeButton) {
						out.println("<form class=\"btn-group\" method=\"post\" action=\""+request.getContextPath()+"/tools/edit-communities\">");
						out.println("<input type=\"hidden\" name=\"parent_community_id\" value=\""+comm.getParentCommunity()+"\" />");
						out.println("<input type=\"hidden\" name=\"community_id\" value=\""+comm.getID()+"\" />");
						out.println("<input type=\"hidden\" name=\"action\" value=\""+EditCommunitiesServlet.START_DELETE_COMMUNITY+"\" />");
						out.println("<button type=\"submit\" class=\"btn btn-xs btn-danger\"><span class=\"glyphicon glyphicon-trash\"></span></button>");
						out.println("</form>");
					}
				}
				listCommunities(comm.getSubcommunities(), out, request, ic, false, removeButton);
				listCollections(comm, comm.getCollections(), out, request, ic, removeButton);
				if (!topLevel) {
					out.println("</li>");
				}
			}
			if (!topLevel) {
				out.println("</ul>");
			}
		}
	}
	
	public void listCollections(Community parentCommunity, Collection[] collections, JspWriter out, HttpServletRequest request, ItemCounter ic, boolean removeButton) throws IOException, SQLException
	{
		if (collections.length > 0)
		{
			out.println("<ul class=\"linklist\">");
			for (Collection coll : collections) {
				out.println("<li>");
				out.println("<a href=\""+request.getContextPath()+"/handle/"+coll.getHandle()+"\">"+coll.getName()+"</a>");
				if (ConfigurationManager.getBooleanProperty("webui.strengths.show")) {
					try {
						out.println("["+ic.getCount(coll)+"]");
					}
					catch (Exception e) {
						System.out.println("Exception getting item count for collection: "+coll.getID());
					}
				}
				if (removeButton) {
					out.println("<form class=\"btn-group\" method=\"post\" action=\""+request.getContextPath()+"/tools/edit-communities\">");
					out.println("<input type=\"hidden\" name=\"parent_community_id\" value=\""+parentCommunity.getID()+"\" />");
					out.println("<input type=\"hidden\" name=\"community_id\" value=\""+parentCommunity.getID()+"\" />");
					out.println("<input type=\"hidden\" name=\"collection_id\" value=\""+coll.getID()+"\" />");
					out.println("<input type=\"hidden\" name=\"action\" value=\""+EditCommunitiesServlet.START_DELETE_COLLECTION+"\" />");
					out.println("<button type=\"submit\" class=\"btn btn-xs btn-danger\"><span class=\"glyphicon glyphicon-trash\"></span></button>");
					out.println("</form>");
				}
				out.println("</li>");
			}
			out.println("</ul>");
		}
	}
	
%>

<div>
<h2>Communities & Collections</h2>
<% 
    if (subcommunities.length != 0)
	{
%>

<%
	listCommunities(subcommunities, out, request, ic, true, remove_button);
%>
<% } 
	if (collections.length > 0) {
		listCollections(community, collections, out, request, ic, remove_button);
	}

%>
</div>

<div class="row">

    <%
    	int discovery_panel_cols = 12;
    	int discovery_facet_cols = 4;
    %>
	<%@ include file="discovery/static-sidebar-facet.jsp" %>
</div>
</anu:content>
<anu:content layout="narrow">
    <% if(editor_button || add_button)  // edit button(s)
    { %>
<fmt:message key="jsp.admintools" var="admintools"/>
<anu:boxheader text="${admintools}" />
<anu:box style="solid">
             <% if(editor_button) { %>
	            <form method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
		          <input type="hidden" name="community_id" value="<%= community.getID() %>" />
		          <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_EDIT_COMMUNITY%>" />
                  <%--<input type="submit" value="Edit..." />--%>
                  <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.general.edit.button"/>" />
                </form>
             <% } %>
             <% if(add_button) { %>

				<form method="post" action="<%=request.getContextPath()%>/tools/collection-wizard">
		     		<input type="hidden" name="community_id" value="<%= community.getID() %>" />
                    <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.community-home.create1.button"/>" />
                </form>
                
                <form method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
                    <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_CREATE_COMMUNITY%>" />
                    <input type="hidden" name="parent_community_id" value="<%= community.getID() %>" />
                    <%--<input type="submit" name="submit" value="Create Sub-community" />--%>
                    <input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.community-home.create2.button"/>" />
                 </form>
             <% } %>
            <% if( editor_button ) { %>
                <form method="post" action="<%=request.getContextPath()%>/mydspace">
                  <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                  <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_EXPORT_ARCHIVE %>" />
                  <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.mydspace.request.export.community"/>" />
                </form>
              <form method="post" action="<%=request.getContextPath()%>/mydspace">
                <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_MIGRATE_ARCHIVE %>" />
                <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.mydspace.request.export.migratecommunity"/>" />
              </form>
               <form method="post" action="<%=request.getContextPath()%>/dspace-admin/metadataexport">
                 <input type="hidden" name="handle" value="<%= community.getHandle() %>" />
                 <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.general.metadataexport.button"/>" />
               </form>
			<% } %>
</anu:box>
    <% } %>


<%
	if(rs != null) {
%>
	<div class="box-header bg-white bdr-white">
	
	
<%
    if(feedEnabled)
    {
	%>
	<span class="right"><a href="<%= request.getContextPath() %>/feed/atom_1.0/<%= community.getHandle() %>">
	
	<img src="//style.anu.edu.au/_anu/images/share/rss.png" class="w16px" alt="RSS feed" /></a></span>
	<%
    }
%><span>RECENT SUBMISSIONS</span>
	</div>
	<anu:box style="solid">
	<%
		Item[] items = rs.getRecentSubmissions();
		if(items!=null && items.length>0) 
		{
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
			%><p><a class="nounderline" href="<%= request.getContextPath() %>/handle/<%= items[i].getHandle() %>"><%= displayTitle %></a></p><%
			}
		}
	%>
	</anu:box>
	
<%
	}
%>
<anu:box backgroundColour="black">
	<p class="center nopadbottom nopadtop"><a class="nounderline" href="<%= request.getContextPath() %>/handle/<%= community.getHandle() %>/statistics"><span class="large"><fmt:message key="jsp.community-home.display-statistics"/></span></a></p>
</anu:box>
		
</anu:content>
</dspace:layout>
