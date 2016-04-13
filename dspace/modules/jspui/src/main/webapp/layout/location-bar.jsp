<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Location bar component
  -
  - This component displays the "breadcrumb" style navigation aid at the top
  - of most screens.
  -
  - Uses request attributes set in org.dspace.app.webui.jsptag.Layout, and
  - hence must only be used as part of the execution of that tag.  Plus,
  - dspace.layout.locbar should be verified to be true before this is included.
  -
  -  dspace.layout.parenttitles - List of titles of parent pages
  -  dspace.layout.parentlinks  - List of URLs of parent pages, empty string
  -                               for non-links
  --%>

<%@ taglib uri="http://www.anu.edu.au/taglib" prefix="anu" %>
<%@ page contentType="text/html;charset=UTF-8" %>
  
<%@ page import="java.util.List" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%

    String bannerName = ConfigurationManager.getProperty("openresearch.name");
    String bannerUrl = ConfigurationManager.getProperty("openresearch.url");
%>

<anu:breadcrumbs>
<%
    List parentTitles = (List) request.getAttribute("dspace.layout.parenttitles");
    List parentLinks = (List) request.getAttribute("dspace.layout.parentlinks");

    for (int i = 0; i < parentTitles.size(); i++)
    {
        String s = (String) parentTitles.get(i);
        String u = (String) parentLinks.get(i);

        if (u.equals(""))
        {
            if (i == parentTitles.size())
            {
%>
<anu:crumb title="<%= s %>" />

<%           
            }
            else
            {
%>
<anu:crumb title="<%= s %>" />
<%			}
        }
        else
        {
			if (i == 0) 
			{
%>
  <anu:crumb title="Home" href="<%= bannerUrl %>" />
<%
			}
			else{
%>
  <anu:crumb title="<%= s %>" href="<%= request.getContextPath() + u %>" />
<%
			}
        }
}
%>
<% 
	String title = (String)request.getAttribute("dspace.layout.title");
	if (title != null) {
%>
<anu:crumb title="<%= title %>"/>
<%
	}
%>
</anu:breadcrumbs>
