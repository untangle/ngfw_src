<%@ page language="java" import="com.metavize.mvvm.client.*, com.metavize.mvvm.tran.*, com.metavize.mvvm.security.*, com.metavize.tran.spyware.*"%>

<%
MvvmRemoteContext ctx = MvvmRemoteContextFactory.factory().systemLogin(0, Thread.currentThread().getContextClassLoader());
TransformManager tman = ctx.transformManager();

String nonce = request.getParameter("nonce");
String tidStr = request.getParameter("tid");
Tid tid = new Tid(Long.parseLong(tidStr));

TransformContext tctx = tman.transformContext(tid);
Spyware tran = (Spyware)tctx.transform();
BlockDetails bd = tran.getBlockDetails(nonce);

String header = "Untangle Networks Spyware Blocker";
String host = bd.getHost();
String url = bd.getUrl();
%>

<html>
<head>
<title>403 Forbidden</title>

<script language="JavaScript">
nonce = '<%=nonce%>';
tid = '<%=tidStr%>';
url = '<%=url%>';
</script>

<script type="text/javascript" src="spyware.js"></script>

</head>
<body>
<center><b>Untangle Networks Spyware Blocker</b></center>
<p>This site blocked because it may contain spyware.</p>
<p>Host: <%=host%></p>
<p>URL: <%=url%></p>

<table>
  <tr>
    <td>
      <input id="unblockNowButton" type="button" value="Unblock For Now"
      onclick="unblockSite(false)"/>
    </td>

    <td>
      <input id="unblockGlobalButton" type="button" value="Unblock Permanently"
      onclick="unblockSite(true)"/>
    </td>
  </tr>
</table>

<hr>
<address>Untangle Networks EdgeGuard</address>
</body>
</html>

<%
MvvmRemoteContextFactory.factory().logout();
%>