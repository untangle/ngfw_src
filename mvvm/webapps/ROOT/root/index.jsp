<%@ page language="java"%>

<%
String welcomeUrl = application.getInitParameter("welcomeFile");
System.out.println("WELCOME_URL: " + welcomeUrl);
response.sendRedirect(welcomeUrl);
%>
