<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Renders a whole HTML page for displaying item metadata.  Simply includes
  - the relevant item display component in a standard HTML page.
  -
  - Attributes:
  -    display.all - Boolean - if true, display full metadata record
  -    item        - the Item to display
  -    collections - Array of Collections this item appears in.  This must be
  -                  passed in for two reasons: 1) item.getCollections() could
  -                  fail, and we're already committed to JSP display, and
  -                  2) the item might be in the process of being submitted and
  -                  a mapping between the item and collection might not
  -                  appear yet.  If this is omitted, the item display won't
  -                  display any collections.
  -    admin_button - Boolean, show admin 'edit' button
  --%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://www.anu.edu.au/taglib" prefix="anu" %>

<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Metadatum" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.handle.HandleManager" %>
<%@ page import="org.dspace.license.CreativeCommons" %>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@page import="org.dspace.versioning.Version"%>
<%@page import="org.dspace.core.Context"%>
<%@page import="org.dspace.app.webui.util.VersionUtil"%>
<%@page import="org.dspace.app.webui.util.UIUtil"%>
<%@page import="org.dspace.authorize.AuthorizeManager"%>
<%@page import="java.util.List"%>
<%@page import="org.dspace.core.Constants"%>
<%@page import="org.dspace.eperson.EPerson"%>
<%@page import="org.dspace.versioning.VersionHistory"%>
<%@page import="org.dspace.content.Bundle" %>
<%@page import="org.dspace.content.Bitstream" %>
<%@page import="org.dspace.statistics.util.SpiderDetector" %>
<%
    // Attributes
    Boolean displayAllBoolean = (Boolean) request.getAttribute("display.all");
    boolean displayAll = (displayAllBoolean != null && displayAllBoolean.booleanValue());
    Boolean suggest = (Boolean)request.getAttribute("suggest.enable");
    boolean suggestLink = (suggest == null ? false : suggest.booleanValue());
    Item item = (Item) request.getAttribute("item");
    Collection[] collections = (Collection[]) request.getAttribute("collections");
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());
    
    // get the workspace id if one has been passed
    Integer workspace_id = (Integer) request.getAttribute("workspace_id");

    // get the handle if the item has one yet
    String handle = item.getHandle();

    // CC URL & RDF
    String cc_url = CreativeCommons.getLicenseURL(item);
    String cc_rdf = CreativeCommons.getLicenseRDF(item);

    // Full title needs to be put into a string to use as tag argument
    String title = "";
    if (handle == null)
 	{
		title = "Workspace Item";
	}
	else 
	{
		Metadatum[] titleValue = item.getDC("title", null, Item.ANY);
		if (titleValue.length != 0)
		{
			title = titleValue[0].value;
		}
		else
		{
			title = "Item " + handle;
		}
	}

	String statement = null;
	if (item.getBundles("BRANDED_PREVIEW").length > 0)
	{
		String s = ConfigurationManager.getProperty("webui.preview.dc");
		
		if (s != null)
		{
			Metadatum[] dcValue;
			
			int i = s.indexOf('.');
			
			if (i == -1)
			{
				dcValue = item.getDC(s, Item.ANY, Item.ANY);
			}
			else
			{
				dcValue = item.getDC(s.substring(0,1), s.substring(i + 1), Item.ANY);
			}
			
			if (dcValue.length > 0)
			{
				statement = dcValue[0].value;
			}
		}
	}

	Bitstream selectedBitstream = null;
	try {
		Bundle[] bunds = item.getBundles("ORIGINAL");
		
		if (bunds[0] != null)
		{
			Bitstream[] bits = bunds[0].getBitstreams();

			for (int i = 0; (i < bits.length) && selectedBitstream == null; i++)
			{
				if (bits[i].getID() == bunds[0].getPrimaryBitstreamID())
				{
					selectedBitstream = bits[i];
				}
			}
			
			if (selectedBitstream == null && bits.length > 0)
			{
				selectedBitstream = bits[0];
			}
		}
	}
	catch (Exception e)
	{
		
	}
	
	String publisherVersionUrl = null;
	
	Metadatum[] doiValues = item.getMetadata("local","identifier","doi",Item.ANY);
	if (doiValues.length > 0)
	{
		publisherVersionUrl = doiValues[0].value;
		if (publisherVersionUrl.length() > 0 && !publisherVersionUrl.startsWith("http")) {
			publisherVersionUrl = "https://doi.org/" + publisherVersionUrl;
		}
	}
	else {
		Metadatum[] sourceUriValues = item.getMetadata("dc","source","uri",Item.ANY);
		if (sourceUriValues.length > 0)
		{
			publisherVersionUrl = sourceUriValues[0].value;
			if (publisherVersionUrl.length() == 0) {
				publisherVersionUrl = null;
			}
		}
	}
    
    Boolean versioningEnabledBool = (Boolean)request.getAttribute("versioning.enabled");
    boolean versioningEnabled = (versioningEnabledBool!=null && versioningEnabledBool.booleanValue());
    Boolean hasVersionButtonBool = (Boolean)request.getAttribute("versioning.hasversionbutton");
    Boolean hasVersionHistoryBool = (Boolean)request.getAttribute("versioning.hasversionhistory");
    boolean hasVersionButton = (hasVersionButtonBool!=null && hasVersionButtonBool.booleanValue());
    boolean hasVersionHistory = (hasVersionHistoryBool!=null && hasVersionHistoryBool.booleanValue());
    
    Boolean newversionavailableBool = (Boolean)request.getAttribute("versioning.newversionavailable");
    boolean newVersionAvailable = (newversionavailableBool!=null && newversionavailableBool.booleanValue());
    Boolean showVersionWorkflowAvailableBool = (Boolean)request.getAttribute("versioning.showversionwfavailable");
    boolean showVersionWorkflowAvailable = (showVersionWorkflowAvailableBool!=null && showVersionWorkflowAvailableBool.booleanValue());
    
    String latestVersionHandle = (String)request.getAttribute("versioning.latestversionhandle");
    String latestVersionURL = (String)request.getAttribute("versioning.latestversionurl");
    
    VersionHistory history = (VersionHistory)request.getAttribute("versioning.history");
    List<Version> historyVersions = (List<Version>)request.getAttribute("versioning.historyversions");

    boolean openAccess = false;
    Metadatum[] accessRights = item.getMetadata("dcterms", "accessRights", Item.ANY, Item.ANY);
	if (accessRights.length > 0 && accessRights[0].value.toLowerCase().equals("open access")){
		openAccess = true;
	}
