<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Display item/collection/community statistics
  -
  - Attributes:
  -    statsVisits - bean containing name, data, column and row labels
  -    statsMonthlyVisits - bean containing name, data, column and row labels
  -    statsFileDownloads - bean containing name, data, column and row labels
  -    statsCountryVisits - bean containing name, data, column and row labels
  -    statsCityVisits - bean containing name, data, column and row labels
  -    isItem - boolean variable, returns true if the DSO is an Item 
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.anu.edu.au/taglib" prefix="anu" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.text.DateFormatSymbols" %>

<%@page import="org.dspace.app.webui.servlet.MyDSpaceServlet"%>

<% Boolean isItem = (Boolean)request.getAttribute("isItem"); %>

<%
		Calendar cal = Calendar.getInstance();
		int currentYear = cal.get(Calendar.YEAR);
		int currentMonth = cal.get(Calendar.MONTH);
		int currentDay = cal.get(Calendar.DAY_OF_MONTH);
		
		int startDay = 0;
		int startMonth = -1;
		int startYear = 0;
		int endDay = 0;
		int endMonth = -1;
		int endYear = 0;
		
		
		String sDay = request.getParameter("sDay");
		if (sDay != null) {
			startDay = Integer.parseInt(sDay);
		}
		String sMonth = request.getParameter("sMonth");
		if (sMonth != null) {
			startMonth = Integer.parseInt(sMonth);
		}
		String sYear = request.getParameter("sYear");
		if (sYear != null) {
			startYear = Integer.parseInt(sYear);
		}
		
		String eDay = request.getParameter("eDay");
		if (eDay != null) {
			endDay = Integer.parseInt(eDay);
		}
		String eMonth = request.getParameter("eMonth");
		if (eMonth != null) {
			endMonth = Integer.parseInt(eMonth);
		}
		String eYear = request.getParameter("eYear");
		if (eYear != null) {
			endYear = Integer.parseInt(eYear);
		}
		
		String[] monthsList = new DateFormatSymbols().getMonths();
%>


<anu:content layout="doublewide">
<dspace:layout titlekey="jsp.statistics.title">
<h1><fmt:message key="jsp.statistics.title"/> 
<c:if test="${not empty title}">for</c:if> 
<c:if test="${not empty handle}">
	<a href="<%= request.getContextPath() %>/handle/${handle}">${title}</a>
</c:if>
<c:if test="${empty handle}">
	${title}
</c:if>
</h1>
<form id="updateDate" method="get" class="box bdr-solid bdr-uni">
<h2>Set statistics limitations</h2>
<p>
	Start Date
	<select id="sDay" name="sDay">
		<c:forEach var="i" begin="1" end="31">
			<option value="${i}"
			<c:if test="${i == param.sDay}">
				selected="selected"
			</c:if>
			>${i}</option>
		</c:forEach>
	</select>
	<select id="sMonth" name="sMonth">
		<% 
		for (int i = 0; i < monthsList.length; i++) {
			out.println("<option value=\""+i+"\"");
			
			if (startMonth == i) {
				out.println("selected=\"selected\"");
			}
			out.println(">"+monthsList[i]+"</option>");
		}
		%>
	</select>
	<select id="sYear" name="sYear">
	<% 
		for (int i = 2011; i < currentYear + 1; i++) {
			out.println("<option value=\""+i+"\"");
			if (startYear == i) {
				out.println(" selected=\"selected\"");
			}
			out.println(">"+i+"</option>");
		}
	%>
	</select>
</p>
<p>
	End Date
	<select id="eDay" name="eDay">
		<%
			for (int i = 1; i <= 31; i++) {
				out.println("<option value=\""+i+"\"");
				if (endDay == i || endDay == 0 && i == currentDay) {
					out.println("selected=\"selected\"");
				}
				out.println(">"+i+"</option>");
			}
		%>
	</select>
	<select id="eMonth" name="eMonth">
		<% 
		for (int i = 0; i < monthsList.length; i++) {
			out.println("<option value=\""+i+"\"");
			if (endMonth == i || endMonth == -1 && i == currentMonth) {
				out.println(" selected=\"selected\"");
			}
			out.println(">"+monthsList[i]+"</option>");
		}
		%>
	</select>
	<select id="eYear" name="eYear">
	<% 
		for (int i = 2011; i < currentYear + 1; i++) {
			out.println("<option value=\""+i+"\"");
			if (endYear == i  || endYear == 0 && i == currentYear) {
				out.println(" selected=\"selected\"");
			}
			out.println(">"+i+"</option>");
		}
	%>
	</select>
