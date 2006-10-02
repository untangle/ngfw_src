<%@ page language="java" import="com.metavize.mvvm.portal.PortalLogin"%>

<%
String nonce = request.getParameter("nonce");
System.out.println("NONCE: " + nonce);

String greeting = "<<GREETING>>";
String contact = "<<CONTACT>>";
String host = "<<HOST>>";
String uri = "<<URI>>";
String category = "<<CATEGORY>>";
%>

<html>
<head>
<title>403 Forbidden</title>
<script type="text/javascript" src="httpblocker.js"></script>
</head>
<body>
<center><b><%=greeting%></b></center>
<p>This site blocked because of inappropriate content</p>
<p>Host: <%=host%></p>
<p>URI: <%=uri%></p>
<p>Category: <%=category%></p>

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
