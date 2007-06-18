<%@ page language="java" import="com.untangle.uvm.*"%>

<%
String wu = LocalUvmContextFactory.context().appServerManager().getRootWelcome();
response.sendRedirect(wu);
%>