</p>
<p>
Order Results By:
<select id="orderGeo" name="orderGeo">
	<option value="0" <c:if test="${param.orderGeo == 0}">selected="selected"</c:if>>Views</option>
	<option value="1" <c:if test="${param.orderGeo == 1}">selected="selected"</c:if>>Downloads</option>
</select>
</p>
<p>
Limit results to top <input type="text" id="limit" name="limit" value="${param.limit}" />
</p>
<p>
IP Ranges 
<select name="ipRanges">
	<option value="">All</option>
	<option value="external" <c:if test="${param.ipRanges == 'external'}">selected="selected"</c:if>>External</option>
	<option value="internal" <c:if test="${param.ipRanges == 'internal'}">selected="selected"</c:if>>Internal</option>
</select>
</p>
<div>
	<input id="author" name="author" type="hidden" value="${param.author}"/>
	<input id="updateLimits" name="updateLimits" type="submit" value="Update"/>
</div>
</form>

<h2><fmt:message key="jsp.statistics.heading.visits"/></h2>
<table class="statsTable">
<tr>
<c:forEach var="colLabel" items="${statsVisits.colLabels}" varStatus="labelCounter">
	<th><c:out value="${colLabel}" /></th>
</c:forEach>
</tr>
<c:forEach var="row" items="${statsVisits.matrix}" varStatus="rowCounter">
	<tr>
	<c:forEach var="col" items="${row}" varStatus="colCounter">
		<td><c:out value="${not empty col ? col : 0}" /></td>
	</c:forEach>
	</tr>
</c:forEach>
</table>

<c:if test="${not empty statsCountryVisits}">
	<c:url var="countryVisitsUrl" value="">
		<c:param name="sDay" value="${param.sDay}" />
		<c:param name="sMonth" value="${param.sMonth}" />
		<c:param name="sYear" value="${param.sYear}" />
		<c:param name="eDay" value="${param.eDay}" />
		<c:param name="eMonth" value="${param.eMonth}" />
		<c:param name="eYear" value="${param.eYear}" />
		<c:param name="ipRanges" value="${param.ipRanges}" />
		<c:param name="orderGeo" value="${param.orderGeo}"/>
		<c:param name="author" value="${param.author}"/>
		<c:param name="limit" value="${param.limit}"/>
		<c:param name="format" value="csv" />
		<c:param name="section" value="statsCountryVisits" />
	</c:url>
	<h2><fmt:message key="jsp.statistics.heading.countryvisits"/> <a href="${countryVisitsUrl}" title="Download statistics as a CSV file"><span class="small small glyphicon glyphicon-download-alt"></span></a></h2>
	<table class="statsTable">
	<tr>
		<th></th>
	<c:forEach var="colLabel" items="${statsCountryVisits.colLabels}">
		<th><c:out value="${colLabel}" /></th>
	</c:forEach>
	</tr>
	<c:forEach var="row" items="${statsCountryVisits.matrix}" varStatus="rowCounter">
		<tr>
			<th>
				<c:out value="${statsCountryVisits.rowLabels[rowCounter.index]}" />
			</th>
		<c:forEach var="col" items="${row}" varStatus="colCounter">
			<td><c:out value="${not empty col ? col : 0}" /></td>
		</c:forEach>
		</tr>
	</c:forEach>
	</table>
</c:if>

<c:if test="${not empty statsTopAuthors}">
	<c:url var="topAuthorVisitsUrl" value="">
		<c:param name="sDay" value="${param.sDay}" />
		<c:param name="sMonth" value="${param.sMonth}" />
		<c:param name="sYear" value="${param.sYear}" />
		<c:param name="eDay" value="${param.eDay}" />
		<c:param name="eMonth" value="${param.eMonth}" />
		<c:param name="eYear" value="${param.eYear}" />
		<c:param name="ipRanges" value="${param.ipRanges}" />
		<c:param name="orderGeo" value="${param.orderGeo}"/>
		<c:param name="author" value="${param.author}"/>
		<c:param name="limit" value="${param.limit}"/>
		<c:param name="format" value="csv" />
		<c:param name="section" value="statsTopAuthors" />
	</c:url>
	<h2><fmt:message key="jsp.statistics.heading.topauthors"/> <a href="${topAuthorVisitsUrl}" title="Download statistics as a CSV file"><span class="small small glyphicon glyphicon-download-alt"></span></a></h2>
	<table class="statsTable">
	<tr>
		<th></th>
	<c:forEach var="colLabel" items="${statsTopAuthors.colLabels}">
		<th><c:out value="${colLabel}" /></th>
	</c:forEach>
	</tr>
	<c:forEach var="row" items="${statsTopAuthors.matrix}" varStatus="rowCounter">
		<tr>
			<th>
				<c:out value="${statsTopAuthors.rowLabels[rowCounter.index]}" />
			</th>
		<c:url value="/statistics" var="authorUrl">
			<c:param name="author" value="${statsTopAuthors.rowLabels[rowCounter.index]}" />
		</c:url>
		<c:forEach var="col" items="${row}" varStatus="colCounter">
			<td><a href="${authorUrl}"><c:out value="${not empty col ? col : 0}" /></a></td>
		</c:forEach>
		</tr>
	</c:forEach>
	</table>
