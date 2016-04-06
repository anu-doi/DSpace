<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Page that displays the email/password login form
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
    
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://www.anu.edu.au/taglib" prefix="anu" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<dspace:layout navbar="default" locbar="off" titlekey="jsp.login.password.title" nocache="true">
<anu:content layout="full">
    <h1><fmt:message key="jsp.login.password.heading"/></h1>
</anu:content>
<anu:content layout="two-third">
    <dspace:include page="/components/login-form.jsp" />
</anu:content>
<anu:content layout="one-third">
	<anu:box backgroundColour="grey" backgroundOpacity="10">
		<h2>Related guidance</h2>
		<a href="https://digitalcollections.anu.edu.au/downloads/submit_an_item.pdf">Submitting an item to the Open Research repository (PDF, 264KB)</a>
	</anu:box>
	<anu:box backgroundColour="grey" backgroundOpacity="10">
		<h2>Contact</h2>
		<div class="clear padbottom">
			<img class="hpad left" alt="Name" src="//style.anu.edu.au/_anu/images/icons/web/person.png">
			<div>General enquiries</div>
		</div>
		<div class="clear padbottom">
			<img class="hpad left" alt="Email" src="//style.anu.edu.au/_anu/images/icons/web/mail.png"/> 
			<a href="mailto:repository.admin@anu.edu.au">Send email</a>
		</div>
	</anu:box>
</anu:content>
</dspace:layout>
