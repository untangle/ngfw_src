<%@ page language="java" %>

<% String sp = (String)request.getContextPath() + "/secure"; %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
<title>Metavize Portal</title>
<style type="text/css">
<!--
@import url(/AjaxTk/img/dwtimgs.css);
-->
<!--
@import url(/images/mvimgs.css);
-->
</style>
<link rel="stylesheet" type="text/css" href="/AjaxTk/common.css"/>
<link rel="stylesheet" type="text/css" href="/AjaxTk/dwt.css"/>
<link rel="stylesheet" type="text/css" href="<%=sp%>/portal.css"/>
<link rel="stylesheet" type="text/css" href="/metavize.css"/>

<script type="text/javascript" src="/AjaxTk/I18nMsg.js"></script>
<script type="text/javascript" src="/AjaxTk/AjxMsg.js"></script>

<jsp:include page="Ajax.inc"/>

<script type="text/javascript" src="<%=sp%>/AddBookmarkDialog.js"></script>
<script type="text/javascript" src="<%=sp%>/AddBookmarkPanel.js"></script>
<script type="text/javascript" src="<%=sp%>/Application.js"></script>
<script type="text/javascript" src="<%=sp%>/ApplicationIframe.js"></script>
<script type="text/javascript" src="<%=sp%>/Bookmark.js"></script>
<script type="text/javascript" src="<%=sp%>/BookmarkList.js"></script>
<script type="text/javascript" src="<%=sp%>/BookmarkManagerDialog.js"></script>
<script type="text/javascript" src="<%=sp%>/BookmarkManagerPanel.js"></script>
<script type="text/javascript" src="<%=sp%>/BookmarkPanel.js"></script>
<script type="text/javascript" src="<%=sp%>/Desktop.js"></script>
<script type="text/javascript" src="<%=sp%>/MvTabView.js"></script>
<script type="text/javascript" src="<%=sp%>/NavigationBar.js"></script>
<script type="text/javascript" src="<%=sp%>/Portal.js"></script>
<script type="text/javascript" src="<%=sp%>/PortalPanel.js"></script>
<script type="text/javascript" src="<%=sp%>/WelcomePanel.js"></script>
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