</c:if>

<c:if test="${not empty statsTopItems}">
	<c:url var="topItemsUrl" value="">
		<c:param name="sDay" value="${param.sDay}" />
		<c:param name="sMonth" value="${param.sMonth}" />
		<c:param name="sYear" value="${param.sYear}" />
		<c:param name="eDay" value="${param.eDay}" />
		<c:param name="eMonth" value="${param.eMonth}" />
		<c:param name="eYear" value="${param.eYear}" />
		<c:param name="ipRanges" value="${param.ipRanges}" />
		<c:param name="orderGeo" value="${param.orderGeo}"/>
		<c:param name="author" value="${param.author}"/>
		<c:param name="limit" value="${param.limit}"/>
		<c:param name="format" value="csv" />
		<c:param name="section" value="statsTopItems" />
	</c:url>
	<h2><fmt:message key="jsp.statistics.heading.topitems"/> <a href="${topItemsUrl}" title="Download statistics as a CSV file"><span class="small small glyphicon glyphicon-download-alt"></span></a></h2>
	<table class="statsTable">
	<tr>
		<th></th>
	<c:forEach var="colLabel" items="${statsTopItems.colLabels}">
		<th><c:out value="${colLabel}" /></th>
	</c:forEach>
	</tr>
	<c:forEach var="row" items="${statsTopItems.matrix}" varStatus="rowCounter">
		<tr>
			<th>
				<c:out value="${statsTopItems.rowLabels[rowCounter.index]}" />
			</th>
		<c:forEach var="col" items="${row}" varStatus="colCounter">
			<td><c:out value="${not empty col ? col : 0}" /></td>
		</c:forEach>
		</tr>
	</c:forEach>
	</table>
</c:if>

<c:if test="${not empty statsTopCollections}">
	<c:url var="topCollsUrl" value="">
		<c:param name="sDay" value="${param.sDay}" />
		<c:param name="sMonth" value="${param.sMonth}" />
		<c:param name="sYear" value="${param.sYear}" />
		<c:param name="eDay" value="${param.eDay}" />
		<c:param name="eMonth" value="${param.eMonth}" />
		<c:param name="eYear" value="${param.eYear}" />
		<c:param name="ipRanges" value="${param.ipRanges}" />
		<c:param name="orderGeo" value="${param.orderGeo}"/>
		<c:param name="author" value="${param.author}"/>
		<c:param name="limit" value="${param.limit}"/>
		<c:param name="format" value="csv" />
		<c:param name="section" value="statsTopCollections" />
	</c:url>
	<h2><fmt:message key="jsp.statistics.heading.topcollections"/> <a href="${topCollsUrl}" title="Download statistics as a CSV file"><span class="small small glyphicon glyphicon-download-alt"></span></a></h2>
	<table class="statsTable">
	<tr>
		<th></th>
	<c:forEach var="colLabel" items="${statsTopCollections.colLabels}">
		<th><c:out value="${colLabel}" /></th>
	</c:forEach>
	</tr>
	<c:forEach var="row" items="${statsTopCollections.matrix}" varStatus="rowCounter">
		<tr>
			<th>
				<c:out value="${statsTopCollections.rowLabels[rowCounter.index]}" />
			</th>
		<c:forEach var="col" items="${row}" varStatus="colCounter">
			<td><c:out value="${not empty col ? col : 0}" /></td>
		</c:forEach>
		</tr>
	</c:forEach>
	</table>
</c:if>

