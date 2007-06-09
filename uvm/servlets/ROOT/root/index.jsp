<%@ page language="java" import="com.untangle.mvvm.*"%>

<%
String wu = MvvmContextFactory.context().appServerManager().getRootWelcome();
response.sendRedirect(wu);
%>
