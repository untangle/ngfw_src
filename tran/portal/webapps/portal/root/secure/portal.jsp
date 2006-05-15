<%@ page language="java" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
<title>Metavize Portal</title>
<style type="text/css">
<!--
@import url(/AjaxTk/img/dwtimgs.css);
-->
</style>
<link rel="stylesheet" type="text/css" href="/AjaxTk/common.css"/>
<link rel="stylesheet" type="text/css" href="/AjaxTk/dwt.css"/>
<link rel="stylesheet" type="text/css" href="portal.css"/>

<script type="text/javascript" src="/AjaxTk/I18nMsg.js"></script>
<script type="text/javascript" src="/AjaxTk/AjxMsg.js"></script>

<jsp:include page="Ajax.inc"/>

<script type="text/javascript" src="Application.js"></script>
<script type="text/javascript" src="Bookmark.js"></script>
<script type="text/javascript" src="Portal.js"></script>
<script type="text/javascript" src="AddBookmarkPanel.js"></script>
<script type="text/javascript" src="AddBookmarkDialog.js"></script>
<script type="text/javascript" src="BookmarkPanel.js"></script>

</head>

<body>

<noscript><p><b>Please enable JavaScript to use this application.</b></p></noscript>

<script language="JavaScript">
function launch() {
DBG = new AjxDebug(AjxDebug.DBG1, null, false);

var shell = new DwtShell("MainShell", false);
var portal = new Portal(shell, "smb://windows.metavize.com/");
}
AjxCore.addOnloadListener(launch);
</script>

</body>

</html>
