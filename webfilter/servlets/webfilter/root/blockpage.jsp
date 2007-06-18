<%@ page language="java" import="com.untangle.uvm.*, com.untangle.uvm.node.*, com.untangle.uvm.security.*, com.untangle.node.webfilter.*"%>

<%
LocalUvmContext uvm = LocalUvmContextFactory.context();
BrandingSettings bs = uvm.brandingManager().getBrandingSettings();
String company = bs.getCompanyName();
String companyUrl = bs.getCompanyUrl();

LocalNodeManager tman = uvm.nodeManager();

String nonce = request.getParameter("nonce");
String tidStr = request.getParameter("tid");
Tid tid = new Tid(Long.parseLong(tidStr));

NodeContext tctx = tman.nodeContext(tid);
WebFilter tran = (WebFilter)tctx.node();
WebFilterBlockDetails bd = tran.getDetails(nonce);

String header = null == bd ? "" : bd.getHeader();
String contact = null == bd ? "your administrator" : bd.getContact();
String host = null == bd ? "" : bd.getFormattedHost();
String url = null == bd ? "" : bd.getFormattedUrl();
String reason = null == bd ? "" : bd.getReason();
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%=company%> | Web Filter Warning</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<style type="text/css">
/* <![CDATA[ */
@import url(/images/base.css);
/* ]]> */
</style>
<script language="JavaScript">
// <![CDATA[
nonce = '<%=nonce%>';
tid = '<%=tidStr%>';
url = '<%=null == bd ? "javascript:history.back()" : bd.getUrl()%>';
// ]]>
</script>
</head>
<body>
<div id="main">    
<!-- Box Start -->
 <div class="main-top-left"></div><div class="main-top-right"></div><div class="main-mid-left"><div class="main-mid-right"><div class="main-mid">
 <!-- Content Start -->
	
	
	<div class="page_head">
		<a href="<%=companyUrl%>"><img src="/images/BrandingLogo.gif" border="0" alt="<%=company%>" /></a> 
		<div><%=header%></div>
	</div>

    <hr />

	
<center>
<div style="padding: 10px 0; margin: 0 auto; width: 500px;">
This web page was blocked because it is considered inappropriate.<br /><br />
<p><b>Host:</b> <%=host%></p>
<p><b>URL:</b> <%=url%></p>
<p><b>Reason:</b> <%=reason%></p>
<p>Please contact <%=contact%>.</p>
</div>
</center>

	<br />
	<address><%=company%> Web Filter</address>
	<hr />

 <!-- Content End -->
 </div></div></div><div class="main-bot-left"></div><div class="main-bot-right"></div>
 <!-- Box End -->
</div>	
</body>
</html>
