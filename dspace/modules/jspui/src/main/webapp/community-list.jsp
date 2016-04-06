<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display hierarchical list of communities and collections
  -
  - Attributes to be passed in:
  -    communities         - array of communities
  -    collections.map  - Map where a keys is a community IDs (Integers) and 
  -                      the value is the array of collections in that community
  -    subcommunities.map  - Map where a keys is a community IDs (Integers) and 
  -                      the value is the array of subcommunities in that community
  -    admin_button - Boolean, show admin 'Create Top-Level Community' button
  --%>

<%@page import="org.dspace.content.Bitstream"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
	
<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.browse.ItemCountException" %>
<%@ page import="org.dspace.browse.ItemCounter" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.Map" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://www.anu.edu.au/taglib" prefix="anu" %>

<%
    Community[] communities = (Community[]) request.getAttribute("communities");
    Map collectionMap = (Map) request.getAttribute("collections.map");
    Map subcommunityMap = (Map) request.getAttribute("subcommunities.map");
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));
    
    String openResearchURL = ConfigurationManager.getProperty("openresearch.url");
%>

<%!

	void showCommunity(Community c, JspWriter out, HttpServletRequest request, ItemCounter ic,
			Map collectionMap, Map subcommunityMap) throws ItemCountException, IOException, SQLException
	{
		boolean hasParent = true;
		if (c.getParentCommunity() == null) {
			hasParent = false;
		}
        Collection[] cols = (Collection[]) collectionMap.get(c.getID());
        Community[] comms = (Community[]) subcommunityMap.get(c.getID());
        out.println("<li>");
		//out.println("<a class=\"right\" href=\""+request.getContextPath()+"/handle/"+c.getHandle()+"\"><img class=\"absmiddle left padright\" src=\"http://style.anu.edu.au/_anu/images/icons/silk/link.png\" /></a>");
		if (hasParent) {
			out.println("<h3 class=\"nounderline\">"+c.getName()+"</h3>");
			out.println("<div class=\"anutoggle\">");
		}
		else {
			out.println("<h2 class=\"nounderline\"><a href=\""+request.getContextPath()+"/handle/"+c.getHandle()+"\">"+c.getName()+"</a></h2>");
		}
		if (cols != null && cols.length > 0)
		{
			out.println("<ul class=\"nounderline nopadtop\">");
			for (int j = 0; j < cols.length; j++)
			{
				out.println("<li><a href=\""+request.getContextPath()+"/handle/"+cols[j].getHandle()+"\">"+cols[j].getName()+"</a></li>");
			}
			out.println("</ul>");
		}
		if (comms != null && comms.length > 0)
		{
			out.println("<ul>");
			{
			for (int k = 0; k < comms.length; k++)
			{
	        	showCommunity(comms[k], out, request, ic, collectionMap, subcommunityMap);
			}
			}
			out.println("</ul>");
		}
		if (hasParent) {
			out.println("</div>");
		}
		out.println("</li>");
	}
%>

<dspace:layout titlekey="jsp.community-list.title">
<%
    if (admin_button)
    {
%>   
<anu:content layout="full">
  
			<div class="panel panel-warning">
			<div class="panel-heading">
				<fmt:message key="jsp.admintools"/>
				<span class="pull-right">
					<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\")%>"><fmt:message key="jsp.adminhelp"/></dspace:popup>
				</span>
			</div>
			<div class="panel-body">
                <form method="post" action="<%=request.getContextPath()%>/dspace-admin/edit-communities">
                    <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_CREATE_COMMUNITY%>" />
					<input class="btn btn-default" type="submit" name="submit" value="<fmt:message key="jsp.community-list.create.button"/>" />
                </form>
            </div>
			</div>
</anu:content>
<%
    }
%>
<fmt:message key="jsp.community-list.title" var="title" />
<anu:content layout="full" title="${title}">
	<p>
	<fmt:message key="jsp.community-list.text1">
		<fmt:param value="<%= openResearchURL %>" />
		<fmt:param value="handle/1885/1" />
		<fmt:param value="handle/1885/2" />
	</fmt:message>
	</p>
</anu:content>
<anu:content layout="full">
	<anu:divider style="solid" />
</anu:content>
<% if (communities.length != 0)
{
	%>
	<% 
	for (int i = 0; i < communities.length; i++)
	{
        //showCommunity(communities[i], out, request, ic, collectionMap, subcommunityMap);
		
	%>
		<anu:content layout="doublenarrow" extraClass="box bg-grey10">
			<div>
				<%
					String imgUrl = null;
					if ("1885/1".equals(communities[i].getHandle())) {
						imgUrl = request.getContextPath() + "/image/research-collections.jpg";
					}
					else if ("1885/2".equals(communities[i].getHandle())) {
						imgUrl = request.getContextPath() + "/image/archival-collections.jpg";
					}
				
					if (imgUrl != null) {
				%>
				
					<div class="marginright left w-wide hide-rsp">
						<a href="#"><img src="<%= imgUrl %>" alt="Community logo"/></a>
					</div>
				<%
					}
				%>
			
				<div>
				<h2><a href="<%= request.getContextPath() %>/handle/<%= communities[i].getHandle() %>">Search <%= communities[i].getName() %>&nbsp;&raquo;</a></h2>
				<%
					String shortDesc = communities[i].getMetadata("short_description");
					if (shortDesc != null) {
				%>
					<%= shortDesc %>
				<%
					}
				%>
				</div>
			</div>
		</anu:content>
	<%
	}
	%>
 
<% }
%>
</dspace:layout>
