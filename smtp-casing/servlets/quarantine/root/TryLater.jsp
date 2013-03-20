<%@page language="java" import="com.untangle.uvm.*"%>
<%@ taglib uri="/WEB-INF/taglibs/quarantine_euv.tld" prefix="quarantine" %>
<%--
 * $Id$
--%>

<%                    
UvmContext uvm = UvmContextFactory.context();
String company = uvm.brandingManager().getCompanyName();
String companyUrl = uvm.brandingManager().getCompanyUrl();
%>            
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%=company%> | Try Later</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<style type="text/css">
    @import "/ext4/resources/css/ext-all-gray.css";
    @import "/skins/default/css/user.css";
</style>
</head>
<body class="quarantine">
<div id="content" class="service-error-height">
	<div id="header"><a href="<%=companyUrl%>"><img src="/images/BrandingLogo.gif" border="0" alt="<%=company%> logo"/></a><div class="title">Quarantine Service Error</div></div>
	<div id="main">
		<div style="padding: 7em 0 0 10px;  ">
	        The <%=company%> Server has encountered an error.
	        Please try later.
	        Thanks and sorry.
	    </div>
	</div>	
</div>
</body>
</html>