<c:if test="${not empty statsTopDownloads}">
	<c:url var="topDownloadsUrl" value="">
		<c:param name="sDay" value="${param.sDay}" />
		<c:param name="sMonth" value="${param.sMonth}" />
		<c:param name="sYear" value="${param.sYear}" />
		<c:param name="eDay" value="${param.eDay}" />
		<c:param name="eMonth" value="${param.eMonth}" />
		<c:param name="eYear" value="${param.eYear}" />
		<c:param name="ipRanges" value="${param.ipRanges}" />
		<c:param name="orderGeo" value="${param.orderGeo}"/>
		<c:param name="author" value="${param.author}"/>
		<c:param name="limit" value="${param.limit}"/>
		<c:param name="format" value="csv" />
		<c:param name="section" value="statsTopDownloads" />
	</c:url>
	<h2><fmt:message key="jsp.statistics.heading.filedownloads"/> <a href="${topDownloadsUrl}" title="Download statistics as a CSV file"><span class="small small glyphicon glyphicon-download-alt"></span></a></h2>
	<table class="statsTable">
	<tr>
		<th></th>
		<th><c:out value="${statsTopDownloads.colLabels[1]}" /></th>
	</tr>
	<c:forEach var="row" items="${statsTopDownloads.matrix}" varStatus="rowCounter">
		<tr>
			<th>
				<a href="<%= request.getContextPath() %>/handle/${row[0]}"><c:out value="${statsTopDownloads.rowLabels[rowCounter.index]}" /></a>
			</th>
			<td><c:out value="${row[1]}" /></td>
		</tr>
	</c:forEach>
	</table>
</c:if>

<c:if test="${not empty statsReferralSources}">
	<c:url var="referralSourceUrl" value="">
		<c:param name="sDay" value="${param.sDay}" />
		<c:param name="sMonth" value="${param.sMonth}" />
		<c:param name="sYear" value="${param.sYear}" />
		<c:param name="eDay" value="${param.eDay}" />
		<c:param name="eMonth" value="${param.eMonth}" />
		<c:param name="eYear" value="${param.eYear}" />
		<c:param name="ipRanges" value="${param.ipRanges}" />
		<c:param name="orderGeo" value="${param.orderGeo}"/>
		<c:param name="author" value="${param.author}"/>
		<c:param name="limit" value="${param.limit}"/>
		<c:param name="format" value="csv" />
		<c:param name="section" value="statsReferralSources" />
	</c:url>
	<h2><fmt:message key="jsp.statistics.heading.referralSources"/> <a href="${referralSourceUrl}" title="Download statistics as a CSV file"><span class="small small glyphicon glyphicon-download-alt"></span></a></h2>
	<table class="statsTable">
	<tr>
		<th></th>
	<c:forEach var="colLabel" items="${statsReferralSources.colLabels}">
		<th><c:out value="${colLabel}" /></th>
	</c:forEach>
	</tr>
	<c:forEach var="row" items="${statsReferralSources.matrix}" varStatus="rowCounter">
		<tr>
			<th>
				<c:out value="${statsReferralSources.rowLabels[rowCounter.index]}" />
			</th>
		<c:forEach var="col" items="${row}" varStatus="colCounter">
			<td><c:out value="${not empty col ? col : 0}" /></td>
		</c:forEach>
		</tr>
	</c:forEach>
	</table>
</c:if>

<c:url var="monthlyVisitsUrl" value="">
	<c:param name="sDay" value="${param.sDay}" />
	<c:param name="sMonth" value="${param.sMonth}" />
	<c:param name="sYear" value="${param.sYear}" />
	<c:param name="eDay" value="${param.eDay}" />
	<c:param name="eMonth" value="${param.eMonth}" />
	<c:param name="eYear" value="${param.eYear}" />
	<c:param name="ipRanges" value="${param.ipRanges}" />
	<c:param name="orderGeo" value="${param.orderGeo}"/>
	<c:param name="author" value="${param.author}"/>
	<c:param name="limit" value="${param.limit}"/>
	<c:param name="format" value="csv" />
	<c:param name="section" value="statsMonthlyVisits" />
</c:url>
<h2><fmt:message key="jsp.statistics.heading.monthlyvisits"/> <a href="${monthlyVisitsUrl}" title="Download statistics as a CSV file"><span class="small small glyphicon glyphicon-download-alt"></span></a></h2>
<table class="statsTable">
<tr>
	<th></th>
<c:forEach var="colLabel" items="${statsMonthlyVisits.colLabels}">
	<th><c:out value="${colLabel}" /></th>
</c:forEach>
</tr>
<c:forEach var="row" items="${statsMonthlyVisits.matrix}" varStatus="rowCounter">
	<tr>
		<th>
			<c:out value="${statsMonthlyVisits.rowLabels[rowCounter.index]}" />
		</th>
	<c:forEach var="col" items="${row}" varStatus="colCounter">
		<td><c:out value="${col}" /></td>
	</c:forEach>
	</tr>
