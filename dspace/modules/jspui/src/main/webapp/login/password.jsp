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

<anu:content layout="doublenarrow">
<dspace:layout navbar="default" locbar="off" titlekey="jsp.login.password.title" nocache="true">
    <h1><fmt:message key="jsp.login.password.heading"/></h1>
    <dspace:include page="/components/login-form.jsp" />
</dspace:layout>
</anu:content>
