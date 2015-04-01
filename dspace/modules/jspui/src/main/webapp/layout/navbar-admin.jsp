<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Navigation bar for admin pages
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.LinkedList" %>
<%@ page import="java.util.List" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="org.dspace.sort.SortOption" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@page import="org.apache.commons.lang.StringUtils"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://www.anu.edu.au/taglib" prefix="anu" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
	// Is anyone logged in?
	EPerson user = (EPerson) request.getAttribute("dspace.current.user");

    // Get the current page, minus query string
    String currentPage = UIUtil.getOriginalURL(request);    
    int c = currentPage.indexOf( '?' );
    if( c > -1 )
    {
        currentPage = currentPage.substring(0, c);
    }
    
    // E-mail may have to be truncated
    String navbarEmail = null;
    if (user != null)
    {
        navbarEmail = user.getEmail();
    }

%>

<anu:menu showSearch="false" id="1108" shortTitle="Administration" ssl="true">
	<%
		if (user != null)
		{
	%>
		<anu:box style="solid">
			<small>
			<fmt:message key="jsp.layout.navbar-default.loggedin" var="signin">
				  <fmt:param><%= StringUtils.abbreviate(navbarEmail, 20) %></fmt:param>
			</fmt:message>
			${signin}
			
			(<a href="<%= request.getContextPath() %>/logout"><fmt:message key="jsp.layout.navbar-default.logout"/></a>)
			</small>
		</anu:box>
	<%
		}
	%>
	<!-- Home -->
	<anu:submenu title="Administration">
		<li><a href="<%= request.getContextPath() %>/"><fmt:message	key="jsp.layout.navbar-default.home" /></a></li>
		<li><a href="<%= request.getContextPath() %>/tools/edit-communities"><fmt:message key="jsp.layout.navbar-admin.communities-collections"/></a></li>
	</anu:submenu>
		
	<!-- Content -->
	<fmt:message key="jsp.layout.navbar-admin.contents" var="content"/>
	<anu:submenu title="${content}">
	<li><a href="<%= request.getContextPath() %>/tools/edit-item"><fmt:message key="jsp.layout.navbar-admin.items"/></a></li>
	<li><a href="<%= request.getContextPath() %>/dspace-admin/workflow"><fmt:message key="jsp.layout.navbar-admin.workflow"/></a></li>
	<li><a href="<%= request.getContextPath() %>/dspace-admin/supervise"><fmt:message key="jsp.layout.navbar-admin.supervisors"/></a></li>
	<li><a href="<%= request.getContextPath() %>/dspace-admin/curate"><fmt:message key="jsp.layout.navbar-admin.curate"/></a></li>
	<li><a href="<%= request.getContextPath() %>/dspace-admin/withdrawn"><fmt:message key="jsp.layout.navbar-admin.withdrawn"/></a></li>
	<li><a href="<%= request.getContextPath() %>/dspace-admin/privateitems"><fmt:message key="jsp.layout.navbar-admin.privateitems"/></a></li>
	<li><a href="<%= request.getContextPath() %>/dspace-admin/metadataimport"><fmt:message key="jsp.layout.navbar-admin.metadataimport"/></a></li>
	<li><a href="<%= request.getContextPath() %>/dspace-admin/batchmetadataimport"><fmt:message key="jsp.layout.navbar-admin.batchmetadataimport"/></a></li>
	</anu:submenu>
	
	<!-- Access Control -->
	<fmt:message key="jsp.layout.navbar-admin.accesscontrol" var="accessControl"/>
	<anu:submenu title="${accessControl}">
	<li><a href="<%= request.getContextPath() %>/dspace-admin/edit-epeople"><fmt:message key="jsp.layout.navbar-admin.epeople"/></a></li>
	<li><a href="<%= request.getContextPath() %>/tools/group-edit"><fmt:message key="jsp.layout.navbar-admin.groups"/></a></li>
	<li><a href="<%= request.getContextPath() %>/tools/authorize"><fmt:message key="jsp.layout.navbar-admin.authorization"/></a></li>
	</anu:submenu>
	
	<!-- Statistics -->
	<fmt:message key="jsp.layout.navbar-admin.statistics" var="statistics"/>
	<anu:submenu title="${statistics}">
	<li><a href="<%= request.getContextPath() %>/statistics"><fmt:message key="jsp.layout.navbar-admin.statistics"/></a></li>
	</anu:submenu>
	
	<!-- General Settings -->
	<fmt:message key="jsp.layout.navbar-admin.settings" var="settings"/>
	<anu:submenu title="${settings}">
	<li><a href="<%= request.getContextPath() %>/dspace-admin/metadata-schema-registry"><fmt:message key="jsp.layout.navbar-admin.metadataregistry"/></a></li>
	<li><a href="<%= request.getContextPath() %>/dspace-admin/format-registry"><fmt:message key="jsp.layout.navbar-admin.formatregistry"/></a></li>
	<li class="divider"></li>
	<li><a href="<%= request.getContextPath() %>/dspace-admin/news-edit"><fmt:message key="jsp.layout.navbar-admin.editnews"/></a></li>
	<li class="divider"></li>
	<li><a href="<%= request.getContextPath() %>/dspace-admin/license-edit"><fmt:message key="jsp.layout.navbar-admin.editlicense"/></a></li>
	</anu:submenu>
	
	<!-- Help -->        
	<fmt:message key="jsp.layout.navbar-admin.accesscontrol" var="about"/>
	<anu:submenu title="About">
        <li class="<%= ( currentPage.endsWith( "/help" ) ? "active" : "" ) %>"><dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") %>"><fmt:message key="jsp.layout.navbar-admin.help"/></dspace:popup></li>
	</anu:submenu>
</anu:menu>
