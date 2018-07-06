<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Home page JSP
  -
  - Attributes:
  -    communities - Community[] all communities in DSpace
  -    recent.submissions - RecetSubmissions
  --%>

<%@page import="org.dspace.core.Utils"%>
<%@page import="org.dspace.content.Bitstream"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://www.anu.edu.au/taglib" prefix="anu" %>

<%@ page import="java.io.File" %>
<%@ page import="java.util.Enumeration"%>
<%@ page import="java.util.Locale"%>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.webui.components.RecentSubmissions" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.NewsManager" %>
<%@ page import="org.dspace.browse.ItemCounter" %>
<%@ page import="org.dspace.content.Metadatum" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>

<%
    Community[] communities = (Community[]) request.getAttribute("communities");

    Locale[] supportedLocales = I18nUtil.getSupportedLocales();
    Locale sessionLocale = UIUtil.getSessionLocale(request);
    Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
    String topNews = NewsManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-top.html"));
    String sideNews = NewsManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-side.html"));

    boolean feedEnabled = ConfigurationManager.getBooleanProperty("webui.feed.enable");
    String feedData = "NONE";
    if (feedEnabled)
    {
        feedData = "ALL:" + ConfigurationManager.getProperty("webui.feed.formats");
    }
    
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));

    RecentSubmissions submissions = (RecentSubmissions) request.getAttribute("recent.submissions");
	
    BrowseIndex[] bis = BrowseIndex.getBrowseIndices();
    
    String openResearchURL = ConfigurationManager.getProperty("openresearch.url");
%>

<dspace:layout locbar="off" titlekey="jsp.home.title" feedData="<%= feedData %>">

<anu:content layout="full" title="${title}">
	<%-- <p>
	<fmt:message key="jsp.community-list.text1" />
	</p> --%>
	<div class="box20 bg-uni25 nomargin nomarginbottom bigsearch">
	<div class="large padbottom">Search</div>
	<form id="searchform" method="get" action="<%= request.getContextPath() %>/advanced-search">
		<input id="query" name="query" class="margintop marginbottom text tfull" type="text" placeholder="Search by keyword..." size="24" />
		<input id="main" type="submit" value="GO" class="btn-uni-grad btn-medium" />
	</form>
	<p>
		<a href="<%= request.getContextPath() %>/advanced-search">advanced search &raquo;</a>
	</p>
	<%
		if (bis.length > 0)
		{
	%>
	<div class="divline-solid-uni padtop"></div>
	<div class="large padtop padbottom">
		Browse by:
	</div>
	<div class="full nopadleft noprint nopadtop padbottom tools-uni">
		<ul class="nopadbottom nopadtop">
			<%
				for (int i =0; i < bis.length; i++)
				{
				String key = "browse.menu." + bis[i].getName();
			%>
				<li>
					<a class="large acton-tabs-link-processed" href="<%= request.getContextPath() %>/browse?type=<%= bis[i].getName() %>"><fmt:message key="<%= key %>" /></a>
				</li>
			<%
				}
			%>
		</ul>
	</div>
	<p class="padbottom padtop"></p>
	<%
		}
	%>
	</div>
</anu:content>

<% if (communities.length != 0)
{
	%>
	<% 
	for (int i = 0; i < communities.length; i++)
	{
	%>
		<anu:content layout="one-third">
			<div class="div1 box bg-grey10 colbox">
			<div>
				<%
					String imgUrl = null;
					if ("1885/1".equals(communities[i].getHandle())) {
						imgUrl = request.getContextPath() + "/image/anu-research.jpg";
					}
					else if ("1885/2".equals(communities[i].getHandle())) {
						imgUrl = request.getContextPath() + "/image/archival-and-rare-collections.jpg";
					}
				
					if (imgUrl != null) {
				%>
				
					<div>
						<a href="<%= request.getContextPath() %>/handle/<%= communities[i].getHandle() %>"><img src="<%= imgUrl %>" alt="Community logo"/></a>
					</div>
				<%
					}
				%>
			
				<div>
				<h2><a class="nounderline" href="<%= request.getContextPath() %>/handle/<%= communities[i].getHandle() %>">Search <%= communities[i].getName() %></a></h2>
				<%
					String shortDesc = communities[i].getMetadata("short_description");
					if (shortDesc != null) {
				%>
					<%= shortDesc %>
				<%
					}
				%>
				<span><%= ic.getCount(communities[i]) %> items</span>
				</div>
			</div>
			</div>
		</anu:content>
	<%
	}
	%>
 
<% }
%>
<anu:content layout="one-third">
	<div class="div1 box bg-grey10 colbox">
		<div>
			<a href="<%= openResearchURL %>/contribute"><img src="<%= request.getContextPath() %>/image/contribute.jpg" alt="Contribute logo"/></a>
		</div>
		<h2><a class="nounderline" href="https://openresearch.anu.edu.au/contribute">Contribute</a></h2>
	</div>	
