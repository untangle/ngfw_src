<%@ page language="java" %>

<% String sp = (String)request.getContextPath() + "/secure"; %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
<title>Untangle Networks Portal</title>
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

<script type="text/javascript" src="/AjaxTk/BundledAjx.js"></script>

<script type="text/javascript" src="/UntangleTk/MvRpc.js"></script>

<script type="text/javascript" src="<%=sp%>/BookmarkDialog.js"></script>
<script type="text/javascript" src="<%=sp%>/Application.js"></script>
<script type="text/javascript" src="<%=sp%>/ApplicationIframe.js"></script>
<script type="text/javascript" src="<%=sp%>/ApplicationList.js"></script>
<script type="text/javascript" src="<%=sp%>/ApplicationPanel.js"></script>
<script type="text/javascript" src="<%=sp%>/Bookmark.js"></script>
<script type="text/javascript" src="<%=sp%>/BookmarkList.js"></script>
<script type="text/javascript" src="<%=sp%>/BookmarkManagerDialog.js"></script>
<script type="text/javascript" src="<%=sp%>/BookmarkManagerPanel.js"></script>
<script type="text/javascript" src="<%=sp%>/BookmarkPanel.js"></script>
<script type="text/javascript" src="<%=sp%>/BookmarkProperty.js"></script>
<script type="text/javascript" src="<%=sp%>/Desktop.js"></script>
<script type="text/javascript" src="<%=sp%>/MvTabView.js"></script>
<script type="text/javascript" src="<%=sp%>/NavigationBar.js"></script>
<script type="text/javascript" src="<%=sp%>/Portal.js"></script>
<script type="text/javascript" src="<%=sp%>/PortalPanel.js"></script>

<script type="text/javascript" src="<%=sp%>/FooBar.js"></script>

</head>

<body>

<noscript><p><b>Please enable JavaScript to use this application.</b></p></noscript>

<script language="JavaScript">
function launch() {
DBG = new AjxDebug(AjxDebug.NONE, null, false);

var shell = new DwtShell(null, false);
shell.setVirtual(true);
var portal = new Portal(shell);
portal.zShow(true);
portal.setVisible(true);
}
AjxCore.addOnloadListener(launch);
</script>

</body>

</html>
