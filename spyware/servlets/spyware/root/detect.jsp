<%@ page language="java" import="com.untangle.uvm.*, com.untangle.uvm.node.*, com.untangle.uvm.security.*, com.untangle.node.spyware.*"%>
<%--
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
--%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>403 Forbidden</title>
</head>
<body>
<script id="untangleDetect" type="text/javascript">
// <![CDATA[
var e = document.getElementById("untangleDetect");
if (window == window.top && e.parentNode.tagName == "BODY") {
  window.location.href = '/spyware/blockpage.jsp?<%=request.getQueryString()%>';
}
// ]]>
</script>
</body>
</html>
