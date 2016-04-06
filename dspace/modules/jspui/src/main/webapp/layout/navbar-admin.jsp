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
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://www.anu.edu.au/taglib" prefix="anu" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
	// Is anyone logged in?
	EPerson user = (EPerson) request.getAttribute("dspace.current.user");

    String openResearchURL = ConfigurationManager.getProperty("openresearch.url");
	
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

<anu:topmenu>
<anu:topmenulinks>
	<li><a class="tabs-home" href="<%= openResearchURL %>">Home</a></li>
	<li><a id="gw-mega-tab-2" data-mega-menu-trigger="2" href="#">My Open Research</a></li>
	<li><a id="gw-mega-tab-3" data-mega-menu-trigger="3" href="<%= request.getContextPath() %>/dspace-admin">Administration</a></li>
	<li><a id="gw-mega-tab-4" data-mega-menu-trigger="4" href="#"><fmt:message key="jsp.layout.navbar-admin.contents"/></a></li>
	<li><a id="gw-mega-tab-5" data-mega-menu-trigger="5" href="#"><fmt:message key="jsp.layout.navbar-admin.accesscontrol" /></a></li>
	<li><a href="<%= request.getContextPath() %>/statistics"><fmt:message key="jsp.layout.navbar-admin.statistics"/></a></li>
	<li><a id="gw-mega-tab-7" data-mega-menu-trigger="7" href="#"><fmt:message key="jsp.layout.navbar-admin.settings"/></a></li>
</anu:topmenulinks>
</anu:topmenu>

<anu:topmenucontents>
	<anu:topmenudropdown tabId="2">
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
				<li><a href="<%= request.getContextPath() %>/profile"><fmt:message key="jsp.layout.navbar-default.edit"/></a></li>
				<li><a href="<%= request.getContextPath() %>/subscribe"><fmt:message key="jsp.layout.navbar-default.receive"/></a></li>
			</ul>
		</div>
		<div class="narrow gw-mega-t2 nopadtop nopadbottom">
			<ul>
				<li><a href="<%= request.getContextPath() %>/statistics"><fmt:message key="jsp.layout.navbar-admin.statistics"/></a></li>
				<li><a href="<%= request.getContextPath() %>/logout"><fmt:message key="jsp.layout.navbar-default.logout"/></a></li>
			</ul>
		</div>
	</anu:topmenudropdown>
	<anu:topmenudropdown tabId="3">
		<div class="gw-mega-t1 narrow nopadtop nopadbottom">
			<div class="gw-mega-t1">
				<h1>
					<a href="<%= request.getContextPath() %>/dspace-admin">Administration</a>
				</h1>
				<p>Adminstration home page</p>
			</div>
		</div>
		<div class="narrow gw-mega-t2 nopadtop nopadbottom">
			<ul>
				<li><a href="<%= openResearchURL %>"><fmt:message	key="jsp.layout.navbar-default.home" /></a></li>
				<li><a href="<%= request.getContextPath() %>/tools/edit-communities"><fmt:message key="jsp.layout.navbar-admin.communities-collections"/></a></li>
			</ul>
		</div>
	</anu:topmenudropdown>
	<anu:topmenudropdown tabId="4">
		<div class="narrow gw-mega-t2 nopadtop nopadbottom">
			<ul>
				<li><a href="<%= request.getContextPath() %>/dspace-admin/supervise"><fmt:message key="jsp.layout.navbar-admin.supervisors"/></a></li>
				<li><a href="<%= request.getContextPath() %>/dspace-admin/curate"><fmt:message key="jsp.layout.navbar-admin.curate"/></a></li>
			</ul>
		</div>
		<div class="narrow gw-mega-t2 nopadtop nopadbottom">
			<ul>
				<li><a href="<%= request.getContextPath() %>/tools/edit-item"><fmt:message key="jsp.layout.navbar-admin.items"/></a></li>
				<li><a href="<%= request.getContextPath() %>/dspace-admin/workflow"><fmt:message key="jsp.layout.navbar-admin.workflow"/></a></li>
			</ul>
		</div>
		<div class="narrow gw-mega-t2 nopadtop nopadbottom">
			<ul>
				<li><a href="<%= request.getContextPath() %>/dspace-admin/withdrawn"><fmt:message key="jsp.layout.navbar-admin.withdrawn"/></a></li>
				<li><a href="<%= request.getContextPath() %>/dspace-admin/privateitems"><fmt:message key="jsp.layout.navbar-admin.privateitems"/></a></li>
			</ul>
		</div>
		<div class="narrow gw-mega-t2 nopadtop nopadbottom">
			<ul>
				<li><a href="<%= request.getContextPath() %>/dspace-admin/metadataimport"><fmt:message key="jsp.layout.navbar-admin.metadataimport"/></a></li>
				<li><a href="<%= request.getContextPath() %>/dspace-admin/batchimport"><fmt:message key="jsp.layout.navbar-admin.batchimport"/></a></li>
			</ul>
		</div>
	</anu:topmenudropdown>
	<anu:topmenudropdown tabId="5">
		<div class="narrow gw-mega-t2 nopadtop nopadbottom">
			<ul>
				<li><a href="<%= request.getContextPath() %>/dspace-admin/edit-epeople"><fmt:message key="jsp.layout.navbar-admin.epeople"/></a></li>
				<li><a href="<%= request.getContextPath() %>/tools/group-edit"><fmt:message key="jsp.layout.navbar-admin.groups"/></a></li>
				<li><a href="<%= request.getContextPath() %>/tools/authorize"><fmt:message key="jsp.layout.navbar-admin.authorization"/></a></li>
			</ul>
		</div>
	</anu:topmenudropdown>
	<anu:topmenudropdown tabId="7">
		<div class="narrow gw-mega-t2 nopadtop nopadbottom">
			<ul>
				<li><a href="<%= request.getContextPath() %>/dspace-admin/metadata-schema-registry"><fmt:message key="jsp.layout.navbar-admin.metadataregistry"/></a></li>
				<li><a href="<%= request.getContextPath() %>/dspace-admin/format-registry"><fmt:message key="jsp.layout.navbar-admin.formatregistry"/></a></li>
			</ul>
		</div>
		<div class="narrow gw-mega-t2 nopadtop nopadbottom">
			<ul>
				<li><a href="<%= request.getContextPath() %>/dspace-admin/news-edit"><fmt:message key="jsp.layout.navbar-admin.editnews"/></a></li>
				<li><a href="<%= request.getContextPath() %>/dspace-admin/license-edit"><fmt:message key="jsp.layout.navbar-admin.editlicense"/></a></li>
			</ul>
		</div>
	</anu:topmenudropdown>