%>

<%@page import="org.dspace.app.webui.servlet.MyDSpaceServlet"%>
<anu:content layout="full">
<dspace:layout title="<%= title %>">
<script type="text/javascript">
	var contextPath = '<%= request.getContextPath() %>';

	function addImpactServiceCitationCount(service) {
		var idSelector = "#" + service;
		var doi = jQuery(idSelector).data("doi");
		if (doi != null) {
			jQuery.getJSON(contextPath + "/impact" + "?doi=" + doi + "&service=" + service)
				.done(function(data) {
				var citations = data["citationCount"];
				var linkBack = data["linkBack"];
				if (citations && linkBack) {
					jQuery(idSelector + " a").attr("href", linkBack);
					jQuery(idSelector + " .citation-count").text(citations);
					jQuery(idSelector).removeClass("hidden");
				}
				})
				.fail (function(jqxhr, textStatus, error) {
					console.log("Error retrieving " + service + " citation count: " + error);
				});
		}
	}
	
	function addCitationCounts() {
		addImpactServiceCitationCount("wos");
	}
	
	jQuery(document).ready( function() {
		$(".moreelipses").on("click",function(){
			jQuery(this).addClass("hidden");
			jQuery(this).next(".more").removeClass("hidden");
			return false;
		});
		
		addCitationCounts();
	});
