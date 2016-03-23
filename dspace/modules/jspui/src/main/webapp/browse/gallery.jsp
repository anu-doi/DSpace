
<%--
  - Populate gallery JSP
  - Allows a user to view search results as a photo gallery
  -
  - Osama Alkadi (osama.alkadi@anu.edu.au)
  -
   --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="org.dspace.app.webui.components.GalleryItem" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib prefix="anu" uri="http://www.anu.edu.au/taglib" %>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.content.*"%>
<%@ page import="org.dspace.content.Item"%>
<%@ page import="org.dspace.core.ConfigurationManager"%>


 
<anu:content layout="full">
<dspace:layout titlekey="browse.page-title">
  <link rel="stylesheet" href="<%= request.getContextPath() %>/css/lightbox.css" media="screen" type="text/css" />
  <script type="text/javascript" src="<%= request.getContextPath() %>/js/lightbox.min.js"></script>

<div class="imageRow">
    <h1>Browsing ${pageHeader} by ${type}</h1>
    <p>Showing results ${start} to ${finish} of ${total}<p>    
    <c:forEach var="item" items="${galleryItems}">
   
     <div class="single">
       <p class="sml-hdr">

       <a href="${item.url}" data-lightbox="items" data-title="&lt;a target='_self' onClick=&quot;window.location.href=&#x27;${item.handle}&#x27;&quot; 
         href='${item.handle}'&gt;${item.title}&lt;/a&gt;"> <img src="${item.url}" /></a>
  		</p>
     </div> 
	</c:forEach>
</div>
<div>
<center>
 <a href="${prev}"><fmt:message key="browse.full.prev"/></a>&nbsp;
<a href="${next}"><fmt:message key="browse.full.next"/></a>&nbsp;
</center>
</div>
<div><a href="${back}"><fmt:message key="browse.back.to"/> ${pageHeader} </a></div>
</dspace:layout>
</anu:content>


