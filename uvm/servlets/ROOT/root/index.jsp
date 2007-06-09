<%@ page language="java" import="com.untangle.uvm.*"%>

<%
String wu = UvmContextFactory.context().appServerManager().getRootWelcome();
response.sendRedirect(wu);
%>
