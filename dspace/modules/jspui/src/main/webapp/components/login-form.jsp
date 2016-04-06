<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Component which displays a login form and associated information
  --%>
  
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
<%@ taglib prefix="anu" uri="http://www.anu.edu.au/taglib" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<script type="text/javascript">
var $a = jQuery.noConflict();

$a(document).ready(function() {
$a("#tlogin_email").focus();
});
</script> 
  
     <form name="loginform" id="loginform" method="post" class="anuform labelwide" action="<%= request.getContextPath() %>/password-login"> 
		<fieldset>
		<legend>Login</legend>
		<p>
        <label for="tlogin_email"><fmt:message key="jsp.components.login-form.email"/></label>
		<input type="text" name="login_email" id="tlogin_email" tabindex="1" class="tfull text" />
		</p>
		<p>
		<label for="tlogin_password"><fmt:message key="jsp.components.login-form.password"/></label>
		<input type="password" name="login_password" id="tlogin_password" tabindex="2" class="tfull text" />
		</p>
		<p>
		<label></label><a href="<%= request.getContextPath() %>/forgot">Forgot your password?</a>
		</p>
		</fieldset>
		<p class="text-right" style="margin-bottom: 10px;">
		<input type="submit" name="login_submit" class="btn-uni-grad" value="<fmt:message key="jsp.components.login-form.login"/>" />
		</p>
      </form>
	  <p> Any staff member (including those holding honorary status, such as Emeriti and Visitors) and Higher Degree by 
	  Research students of the University can submit material to ANU Open Research repository. Undergraduate and Postgraduate by
	  Coursework students can deposit research outputs (such as journal article, theses).</p>
	  <p>University staff can deposit work they completed before they began at ANU, but must indicate their former affiliation in the
	  "Notes" field.</p>
	  <p>The Open Research staff will check copyright permissions of your uploaded item and contact you should an alternative version
	  be required to meet publisher policies.</p>