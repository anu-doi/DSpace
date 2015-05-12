<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://www.anu.edu.au/taglib" prefix="anu" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ page import="java.util.Calendar" %>
<%@ page import="java.text.DateFormatSymbols" %>

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
<dspace:layout style="submission" title="jsp.statistics.report.title">

<form id="updateDate" class="box bdr-solid bdr-uni">
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
<div>
	<input id="updateLimits" name="updateLimits" type="submit" value="Update"/>
</div>
</form>

<c:url var="addedByCollectionUrl" value="">
	<c:param name="sDay" value="${param.sDay}" />
	<c:param name="sMonth" value="${param.sMonth}" />
	<c:param name="sYear" value="${param.sYear}" />
	<c:param name="eDay" value="${param.eDay}" />
	<c:param name="eMonth" value="${param.eMonth}" />
	<c:param name="eYear" value="${param.eYear}" />
	<c:param name="format" value="csv" />
	<c:param name="section" value="statsNewItemsCollection" />
</c:url>
<h2>New Items Added By Collection <a href="${addedByCollectionUrl}" title="Download statistics as a CSV file"><span class="small small glyphicon glyphicon-download-alt"></span></a></h2>
<table class="statsTable">
<c:forEach var="row" items="${statsNewItemsCollection.matrix}" varStatus="rowCounter">
	<tr>
		<th>
			<c:out value="${statsNewItemsCollection.rowLabels[rowCounter.index]}" />
		</th>
	<c:forEach var="col" items="${row}" varStatus="colCounter">
		<td><c:out value="${not empty col ? col : 0 }" /></td>
	</c:forEach>
	</tr>
</c:forEach>
</table>

<c:url var="addedByTypeUrl" value="">
	<c:param name="sDay" value="${param.sDay}" />
	<c:param name="sMonth" value="${param.sMonth}" />
	<c:param name="sYear" value="${param.sYear}" />
	<c:param name="eDay" value="${param.eDay}" />
	<c:param name="eMonth" value="${param.eMonth}" />
	<c:param name="eYear" value="${param.eYear}" />
	<c:param name="format" value="csv" />
	<c:param name="section" value="statsNewItemsType" />
</c:url>
<h2>New Items By Type <a href="${addedByTypeUrl}" title="Download statistics as a CSV file"><span class="small small glyphicon glyphicon-download-alt"></span></a></h2>
<table class="statsTable">
<c:forEach var="row" items="${statsNewItemsType.matrix}" varStatus="rowCounter">
	<tr>
		<th>
			<c:out value="${statsNewItemsType.rowLabels[rowCounter.index]}" />
		</th>
	<c:forEach var="col" items="${row}" varStatus="colCounter">
		<td><c:out value="${not empty col ? col : 0 }" /></td>
	</c:forEach>
	</tr>
</c:forEach>
</table>

<c:url var="itemCountUrl" value="">
	<c:param name="sDay" value="${param.sDay}" />
	<c:param name="sMonth" value="${param.sMonth}" />
	<c:param name="sYear" value="${param.sYear}" />
	<c:param name="eDay" value="${param.eDay}" />
	<c:param name="eMonth" value="${param.eMonth}" />
	<c:param name="eYear" value="${param.eYear}" />
	<c:param name="format" value="csv" />
	<c:param name="section" value="statsItemCounts" />
</c:url>
<h2>Number of Items By Community and Collection <a href="${itemCountUrl}" title="Download statistics as a CSV file"><span class="small small glyphicon glyphicon-download-alt"></span></a></h2>
<table class="statsTable">
<c:forEach var="row" items="${statsItemCounts.matrix}" varStatus="rowCounter">
	<tr>
		<th>
			<c:out value="${statsItemCounts.rowLabels[rowCounter.index]}" />
		</th>
	<c:forEach var="col" items="${row}" varStatus="colCounter">
		<td><c:out value="${not empty col ? col : 0 }" /></td>
	</c:forEach>
	</tr>
</c:forEach>
</table>
	
</dspace:layout>
</anu:content>