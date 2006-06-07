<%@ page language="java"%>

<%
String welcomeUrl = application.getInitParameter("welcomeFile");
response.sendRedirect(welcomeUrl);
%>
