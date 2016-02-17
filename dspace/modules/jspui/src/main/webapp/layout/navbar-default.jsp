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


<anu:menu showSearch="false" id="1108" shortTitle="CAS" ssl="true">
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
	<%-- Search Box --%>
	<div class="search-box">
		<p><label for="tequery"><fmt:message key="jsp.layout.navbar-default.search"/></label></p>
		<form method="get" action="<%= request.getContextPath() %>/advanced-search" class="navbar-form navbar-right" scope="search">
			<div class="form-group">
				<input type="text" class="form-control search-query" placeholder="<fmt:message key="jsp.layout.navbar-default.search"/>" name="query" id="tequery" size="25"/>
				<input type="submit" value="Go" class="search-button" />
			</div>
			<%--<button type="submit" class="btn"><span>GO</span></button>--%>
			<%--
			Disabled. advanced search is not decoding search query properly.
			<br/><a href="<%= request.getContextPath() %>/advanced-search"><fmt:message key="jsp.layout.navbar-default.advanced"/></a>
<%
			if (ConfigurationManager.getBooleanProperty("webui.controlledvocabulary.enable"))
			{
%>        
              <br/><a href="<%= request.getContextPath() %>/subject-search"><fmt:message key="jsp.layout.navbar-default.subjectsearch"/></a>
<%
            }
%> --%>
		</form>
	</div>
	<fmt:message key="jsp.layout.navbar-default.browse" var="browse"/>
	<anu:submenu title="${browse}">
		<li><a href="<%= openResearchURL %>/index.php"><fmt:message key="jsp.layout.navbar-default.home" /></a></li>
		<li><a href="<%= openResearchURL %>/about-open-research"><fmt:message key="jsp.layout.navbar-default.about-open-research" /></a></li>
		<li><a href="<%= request.getContextPath() %>/community-list"><fmt:message key="jsp.layout.navbar-default.communities-collections" /></a></li>
		<li><a href="<%= request.getContextPath() %>/contribute-your-research/index.php"><fmt:message key="jsp.layout.navbar-default.contrib-your-research" /></a>
			<ul>
				<li><a href="<%= openResearchURL %>/contribute-your-research/anu-digital-theses/index.php"><fmt:message key="jsp.layout.navbar-default.anu-digital-theses" /></a>
					<ul>
						<li><a href="<%= openResearchURL %>/contribute-your-research/contribute-your-digital-thesis/index.php"><fmt:message key="jsp.layout.navbar-default.contrib-digital-thesis" /></a></li>
					</ul>
				</li>
			</ul>
		</li>
		<li><a href="<%= openResearchURL %>/publishing-open-access/index.php"><fmt:message key="jsp.layout.navbar-default.pub-open-access" /></a></li>
		<li><a href="<%= openResearchURL %>/policy/"><fmt:message key="jsp.layout.navbar-default.policy" /></a></li>
		<li><a href="<%= openResearchURL %>/copyright-considerations/index.php"><fmt:message key="jsp.layout.navbar-default.copyright-consideration" /></a></li>
		<li><a href="<%= openResearchURL %>/contact-us"><fmt:message key="jsp.layout.navbar-default.contact-us" /></a></li>
		
	</anu:submenu>

	<%
		if (user != null)
		{
	%>
		<fmt:message key="jsp.layout.navbar-default.loggedin" var="signin">
		      <fmt:param><%= StringUtils.abbreviate(navbarEmail, 20) %></fmt:param>
		</fmt:message>
		<%
    } else {
		%>
	<fmt:message key="jsp.layout.navbar-default.sign" var="signin"/>
	<%
		}
	%>
	    <%
	    	if (user != null)
	    	{
	    %>
	<anu:submenu title="${signin}">
	    <li><a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.layout.navbar-default.users"/></a></li>
	    <li><a href="<%= request.getContextPath() %>/subscribe"><fmt:message key="jsp.layout.navbar-default.receive"/></a></li>
	    <li><a href="<%= request.getContextPath() %>/profile"><fmt:message key="jsp.layout.navbar-default.edit"/></a></li>
	    <li><a href="<%= request.getContextPath() %>/logout"><fmt:message key="jsp.layout.navbar-default.logout"/></a></li>
	</anu:submenu>
	
	<fmt:message key="jsp.layout.navbar-admin.statistics" var="statistics"/>
	<anu:submenu title="${statistics}">
	<li><a href="<%= request.getContextPath() %>/statistics"><fmt:message key="jsp.layout.navbar-admin.statistics"/></a></li>
	</anu:submenu>
	    <%
	    	}
	    %>
	
	<%
		  if (isAdmin)
		  {
		%>
			<anu:submenu title="Administer">
               <li><a href="<%= request.getContextPath() %>/dspace-admin"><fmt:message key="jsp.administer"/></a></li>
            </anu:submenu>
		<%
		  }
		%>
		
	<fmt:message key="jsp.layout.navbar-default.about" var="about"/>
	<anu:submenu title="${about}">
		<li><a href="http://anulib.anu.edu.au/"><fmt:message key="jsp.layout.navbar-default.library"/></a></li>
		<li><a href="http://www.anu.edu.au/research"><fmt:message key="jsp.layout.navbar-default.research-innovation"/></a></li>
		<li><a href="http://archives.anu.edu.au"><fmt:message key="jsp.layout.navbar-default.archives"/></a></li>
		<li><a href="http://press.anu.edu.au"><fmt:message key="jsp.layout.navbar-default.press"/></a></li>
		<li><a href="http://copyright.anu.edu.au"><fmt:message key="jsp.layout.navbar-default.copyright"/></a></li>
	</anu:submenu>
	
	<div class="social-icons text-center">
		<a href="https://twitter.com/ANUOpenAccess"><img src="//style.anu.edu.au/_anu/images/share/twitter.png" alt="ANU Open Access on Twitter"/></a> 
	</div>
</anu:menu>

