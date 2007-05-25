<%@ page language="java" import="com.untangle.mvvm.*,com.untangle.mvvm.tran.*, com.untangle.mvvm.security.*, com.untangle.tran.http.*, com.untangle.tran.clamphish.*"%>

<%
MvvmLocalContext mvvm = MvvmContextFactory.context();
BrandingSettings bs = mvvm.brandingManager().getBrandingSettings();
String company = bs.getCompanyName();
String companyUrl = bs.getCompanyUrl();

LocalTransformManager tman = mvvm.transformManager();

String nonce = request.getParameter("nonce");
String tidStr = request.getParameter("tid");
Tid tid = new Tid(Long.parseLong(tidStr));

TransformContext tctx = tman.transformContext(tid);
ClamPhish tran = (ClamPhish)tctx.transform();
UserWhitelistMode mode = tran.getUserWhitelistMode();
ClamPhishBlockDetails bd = tran.getBlockDetails(nonce);

String header = "Phish Blocker";
String host = null == bd ? "" : bd.getFormattedHost();
String url = null == bd ? "" : bd.getFormattedUrl();
%>

<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
  <head>
    <link href="/main.css" rel="stylesheet" type="text/css"/>

<title><%=company%> Phish Blocker Warning</title>
<script language="JavaScript">
nonce = '<%=nonce%>';
tid = '<%=tidStr%>';
url = '<%=null == bd ? "javascript:history.back()" : bd.getUrl()%>';
</script>

<script type="text/javascript" src="blockpage.js"></script>

</head>

  <body style="margin: 20px 20px 20px 20px">
    <table cellspacing="0" cellpadding="0" border="0" align="center" width="100%">
      <tbody>
        <tr>
          <td id="table_main_top_left"><img src="/images/background/spacer.gif" alt=" " height="23" width="23"/></td>
          <td id="table_main_top" width="100%"><img src="/images/background/spacer.gif" alt=" " height="1" width="1"/></td>
          <td id="table_main_top_right"> <img src="/images/background/spacer.gif" alt=" " height="23" width="23"/></td>
        </tr>
        <tr>
          <td id="table_main_left"><img src="/images/background/spacer.gif" alt=" " height="1" width="1"/></td>
          <td id="table_main_center">

<table>
<tbody>
<tr>
  <td colspan=1 width="154px">
<a href="<%=companyUrl%>"><img src="/images/BrandingLogo.gif" border="0" alt="<%=company%>" width="150" height="96"/></a>
  </td>
  <td style="padding: 0px 0px 0px 10px" class="page_header_title" align="left" valign="middle">
  <%=header%>
  </td>
</tr>
</tbody>
</table>

<table width="100%">
<tbody>
<tr>
<td><hr width="100%" size="1" color="#969696"/></td>
</tr>
<tr>
<td>This web page was blocked because it may be designed to steal personal information.</td>
</tr>
<tr>
<td><p><b>Host:</b> <%=host%></p></td>
</tr>
<tr>
<td><p><b>URL:</b> <%=url%></p></td>
</tr>
<tr>
<td>
  <%
  if (UserWhitelistMode.NONE != mode && null != bd.getWhitelistHost()) {
  %>
      <input id="unblockNowButton" type="button" value="Unblock For Now"
      onclick="unblockSite(false)"/>
  <%
  if (UserWhitelistMode.USER_AND_GLOBAL == mode) {
  %>
      <input id="unblockGlobalButton" type="button" value="Unblock Permanently"
      onclick="unblockSite(true)"/>
  <%
  }
  }
  %>
</td>
</tr>
</tbody>

<tfoot>
<tr>
<td><hr width="100%" size="1" color="#969696"/></td>
</tr>
<tr>
<td><address><%=company%> Phish Blocker</address></td>
</tr>
</tfoot>
</table>

          </td>
          <td id="table_main_right"><img src="/images/background/spacer.gif" alt=" " height="1" width="1"/></td>
        </tr>
        <tr>
          <td id="table_main_bottom_left"><img src="/images/background/spacer.gif" alt=" " height="23" width="23"/></td>
          <td id="table_main_bottom"><img src="/images/background/spacer.gif" alt=" " height="1" width="1"/></td>
          <td id="table_main_bottom_right"> <img src="/images/background/spacer.gif" alt=" " height="23" width="23"/></td>
          </td>
        </tr>
      </tbody>
    </table>
  </body>
</html>
