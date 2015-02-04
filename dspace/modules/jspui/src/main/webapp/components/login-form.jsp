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
		<p>
        <label for="tlogin_email"><fmt:message key="jsp.components.login-form.email"/></label>
		<input type="text" name="login_email" id="tlogin_email" tabindex="1" class="tfull text" />
		</p>
		<p>
		<label for="tlogin_password"><fmt:message key="jsp.components.login-form.password"/></label>
		<input type="password" name="login_password" id="tlogin_password" tabindex="2" class="tfull text" />
		</p>
		</fieldset>
		<p class="text-right" style="margin-bottom: 10px;">
		<input type="submit" name="login_submit" class="btn-uni-grad" value="<fmt:message key="jsp.components.login-form.login"/>" />
		</p>
      </form>

<anu:message type="info">
        <h2>Items with restrictions</h2>
        <p>Generally all material in Digital Collections is available open access, with three exceptions:</p>
   
        <ol>
                <li>Embargos - There are a few items which are held under embargo until we can make them available.</li>
                <li>Restrictions - A very small number of items have restrictions on their availability.</li>
		<li>High resolution images - see <strong>Research Collections</strong> information at <a href="https://digitalcollections.anu.edu.au/contacts">https://digitalcollections.anu.edu.au/contacts</a>
		for information on how to purchase these images.</li>
        </ol>

        <p>If you are having trouble opening an item, please contact us at <a
        href="mailto:repository.admin@anu.edu.au">repository.admin@anu.edu.au</a>
        for assistance.</p>
</anu:message>

<br/>
<anu:message type="warn">
	<h2>Login support</h2>
	<ul>
   		<li><a href="<%= request.getContextPath() %>/forgot">Forgot your password?</a></li>
   	</ul>

</anu:message>