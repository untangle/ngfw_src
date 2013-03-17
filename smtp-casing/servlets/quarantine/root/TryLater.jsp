<%@page language="java" import="com.untangle.uvm.*"%>
<%@ taglib uri="/WEB-INF/taglibs/quarantine_euv.tld" prefix="quarantine" %>
<%--
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
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
/* <![CDATA[ */
@import url(/ext/resources/css/ext-all.css);
/* ]]> */
</style>
<link href="/skins/default/css/ext-skin.css" rel="stylesheet" type="text/css">
<link href="/skins/default/css/user.css" rel="stylesheet" type="text/css">
</head>
<body class="quarantine">
<div id="content" class="service-error-height">
	<div id="header"><a href="<%=companyUrl%>"><img src="/images/BrandingLogo.gif" border="0" alt="<%=company%> logo"/></a><div class="title">Quarantine Service Error</div></div>

	<div id="main">
	 <!-- Box Start -->
	 <!-- Content Start -->
		<!--
		<div class="page_head">
	        <a href="<%=companyUrl%>"><img src="/images/BrandingLogo.gif" border="0" alt="<%=company%> logo"/></a> <div>Quarantine service error</div>
		</div>
		
	    <hr />
	          
	  	<center>
		-->
		<div style="padding: 7em 0 0 10px;  ">

	        The <%=company%> Server has encountered an error.
	        Please try later.
	        Thanks and sorry.

	    </div>
	<!--
	    </center>        
	        
	    <address>Powered by Untangle&trade; Server</address>
	        
		<hr />
	-->	
	 <!-- Content End -->
	</div>	
</div>

</body>
</html>