</anu:topmenucontents>

<anu:topmobilemenu>
	<li><a href="<%= request.getContextPath() %>/dspace-admin">Administration</a>
		<ul>
			<li><a href="<%= request.getContextPath() %>/"><fmt:message	key="jsp.layout.navbar-default.home" /></a></li>
			<li><a href="<%= request.getContextPath() %>/tools/edit-communities"><fmt:message key="jsp.layout.navbar-admin.communities-collections"/></a></li>
		</ul>
	</li>
	<li><a href="#"><fmt:message key="jsp.layout.navbar-admin.contents"/></a>
		<ul>
			<li><a href="<%= request.getContextPath() %>/tools/edit-item"><fmt:message key="jsp.layout.navbar-admin.items"/></a></li>
			<li><a href="<%= request.getContextPath() %>/dspace-admin/workflow"><fmt:message key="jsp.layout.navbar-admin.workflow"/></a></li>
			<li><a href="<%= request.getContextPath() %>/dspace-admin/supervise"><fmt:message key="jsp.layout.navbar-admin.supervisors"/></a></li>
			<li><a href="<%= request.getContextPath() %>/dspace-admin/curate"><fmt:message key="jsp.layout.navbar-admin.curate"/></a></li>
			<li><a href="<%= request.getContextPath() %>/dspace-admin/withdrawn"><fmt:message key="jsp.layout.navbar-admin.withdrawn"/></a></li>
			<li><a href="<%= request.getContextPath() %>/dspace-admin/privateitems"><fmt:message key="jsp.layout.navbar-admin.privateitems"/></a></li>
			<li><a href="<%= request.getContextPath() %>/dspace-admin/metadataimport"><fmt:message key="jsp.layout.navbar-admin.metadataimport"/></a></li>
			<li><a href="<%= request.getContextPath() %>/dspace-admin/batchimport"><fmt:message key="jsp.layout.navbar-admin.batchimport"/></a></li>
		</ul>
	</li>
	<li><a href="#"><fmt:message key="jsp.layout.navbar-admin.accesscontrol" /></a>
		<ul>
			<li><a href="<%= request.getContextPath() %>/dspace-admin/edit-epeople"><fmt:message key="jsp.layout.navbar-admin.epeople"/></a></li>
			<li><a href="<%= request.getContextPath() %>/tools/group-edit"><fmt:message key="jsp.layout.navbar-admin.groups"/></a></li>
			<li><a href="<%= request.getContextPath() %>/tools/authorize"><fmt:message key="jsp.layout.navbar-admin.authorization"/></a></li>
		</ul>
	</li>
	<li><a href="<%= request.getContextPath() %>/statistics"><fmt:message key="jsp.layout.navbar-admin.statistics"/></a></li>
	<li><a href="#"><fmt:message key="jsp.layout.navbar-admin.settings"/></a>
		<ul>
			<li><a href="<%= request.getContextPath() %>/dspace-admin/metadata-schema-registry"><fmt:message key="jsp.layout.navbar-admin.metadataregistry"/></a></li>
			<li><a href="<%= request.getContextPath() %>/dspace-admin/format-registry"><fmt:message key="jsp.layout.navbar-admin.formatregistry"/></a></li>
			<li><a href="<%= request.getContextPath() %>/dspace-admin/news-edit"><fmt:message key="jsp.layout.navbar-admin.editnews"/></a></li>
			<li><a href="<%= request.getContextPath() %>/dspace-admin/license-edit"><fmt:message key="jsp.layout.navbar-admin.editlicense"/></a></li>
		</ul>
	</li>
	<%
	if (user != null)
	{
%>
	<li><a class="tabs-logout" href="<%=  request.getContextPath() %>/logout">Logout</a></li>
<%
	}
%>
</anu:topmobilemenu>
