<%@ page language="java" import="com.metavize.mvvm.client.*, com.metavize.mvvm.tran.*, com.metavize.mvvm.security.*, com.metavize.tran.httpblocker.*"%>

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

<title>Untangle Networks Warning</title>
<script language="JavaScript">
nonce = '<%=nonce%>';
tid = '<%=tidStr%>';
url = '<%=url%>';
</script>

  </head>

  <body>
    <table cellspacing="0" cellpadding="0" border="0" align="center" class="main">
      <tbody>
        <tr>
          <td class="main-top">
          </td>
        </tr>
        <tr>
          <td class="main-middle">

<table>
<tbody>
<tr>
  <td colspan=1 width="100px">
<a href="http://www.untangle.com"><img src="/images/LogoNoText96x96.gif" border="0" alt="Untangle Networks logo"/></a>
  </td>
  <td colspan=1 align=left valign=center>
    <span style="font-family: SansSerif; font-size: 18.0px;">
<%=header%>
    </span></td>
</tr>
</tbody>
</table>

<table>
<tbody>
<tr>
<td><hr></td>
</tr>
<tr>
<td>This web page was blocked because it is considered inappropriate.</td>
</tr>
<tr>
<td>Host: <%=host%></td>
</tr>
<tr>
<td>URL: <%=url%></td>
</tr>
<tr>
<td>Category: <%=reason%></td>
</tr>
<tr>
<td>Please contact <%=contact%>.</td>
</tr>
</tbody>

<tfoot>
<tr>
<td><hr></td>
</tr>
<tr>
<td><address>Untangle Networks EdgeGuard</address></td>
</tr>
</tfoot>
</table>

          </td>
        </tr>
        <tr>
          <td class="main-bottom">
          </td>
        </tr>
      </tbody>
    </table>
  </body>
</html>

<%
MvvmRemoteContextFactory.factory().logout();
%>
