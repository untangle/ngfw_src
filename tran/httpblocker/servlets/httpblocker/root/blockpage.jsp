<%@ page language="java" import="com.untangle.mvvm.client.*, com.untangle.mvvm.tran.*, com.untangle.mvvm.security.*, com.untangle.tran.httpblocker.*"%>

<%
MvvmRemoteContext ctx = MvvmRemoteContextFactory.factory().systemLogin(0, Thread.currentThread().getContextClassLoader());
TransformManager tman = ctx.transformManager();

String nonce = request.getParameter("nonce");
String tidStr = request.getParameter("tid");
Tid tid = new Tid(Long.parseLong(tidStr));

TransformContext tctx = tman.transformContext(tid);
HttpBlocker tran = (HttpBlocker)tctx.transform();
BlockDetails bd = tran.getDetails(nonce);

String header = bd.getHeader();
String contact = bd.getContact();
String host = bd.getHost();
String uri = bd.getUri().toString();
String reason = bd.getReason();
String url = bd.getUrl().toString();
%>

<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
  <head>
    <link href="/main.css" rel="stylesheet" type="text/css"/>

<title>Untangle Web Content Control Warning</title>
<script language="JavaScript">
nonce = '<%=nonce%>';
tid = '<%=tidStr%>';
url = '<%=url%>';
</script>

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
<td>This web page was blocked because it is considered inappropriate.</td>
</tr>
<tr>
<td><b>Host:</b> <%=host%></td>
</tr>
<tr>
<td><b>URL:</b> <%=url%></td>
</tr>
<tr>
<td><b>Category:</b> <%=reason%></td>
</tr>
<tr>
<td>Please contact <%=contact%>.</td>
</tr>
</tbody>
<tfoot>
<tr>
<td><hr width="100%" size="1" color="#969696"/></td>
</tr>
<tr>
<td><address>Untangle Web Content Control</address></td>
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
        </tr>
      </tbody>
    </table>
  </body>
</html>

<%
MvvmRemoteContextFactory.factory().logout();
%>