</c:forEach>
</table>

<c:if test="${not empty statsNewByCollection}">
	
	<c:url var="newByCollUrl" value="">
		<c:param name="sDay" value="${param.sDay}" />
		<c:param name="sMonth" value="${param.sMonth}" />
		<c:param name="sYear" value="${param.sYear}" />
		<c:param name="eDay" value="${param.eDay}" />
		<c:param name="eMonth" value="${param.eMonth}" />
		<c:param name="eYear" value="${param.eYear}" />
		<c:param name="ipRanges" value="${param.ipRanges}" />
		<c:param name="orderGeo" value="${param.orderGeo}"/>
		<c:param name="author" value="${param.author}"/>
		<c:param name="limit" value="${param.limit}"/>
		<c:param name="format" value="csv" />
		<c:param name="section" value="statsNewByCollection" />
	</c:url>
	<h2><fmt:message key="jsp.statistics.heading.newbycollection"/> <a href="${newByCollUrl}" title="Download statistics as a CSV file"><span class="small small glyphicon glyphicon-download-alt"></span></a></h2>
	<table class="statsTable">
	<tr>
		<th></th>
		<th><c:out value="${statsNewByCollection.colLabels[0]}" /></th>
	</tr>
	<c:forEach var="row" items="${statsNewByCollection.matrix}" varStatus="rowCounter">
		<tr>
			<th>
				<c:out value="${statsNewByCollection.rowLabels[rowCounter.index]}" />
			</th>
			<td><c:out value="${row[0]}" /></td>
		</tr>
	</c:forEach>
	</table>
</c:if>

<c:if test="${not empty statsNewByType}">
	
	<c:url var="newByTypeUrl" value="">
		<c:param name="sDay" value="${param.sDay}" />
		<c:param name="sMonth" value="${param.sMonth}" />
		<c:param name="sYear" value="${param.sYear}" />
		<c:param name="eDay" value="${param.eDay}" />
		<c:param name="eMonth" value="${param.eMonth}" />
		<c:param name="eYear" value="${param.eYear}" />
		<c:param name="ipRanges" value="${param.ipRanges}" />
		<c:param name="orderGeo" value="${param.orderGeo}"/>
		<c:param name="author" value="${param.author}"/>
		<c:param name="limit" value="${param.limit}"/>
		<c:param name="format" value="csv" />
		<c:param name="section" value="statsNewByType" />
	</c:url>
	<h2><fmt:message key="jsp.statistics.heading.newbytype"/> <a href="${newByTypeUrl}" title="Download statistics as a CSV file"><span class="small small glyphicon glyphicon-download-alt"></span></a></h2>
	<table class="statsTable">
	<tr>
		<th></th>
		<th><c:out value="${statsNewByType.colLabels[0]}" /></th>
	</tr>
	<c:forEach var="row" items="${statsNewByType.matrix}" varStatus="rowCounter">
		<tr>
			<th>
				<c:out value="${statsNewByType.rowLabels[rowCounter.index]}" />
			</th>
			<td><c:out value="${row[0]}" /></td>
		</tr>
	</c:forEach>
	</table>
</c:if>



<c:if test="${not empty statsItemCount}">
	
	<c:url var="itemCountUrl" value="">
		<c:param name="sDay" value="${param.sDay}" />
		<c:param name="sMonth" value="${param.sMonth}" />
		<c:param name="sYear" value="${param.sYear}" />
		<c:param name="eDay" value="${param.eDay}" />
		<c:param name="eMonth" value="${param.eMonth}" />
		<c:param name="eYear" value="${param.eYear}" />
		<c:param name="ipRanges" value="${param.ipRanges}" />
		<c:param name="orderGeo" value="${param.orderGeo}"/>
		<c:param name="author" value="${param.author}"/>
		<c:param name="limit" value="${param.limit}"/>
		<c:param name="format" value="csv" />
		<c:param name="section" value="statsItemCount" />
	</c:url>
	<h2><fmt:message key="jsp.statistics.heading.itemcount"/> <a href="${itemCountUrl}" title="Download statistics as a CSV file"><span class="small small glyphicon glyphicon-download-alt"></span></a></h2>
	<table class="statsTable">
	<c:forEach var="row" items="${statsItemCount.matrix}" varStatus="rowCounter">
		<tr>
			<th>
				<c:out value="${statsItemCount.rowLabels[rowCounter.index]}" />
			</th>
			<td><c:out value="${row[0]}" /></td>
		</tr>
	</c:forEach>
	</table>
</c:if>
</dspace:layout>
</anu:content>