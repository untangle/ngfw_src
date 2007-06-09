<%@ page language="java" import="com.untangle.uvm.*,com.untangle.uvm.node.*, com.untangle.uvm.security.*, com.untangle.node.http.*, com.untangle.node.phish.*"%>

<%
UvmLocalContext uvm = UvmContextFactory.context();
BrandingSettings bs = uvm.brandingManager().getBrandingSettings();
String company = bs.getCompanyName();
String companyUrl = bs.getCompanyUrl();

LocalNodeManager tman = uvm.nodeManager();

String nonce = request.getParameter("nonce");
String tidStr = request.getParameter("tid");
Tid tid = new Tid(Long.parseLong(tidStr));

NodeContext tctx = tman.nodeContext(tid);
Phish tran = (Phish)tctx.node();
UserWhitelistMode mode = tran.getUserWhitelistMode();
PhishBlockDetails bd = tran.getBlockDetails(nonce);

String header = "Phish Blocker";
String host = null == bd ? "" : bd.getFormattedHost();
String url = null == bd ? "" : bd.getFormattedUrl();
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%=company%> | Phish Blocker Warning</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link href="/main.css" rel="stylesheet" type="text/css" />
<style type="text/css">
/* <![CDATA[ */
@import url(/images/base.css);
/* ]]> */
</style>
<script type="text/javascript">
// <![CDATA[
nonce = '<%=nonce%>';
tid = '<%=tidStr%>';
url = '<%=null == bd ? "javascript:history.back()" : bd.getUrl()%>';
// ]]>
</script>
<script type="text/javascript" src="blockpage.js"></script>
</head>
<body>
<div id="main">
 <!-- Box Start -->
 <div class="main-top-left"></div><div class="main-top-right"></div><div class="main-mid-left"><div class="main-mid-right"><div class="main-mid">
 <!-- Content Start -->
	
	<div class="page_head">
        <a href="<%=companyUrl%>"><img src="/images/BrandingLogo.gif" border="0" alt="<%=company%> logo"/></a> <div><%=header%></div>
	</div>
	
    <hr />
    

	<center>
	<div style="padding: 10px 0; margin: 0 auto; width: 500px;">
		This web page was blocked because it may be designed to steal personal information.<br /><br />
		<p><b>Host:</b> <%=host%></p>
		<p><b>URL:</b> <%=url%></p>
	</div>
	</center>

	<center>
<% if (UserWhitelistMode.NONE != mode && null != bd && null != bd.getWhitelistHost()) { %>
      <button id="unblockNowButton" type="button" onclick="unblockSite(false)">Unblock For Now</button>
	<% if (UserWhitelistMode.USER_AND_GLOBAL == mode) { %>
      <button id="unblockGlobalButton" type="button" onclick="unblockSite(true)">Unblock Permanently</button>

<% 
	   }
   }
%>
  
	</center>

    <address><%=company%> Phish Blocker</address>
    
	<hr />
	
 <!-- Content End -->
 </div></div></div><div class="main-bot-left"></div><div class="main-bot-right"></div>
 <!-- Box End -->
</div>	

</body>
</html>