</script>
<%
    if (handle != null)
    {
%>

		<%		
		if (newVersionAvailable)
		   {
		%>
		<div class="alert alert-warning"><b><fmt:message key="jsp.version.notice.new_version_head"/></b>		
		<fmt:message key="jsp.version.notice.new_version_help"/><a href="<%=latestVersionURL %>"><%= latestVersionHandle %></a>
		</div>
		<%
		    }
		%>
		
		<%		
		if (showVersionWorkflowAvailable)
		   {
		%>
		<div class="alert alert-warning"><b><fmt:message key="jsp.version.notice.workflow_version_head"/></b>		
		<fmt:message key="jsp.version.notice.workflow_version_help"/>
		</div>
		<%
		    }
		%>
		

                <%-- <strong>Please use this identifier to cite or link to this item:
                <code><%= HandleManager.getCanonicalForm(handle) %></code></strong>--%>
				<%-- <anu:message type="info" extraClass="marginbottom">
                <div class="well"><fmt:message key="jsp.display-item.identifier"/>
                <code><%= HandleManager.getCanonicalForm(handle) %></code></div>
				</anu:message> --%>
<%
        if (admin_button)  // admin edit button
        { %>
            <div class="panel panel-warning marginbottom">
            	<div class="panel-heading"><fmt:message key="jsp.admintools"/></div>
            	<div class="panel-body">
                <form method="get" action="<%= request.getContextPath() %>/tools/edit-item">
                    <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                    <%--<input type="submit" name="submit" value="Edit...">--%>
                    <input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.general.edit.button"/>" />
                </form>
                <form method="post" action="<%= request.getContextPath() %>/mydspace">
                    <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_EXPORT_ARCHIVE %>" />
                    <input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.mydspace.request.export.item"/>" />
                </form>
                <form method="post" action="<%= request.getContextPath() %>/mydspace">
                    <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_MIGRATE_ARCHIVE %>" />
                    <input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.mydspace.request.export.migrateitem"/>" />
                </form>
                <form method="post" action="<%= request.getContextPath() %>/dspace-admin/metadataexport">
                    <input type="hidden" name="handle" value="<%= item.getHandle() %>" />
                    <input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.general.metadataexport.button"/>" />
                </form>
					<% if(hasVersionButton) { %>       
                	<form method="get" action="<%= request.getContextPath() %>/tools/version">
                    	<input type="hidden" name="itemID" value="<%= item.getID() %>" />                    
                    	<input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.general.version.button"/>" />
                	</form>
                	<% } %> 
                	<% if(hasVersionHistory) { %>			                
                	<form method="get" action="<%= request.getContextPath() %>/tools/history">
                    	<input type="hidden" name="itemID" value="<%= item.getID() %>" />
                    	<input type="hidden" name="versionID" value="<%= history.getVersion(item)!=null?history.getVersion(item).getVersionId():null %>" />                    
                    	<input class="btn btn-info col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.general.version.history.button"/>" />
                	</form>         	         	
					<% } %>
             </div>
          </div>
<%      } %>

<%
    }

    String displayStyle = (displayAll ? "full" : "");
%>
<h2 class="padbottom"><%= title %></h2>

<div class="w-narrow right margintop marginleft marginbottom nopadbottom">
<% if (statement != null || selectedBitstream != null || (publisherVersionUrl != null && publisherVersionUrl.length() > 0)) { %>
<anu:box style="bdr2">
	<% if(openAccess) { %>
		<div class="open-access">
		<img src="<%= request.getContextPath() %>/image/open_access.svg" alt="Open Access" title="Open Access" />
		</div>
	<% } %>
	<% 	
	if (statement != null) 
	{
	%>
	<p><%= statement %></p>
	<% 
	} 
	if (selectedBitstream != null)
	{
		Context context = UIUtil.obtainContext(request);
		if (AuthorizeManager.authorizeActionBoolean(context, selectedBitstream, Constants.READ))
		{
		%>
		<p>
		<img class="absmiddle left padright" src="//style.anu.edu.au/_anu/images/icons/web/type-download.png" />
		<a class="nounderline" href="<%= request.getContextPath() %>/bitstream/<%= handle %>/<%= selectedBitstream.getSequenceID() %>/<%= selectedBitstream.getName() %>"><fmt:message key="jsp.display-item.download" /></a> (<%= UIUtil.formatFileSize(selectedBitstream.getSize()) %>)
		</p>
		<%
		}
		else
		{
		%>
		<p>
		<img class="absmiddle left padright" src="//style.anu.edu.au/_anu/images/icons/web/link.png" />
		<a class="nounderline" href="<%= request.getContextPath() %>/request-item?handle=<%= handle %>&bitstream-id=<%= selectedBitstream.getID() %>"><fmt:message key="jsp.display-item.request-copy" /></a>
		</p>
		<%
		}
	} 
	if (publisherVersionUrl != null && publisherVersionUrl.length() > 0) {
	%>
	<p><img class="absmiddle left padright" src="//style.anu.edu.au/_anu/images/icons/web/link.png" /><a class="nounderline" href="<%= publisherVersionUrl %>">link to publisher version</a></p>
	<%
	}
	%>
	
</anu:box>
<% } %>
<ul class="nounderline small padtop">
	<li><a class="nounderline" href="<%= request.getContextPath() %>/handle/<%= handle %>/statistics"><fmt:message key="jsp.display-item.display-statistics"/></a></li>
	<li><a class="nounderline" href="<%= request.getContextPath() %>/exportreference?handle=<%= handle %>&format=bibtex">Export Reference to BibTeX</a></li>
	<li><a class="nounderline" href="<%= request.getContextPath() %>/exportreference?handle=<%= handle %>&format=endnote">Export Reference to EndNote XML</a></li>
