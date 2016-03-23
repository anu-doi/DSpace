<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Default navigation bar
--%>

<%@page import="org.apache.commons.lang.StringUtils"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://www.anu.edu.au/taglib" prefix="anu" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="java.util.Map" %>
<%
    // Is anyone logged in?
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");

    // Is the logged in user an admin
    Boolean admin = (Boolean)request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());
    
    String openResearchURL = ConfigurationManager.getProperty("openresearch.url");

    // Get the current page, minus query string
    String currentPage = UIUtil.getOriginalURL(request);
    int c = currentPage.indexOf( '?' );
    if( c > -1 )
    {
        currentPage = currentPage.substring( 0, c );
    }

    // E-mail may have to be truncated
    String navbarEmail = null;

    if (user != null)
    {
        navbarEmail = user.getEmail();
    }
    
    // get the browse indices
    
	BrowseIndex[] bis = BrowseIndex.getBrowseIndices();
    BrowseInfo binfo = (BrowseInfo) request.getAttribute("browse.info");
    String browseCurrent = "";
    if (binfo != null)
    {
        BrowseIndex bix = binfo.getBrowseIndex();
        // Only highlight the current browse, only if it is a metadata index,
        // or the selected sort option is the default for the index
        if (bix.isMetadataIndex() || bix.getSortOption() == binfo.getSortOption())
        {
            if (bix.getName() != null)
    			browseCurrent = bix.getName();
        }
    }
%>


<anu:topmenu>
<anu:topmenulinks>
	<li><a class="tabs-home" href="<%= openResearchURL %>">Home</a></li>
	<li><a href="<%= openResearchURL %>/about-open-research-anu">About</a></li>
	<li><a href="<%= request.getContextPath() %>/community-list">Collections</a></li>
	<li><a href="<%= openResearchURL %>/contribute-your-research/">Contribute</a></li>
	<li><a href="<%= openResearchURL %>/publishing">Publishing</a></li>
	<li><a href="<%= openResearchURL %>/policy">Policy</a></li>
	<li><a href="<%= openResearchURL %>/copyright-considerations">Copyright</a></li>
	<li><a href="<%= openResearchURL %>/contact">Contact</a></li>
		<%
			if (user != null)
			{
		%>
	<li>
	
			<fmt:message key="jsp.layout.navbar-default.loggedin" var="signin">
				  <fmt:param><%= StringUtils.abbreviate(navbarEmail, 25) %></fmt:param>
			</fmt:message>
			<a id="gw-mega-tab-9" data-mega-menu-trigger="9" href="#">
		My Open Research
		</a>
	</li>
			<%
		}
			%>
</anu:topmenulinks>
<%
	if (user != null)
	{
%>
<div class="right padright">
<a class="tabs-logout" href="<%=  request.getContextPath() %>/logout">Logout</a>
</div>
<%
	}
%>
</anu:topmenu>


<anu:topmenucontents>
	<anu:topmenudropdown tabId="9">
		<div class="gw-mega-t1 narrow nopadtop nopadbottom">
			<div class="gw-mega-t1">
				<h1>
					<a href="<%= request.getContextPath() %>/mydspace">My Open Research</a>
				</h1>
				<p>Your list of unfinished submissions or submissions in the workflow.</p>
			</div>
		</div>
		<div class="narrow gw-mega-t2 nopadtop nopadbottom">
			<ul>
				<li><a href="<%= request.getContextPath() %>/statistics"><fmt:message key="jsp.layout.navbar-admin.statistics"/></a></li>
				<li><a href="<%= request.getContextPath() %>/logout"><fmt:message key="jsp.layout.navbar-default.logout"/></a></li>
			</ul>
		</div>
		<div class="narrow gw-mega-t2 nopadtop nopadbottom">
			<ul>
				<li><a href="<%= request.getContextPath() %>/subscribe"><fmt:message key="jsp.layout.navbar-default.receive"/></a></li>
				<li><a href="<%= request.getContextPath() %>/profile"><fmt:message key="jsp.layout.navbar-default.edit"/></a></li>
			</ul>
		</div>
		<%
			if (isAdmin)
			{
		%>
		<div class="narrow gw-mega-t2 nopadtop nopadbottom">
			<ul>
				<li><a href="<%= request.getContextPath() %>/dspace-admin"><fmt:message key="jsp.administer"/></a></li>
			</ul>
		</div>
		<%
			}
		%>
	</anu:topmenudropdown>
</anu:topmenucontents>

<anu:topmobilemenu>
	<li><a href="<%= openResearchURL %>">Home</a></li>
	<li><a href="<%= openResearchURL %>/about-open-research-anu">About</a></li>
	<li><a href="<%= request.getContextPath() %>/community-list">Collections</a></li>
	<li><a href="<%= openResearchURL %>/contribute-your-research/">Contribute</a></li>
	<li><a href="<%= openResearchURL %>/publishing">Publishing</a></li>
	<li><a href="<%= openResearchURL %>/policy">Policy</a></li>
	<li><a href="<%= openResearchURL %>/copyright-considerations">Copyright</a></li>
	<li><a href="<%= openResearchURL %>/contact">Contact</a></li>
		<%
			if (user != null)
			{
		%>
	<li><a href="<%= request.getContextPath() %>/mydspace">My Open Research</a>
		<ul>
			<li><a href="<%= request.getContextPath() %>/statistics"><fmt:message key="jsp.layout.navbar-admin.statistics"/></a></li>
			<li><a href="<%= request.getContextPath() %>/logout"><fmt:message key="jsp.layout.navbar-default.logout"/></a></li>
		<%
			if (isAdmin)
			{
		%>
			<li><a href="<%= request.getContextPath() %>/dspace-admin"><fmt:message key="jsp.administer"/></a></li>
		<%
			}
		%>
		</ul>
	</li>
			<%
		}
			%>
	<%
	if (user != null)
	{
%>
	<li><a class="tabs-logout" href="<%=  request.getContextPath() %>/logout">Logout</a></li>
<%
	}
%>
</anu:topmobilemenu>