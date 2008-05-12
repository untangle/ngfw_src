<%@ page language="java" import="com.untangle.uvm.*, com.untangle.uvm.security.Tid, com.untangle.uvm.node.*, com.untangle.uvm.vnet.*, com.untangle.uvm.util.SessionUtil, org.apache.log4j.helpers.AbsoluteTimeDateFormat, java.util.Properties, java.net.URL, java.io.PrintWriter, javax.naming.*" %>
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
LocalUvmContext uvm = LocalUvmContextFactory.context();

BrandingBaseSettings bs = uvm.brandingManager().getBaseSettings();
String company = bs.getCompanyName();
String companyUrl = bs.getCompanyUrl();

RemoteReportingManager reportingManager = uvm.reportingManager();

boolean reportingEnabled = reportingManager.isReportingEnabled();
boolean reportsAvailable = reportingManager.isReportsAvailable();
if (!reportsAvailable) {
%>


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%=company%> | Reports</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<style type="text/css">
/* <![CDATA[ */
@import url(/images/base.css);
/* ]]> */
</style>
</head>
<body>
<div id="main">    
 <!-- Box Start -->
 <div class="main-top-left"></div><div class="main-top-right"></div><div class="main-mid-left"><div class="main-mid-right"><div class="main-mid">
 <!-- Content Start -->
	
	
	<div class="page_head">
		<a href="<%=companyUrl%>"><img src="/images/BrandingLogo.gif" alt="<%=company%> Logo" /></a> <div><%=company%> Reports</div>
	</div>
	
	
	
    <hr />
	
	
	
	<center>
	<div style="padding: 10px 0; margin: 0 auto; width: 440px; text-align: left;">

 
        <b><i>No reports are available.</i></b>
		<br /><br />

        <% if(!reportingEnabled){ %>
            <%=company%> Reports is not installed into your rack or it is not turned on.<br />
            Reports are only generated when <%=company%> Reports is installed and turned on.
        <% } else{ %>
            When daily, weekly, and/or monthly Reports are scheduled,
            please check back the morning after the scheduled day
            for daily, weekly, and/or monthly Reports.<br />
            <br />
            When scheduled, <%=company%> Reports automatically generates
            the requested Reports during the preceeding night.
        <% } %>

   
		
	</div>
	</center>

	
	
	<hr />

	
 <!-- Content End -->
 </div></div></div><div class="main-bot-left"></div><div class="main-bot-right"></div>
 <!-- Box End -->
</div>	

</body>
</html>

<%
} else {
   // We can redirect them. If it fails (shouldn't with any modern
   // browser), serve them the following backup page.
   response.sendRedirect("./current");
%>

<html><head><title><%=company%> Reports</title>
<style><!---
H1{font-family : sans-serif,Arial,Tahoma;color : white;
  background-color : #0086b2;}
BODY{font-family : sans-serif,Arial,Tahoma;color : black;
  background-color : white;}
B{color : white;background-color : #0086b2;} HR{color : #0086b2;}
--></style></head><body>
<h1><%=company%> Reports - HTTP Status 302 - Moved Temporarily</h1>
<hr size="1" noshade><p><b>type</b> Status report</p>
<p><b>message</b> <u>Moved Temporarily</u></p>
<p><b>description</b>
<u>The requested resource (Moved Temporarily) has moved temporarily
to a new location.</u></p>
<hr size="1" noshade>
</body></html>

<%
}
%>