</ul>
<ul class="nobullet">
<li>
	<!-- Altmetric badge Start -->
<%
	String altmetricData = null;	
	if (item.getMetadata("local", "identifier", "doi", Item.ANY).length > 0){
		altmetricData = item.getMetadata("local", "identifier", "doi", Item.ANY)[0].value;
	}
	//if (item.getMetadata("local.identifier.doi").length > 0) {
	//	altmetricData = item.getMetadata("local.identifier.doi")[0].value;
	//}

	if (altmetricData != null && altmetricData.length() > 0 ) {
%>
	
	<script type='text/javascript' src='//d1bxh8uas1mnw7.cloudfront.net/assets/embed.js'></script>
	<div><h4>Altmetric Citations</h4></div>
	<div data-badge-details="right" data-badge-type="1" data-doi="<%= altmetricData %>" class="altmetric-embed"></div>
	<%
		if (!SpiderDetector.isSpider(request)) {
	%>
	<c:catch var="errormsg">
		<c:import url="https://api.elsevier.com/content/abstract/citation-count" var="scopusimport" >
			<c:param value="<%= altmetricData %>" name="doi" />
			<c:param name="apiKey" value="118b6ce3c3c7f45a9a7925066d849a63" />
			<c:param name="httpAccept" value="text/html" />
		</c:import>
	</c:catch>
	
	<c:if test="${empty errormsg}">
	<p><h4>Scopus Citations</h4></p>
	${scopusimport}
	</c:if>
	<div class="padtop">
	<h4>Dimensions Citations</h4>
	<span class="__dimensions_badge_embed__" data-doi="<%= altmetricData %>" data-style="small_circle"></span>
	<script async src="https://badge.dimensions.ai/badge.js" charset="utf-8"></script>
	</div>
	<div id="wos" class="hidden" data-doi="<%= altmetricData %>">
		<br/>
		<h4>Web of Science Citations</h4>
		Times Cited: <a class="citation-count" href="#">0</a>
	</div>
	<%
		}
	}
%>
	<!-- Altmetric badge End -->
</li>
</ul>
</div>
<div class="w-doublenarrow">
    <dspace:item-preview item="<%= item %>" />
</div>
    <dspace:item item="<%= item %>" collections="<%= collections %>" style="<%= displayStyle %>" />
<div class="container row padtop">
<%
    String locationLink = request.getContextPath() + "/handle/" + handle;

    if (displayAll)
    {
%>
<%
        if (workspace_id != null)
        {
%>
    <form class="col-md-2" method="post" action="<%= request.getContextPath() %>/view-workspaceitem">
        <input type="hidden" name="workspace_id" value="<%= workspace_id.intValue() %>" />
        <input class="btn btn-default" type="submit" name="submit_simple" value="<fmt:message key="jsp.display-item.text1"/>" />
    </form>
<%
        }
        else
        {
%>
<strong>
    <a href="<%=locationLink %>?mode=simple">
        <fmt:message key="jsp.display-item.text1"/>
    </a>
</strong>
<%
        }
%>
<%
    }
    else
    {
%>
<%
        if (workspace_id != null)
        {
%>
    <form class="col-md-2" method="post" action="<%= request.getContextPath() %>/view-workspaceitem">
        <input type="hidden" name="workspace_id" value="<%= workspace_id.intValue() %>" />
        <input class="btn btn-default" type="submit" name="submit_full" value="<fmt:message key="jsp.display-item.text2"/>" />
    </form>
<%
        }
        else
        {
%>
<strong>
    <a href="<%=locationLink %>?mode=full">
        <fmt:message key="jsp.display-item.text2"/>
    </a>
</strong>
<%
        }
    }

    if (workspace_id != null)
    {
%>
   <form class="col-md-2" method="post" action="<%= request.getContextPath() %>/workspace">
        <input type="hidden" name="workspace_id" value="<%= workspace_id.intValue() %>"/>
        <input class="btn btn-primary" type="submit" name="submit_open" value="<fmt:message key="jsp.display-item.back_to_workspace"/>"/>
    </form>
<%
    } else {

		if (suggestLink)
        {
%>
    <a class="btn btn-success" href="<%= request.getContextPath() %>/suggest?handle=<%= handle %>" target="new_window">
       <fmt:message key="jsp.display-item.suggest"/></a>
<%
        }
%>
    <%-- SFX Link --%>
<%
    if (ConfigurationManager.getProperty("sfx.server.url") != null)
    {
        String sfximage = ConfigurationManager.getProperty("sfx.server.image_url");
        if (sfximage == null)
        {
            sfximage = request.getContextPath() + "/image/sfx-link.gif";
        }
%>
        <a class="btn btn-default" href="<dspace:sfxlink item="<%= item %>"/>" /><img src="<%= sfximage %>" border="0" alt="SFX Query" /></a>
<%
    }
    }
