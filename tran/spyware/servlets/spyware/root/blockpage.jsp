<%@ page language="java" import="com.untangle.mvvm.client.*, com.untangle.mvvm.tran.*, com.untangle.mvvm.security.*, com.untangle.tran.spyware.*"%>

<%
MvvmRemoteContext ctx = MvvmRemoteContextFactory.factory().systemLogin(0, Thread.currentThread().getContextClassLoader());
TransformManager tman = ctx.transformManager();

String nonce = request.getParameter("nonce");
String tidStr = request.getParameter("tid");
Tid tid = new Tid(Long.parseLong(tidStr));

TransformContext tctx = tman.transformContext(tid);
Spyware tran = (Spyware)tctx.transform();
UserWhitelistMode mode = tran.getUserWhitelistMode();
BlockDetails bd = tran.getBlockDetails(nonce);

String header = "Spyware Blocker";
String host = bd.getHost();
String url = bd.getUrl();
%>

<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
  <head>
    <link href="/main.css" rel="stylesheet" type="text/css"/>

<title>Untangle Spyware Blocker Warning</title>
<script language="JavaScript">
nonce = '<%=nonce%>';
tid = '<%=tidStr%>';
url = '<%=url%>';
</script>

<script type="text/javascript" src="spyware.js"></script>

  </head>

  <body>
    <table cellspacing="0" cellpadding="0" border="0" align="center" width="100%" style="padding: 20px 20px 20px 20px">
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
<a href="http://www.untangle.com"><img src="/images/Logo150x96.gif" border="0" alt="Untangle logo" width="150" height="96"/></a>
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
<td>This web page was blocked because it may contain spyware.</td>
</tr>
<tr>
<td><b>Host:</b> <%=host%></td>
</tr>
<tr>
<td><b>URL:</b> <%=url%></td>
</tr>
<tr>
<td>
  <%
  if (UserWhitelistMode.NONE != mode) {
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
<td><address>Untangle Spyware Blocker</address></td>
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

<%
MvvmRemoteContextFactory.factory().logout();
%>
