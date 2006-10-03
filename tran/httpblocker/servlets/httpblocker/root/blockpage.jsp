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
%>

<html>
<head>
<title>403 Forbidden</title>
<script type="text/javascript" src="httpblocker.js"></script>
</head>
<body>
<center><b><%=header%></b></center>
<p>This site blocked because of inappropriate content</p>
<p>Host: <%=host%></p>
<p>URI: <%=uri%></p>
<p>Category: <%=reason%></p>

<table>
  <tr>
    <td>
      <input type="button" value="Unblock For Now"
      onclick="unblockSite(false)"/>
    </td>

    <td>
      <input type="button" value="Unblock Permanently"
      onclick="unblockSite(true)"/>
    </td>
  </tr>
</table>

<p>Please contact <%=contact%></p>
<hr>
<address>Untangle Networks EdgeGuard</address>
</body>
</html>

<%
MvvmRemoteContextFactory.factory().logout();
%>