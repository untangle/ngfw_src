<%@ page language="java" import="jcifs.smb.*" %>

<%@ taglib prefix="browser" uri="WEB-INF/browser.tld" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<%
    String userName = request.getParameter("NAME");
%>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>explode</title>
</head>
<body>
<ol>
  <browser:hello name="<%=userName%>" iterations="2">
  <tr><td><b>have a nice day</b></td></tr>
  </browser:hello>

  <%
  SmbFile f = new SmbFile("smb://dmorris:chakas@metaloft.com/sambaloft/");
  for (String s : f.list()) {
  %>
  <li><%=s%></li>
  <%}%>
</ol>
</body>
</html>