%>
</div>
<br/>
    <%-- Versioning table --%>
<%
    if (versioningEnabled && hasVersionHistory)
    {
        boolean item_history_view_admin = ConfigurationManager
                .getBooleanProperty("versioning", "item.history.view.admin");
        if(!item_history_view_admin || admin_button) {         
%>
	<div id="versionHistory" class="panel panel-info">
	<div class="panel-heading"><fmt:message key="jsp.version.history.head2" /></div>
	
	<table class="table panel-body">
		<tr>
			<th id="tt1" class="oddRowEvenCol"><fmt:message key="jsp.version.history.column1"/></th>
			<th 			
				id="tt2" class="oddRowOddCol"><fmt:message key="jsp.version.history.column2"/></th>
			<th 
				 id="tt3" class="oddRowEvenCol"><fmt:message key="jsp.version.history.column3"/></th>
			<th 
				
				id="tt4" class="oddRowOddCol"><fmt:message key="jsp.version.history.column4"/></th>
			<th 
				 id="tt5" class="oddRowEvenCol"><fmt:message key="jsp.version.history.column5"/> </th>
		</tr>
		
		<% for(Version versRow : historyVersions) {  
		
			EPerson versRowPerson = versRow.getEperson();
			String[] identifierPath = VersionUtil.addItemIdentifier(item, versRow);
		%>	
		<tr>			
			<td headers="tt1" class="oddRowEvenCol"><%= versRow.getVersionNumber() %></td>
			<td headers="tt2" class="oddRowOddCol"><a href="<%= request.getContextPath() + identifierPath[0] %>"><%= identifierPath[1] %></a><%= item.getID()==versRow.getItemID()?"<span class=\"glyphicon glyphicon-asterisk\"></span>":""%></td>
			<td headers="tt3" class="oddRowEvenCol"><% if(admin_button) { %><a
				href="mailto:<%= versRowPerson.getEmail() %>"><%=versRowPerson.getFullName() %></a><% } else { %><%=versRowPerson.getFullName() %><% } %></td>
			<td headers="tt4" class="oddRowOddCol"><%= versRow.getVersionDate() %></td>
			<td headers="tt5" class="oddRowEvenCol"><%= versRow.getSummary() %></td>
		</tr>
		<% } %>
	</table>
	<div class="panel-footer"><fmt:message key="jsp.version.history.legend"/></div>
	</div>
<%
        }
    }
%>
<br/>
    <%-- Create Commons Link --%>
<%
    if (cc_url != null)
    {
%>
    <p class="submitFormHelp alert alert-info"><fmt:message key="jsp.display-item.text3"/> <a href="<%= cc_url %>"><fmt:message key="jsp.display-item.license"/></a>
    <a href="<%= cc_url %>"><img src="<%= request.getContextPath() %>/image/cc-somerights.gif" border="0" alt="Creative Commons" style="margin-top: -5px;" class="pull-right"/></a>
    </p>
    <!--
    <%= cc_rdf %>
    -->
<%
    } else {
%>
    <p class="submitFormHelp alert alert-info"><fmt:message key="jsp.display-item.copyright"/></p>
<%
    } 
%>    
</dspace:layout>
</anu:content>