</anu:content>

<anu:content layout="two-third" extraClass="clear-right">
	<div class="divline-solid-uni nopad"></div>
	
<%
if (submissions != null && submissions.count() > 0)
{
%>
	<div>
	<h3 class="padtop">Recent Submissions</h3>
	<ul class="linklist extraspace nopadtop padbottom">
		<%
			for (Item item : submissions.getRecentSubmissions())
			{
				Metadatum[] titleValue = item.getDC("title", null, Item.ANY);
				String displayTitle = "Untitled";
				if (titleValue != null & titleValue.length > 0)
				{
					displayTitle = Utils.addEntities(titleValue[0].value);
				}
		%>
			<li>
				<a href="<%= request.getContextPath() %>/handle/<%=item.getHandle() %>">
					<%= displayTitle %>
				</a>
			</li>
		<%
			}
		%>
	</ul>
	</div>
<%
}
%>
	<div class="divline-solid-uni nopad"></div>
	<h3 class="padtop">Top downloads for past month</h3>
	<script type="text/javascript">
		jQuery(document).ready(function() {
			var today = new Date();
			var lastMonth = new Date();
			lastMonth.setDate(lastMonth.getDate() - 30);
			/*console.log(lastMonth.getDate());
			console.log(lastMonth.getMonth()+1);
			console.log(lastMonth.getFullYear());
			console.log(today.getDate());
			console.log(today.getMonth()+1);
			console.log(today.getFullYear());*/
			jQuery.ajax({
				url: window.location.origin+window.location.pathname+"statistics"
				, type: "GET"
				, data: {
					section: "statsTopItems"
					, format: "json"
					, limit: "5"
					, sDay: lastMonth.getDate()
					, sMonth: lastMonth.getMonth()
					, sYear: lastMonth.getFullYear()
					, eDay: today.getDate()
					, eMonth: today.getMonth()
					, eYear: today.getFullYear()
				}
				, success: function(data) {
					var statsSection = jQuery("#statsTopItems");
					statsSection.html('<ul></ul>');
					var ul = statsSection.find("ul");
					ul.attr("class","linklist extraspace nopadtop");
					var values = data.values
					for (var i = 0; i < values.length; i++) {
						var li = jQuery("<li></li>").attr("class","acton-tabs-link-processed").appendTo(ul);
						if (values[i].Handle) {
							var url = data.path + "/handle/" + values[i].Handle;
							var a = jQuery("<a></a>").attr("href", url).text(values[i].Name);
							a.appendTo(li);
						}
						else {
							li.text(values[i].Name);
						}
					}
				}
				, error: function() {
					jQuery("#statsTopItems").html('Error retreiving top downloads');
				}
			});
		});
	</script>
	<div id="statsTopItems">
		Loading top downloads...
	</div>
</anu:content>

<anu:content layout="one-third">
	<div class="box bdr-solid bdr-uni">
		<a class="twitter-timeline" data-dnt="true" href="https://twitter.com/ANUOpenAccess" height="250px" data-widget-id="706691559580774403">Tweets by @ANUOpenAccess</a>
		<script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+"://platform.twitter.com/widgets.js";fjs.parentNode.insertBefore(js,fjs);}}(document,"script","twitter-wjs");</script>  
 	</div>
	<div class="box bdr-solid bdr-uni">
		<h3 class="nopadtop">Related links</h3>
		<ul class="linklist single-multiple-list">
			<li>
				<a class="acton-tabs-link-processed" href="http://archives.anu.edu.au/">ANU Archives</a>
			</li>
			<li>
				<a class="acton-tabs-link-processed" href="http://anulib.anu.edu.au">ANU Library</a>
			</li>
			<li>
				<a class="acton-tabs-link-processed" href="http://press.anu.edu.au/">ANU Press</a>
			</li>
			<li>
				<a class="acton-tabs-link-processed" href="https://services.anu.edu.au/business-units/research-services-division">Research Services Division</a>
			</li>
		</ul>
	</div>
</anu:content>


</dspace:layout>
