<%@page language="java" import="com.untangle.uvm.*"%>

<%
UvmLocalContext uvm = UvmContextFactory.context();
BrandingSettings bs = uvm.brandingManager().getBrandingSettings();
String company = bs.getCompanyName();
String companyUrl = bs.getCompanyUrl();
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%=company%> | AD Download Utility</title>
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
        <a href="<%=companyUrl%>"><img src="/images/BrandingLogo.gif" border="0" alt="<%=company%> logo"/></a> <div>Active Directory Lookup Server<br />Download Utility</div>
	</div>
	
	
    <hr />
	
	
	
	<center>
	<div style="padding: 10px 0; margin: 0 auto; width: 440px;">

	    <span class="page_sub_title">Download</span>
		<br />

	     <a href="<%= response.encodeURL( "setup.exe" ) %>"><b>Windows Installer</b></a>
		
	</div>
	</center>

	
	<hr />
	
 <!-- Content End -->
 </div></div></div><div class="main-bot-left"></div><div class="main-bot-right"></div>
 <!-- Box End -->
</div>	

</body>
</html>
