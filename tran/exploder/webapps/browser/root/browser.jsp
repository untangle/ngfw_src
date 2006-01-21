<%@ page language="java" import="jcifs.smb.*" %>
<%@ taglib prefix="browser" uri="WEB-INF/browser.tld" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
<title>HippieSoft.com(tm) Exploder(r)</title>
<link rel="stylesheet" type="text/css" href="browser.css"/>
<script type="text/javascript" src="prototype-1.3.1.js"></script>
<script type="text/javascript" src="browser.js"></script>
</head>

<body>

<div id="toolbar">
<input type="button" value="delete" onclick="deleteSelection()"/>
<input type="button" value="refresh" onclick="refreshDetails()"/>
<input type="file" value="upload" onselect="uploadFile()"/>
</div>

<div id="summary">
<CENTER><B>SUMMARY</B></CENTER>
</div>

<div id="tree">
<browser:dirtree url="smb://bebe/"/>
</div>

<div id="detail"/>

</body>
</html>
