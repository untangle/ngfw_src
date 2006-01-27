<%@ page language="java" contentType="text/javascript" %>
<%@ taglib prefix="store" uri="WEB-INF/store.tld" %>

<%
response.setHeader("Cache-Control","no-cache");
response.setHeader("Pragma","no-cache");
response.setDateHeader("Expires", 0);
%>

<store:define-globals/>

function isInstalled(pkg)
{
   return null != installed[pkg];
}