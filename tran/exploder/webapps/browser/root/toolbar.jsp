<%@ page language="java" %>
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

<form action="toolbar.jsp" method="post" enctype="multipart/form-data">
<input type="button" value="delete" onclick="deleteSelection()"/>
<input type="button" value="refresh" onclick="refreshDetails()"/>
<input type="button" value="upload" onclick="modalDialog()"/>
<!-- <input type="file" name="file" onchange="modalDialog()"/> -->
<!-- <input type="submit" value="upload"/> -->
<browser:save-upload/>
</form>

</body>
</html>
