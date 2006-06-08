<%@ page language="java" import="com.metavize.mvvm.*"%>

<%
String wu = MvvmContextFactory.context().appServerManager().getRootWelcome();
response.sendRedirect(wu);
%>
