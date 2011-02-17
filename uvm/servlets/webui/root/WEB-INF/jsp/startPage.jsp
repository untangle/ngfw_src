<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<c:set var="isDebug" value="false"/>
<head>

    <title>${companyName}</title>
    <META content="IE=7.0000" http-equiv="X-UA-Compatible"/>
    <style type="text/css">
        @import "/ext/resources/css/ext-all.css?s=${buildStamp}";
    </style>
<c:if test="${isDebug==false}">
    <script type="text/javascript" src="/ext/adapter/ext/ext-base.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/ext/ext-all.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="/jsonrpc/jsonrpc-min.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/i18n.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/components-min.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/main-min.js?s=${buildStamp}"></script>

    <!-- todo, move this to a place where it is loaded dynamically. -->
    <script type="text/javascript" src="/script/timezone.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/country.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/wizard.js?s=${buildStamp}"></script>
</c:if>
<c:if test="${isDebug==true}">
    <script type="text/javascript" src="/ext/source/core/Ext.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/ext/source/adapter/ext-base.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/ext/ext-all-debug.js?s=${buildStamp}"></script>


    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/i18n.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/components.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/main.js?s=${buildStamp}"></script>

    <!-- todo, move this to a place where it is loaded dynamically. -->
    <script type="text/javascript" src="/script/timezone.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/country.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/wizard.js?s=${buildStamp}"></script>

    <!-- Just for Test, normaly this resources are Dynamically loaded -->
    <script type="text/javascript" src="script/untangle-node-adblocker/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-adconnector/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-bandwidth/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-boxbackup/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-branding/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-clam/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-commtouch/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-cpd/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-faild/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-firewall/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-ips/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-kav/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-license/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-openvpn/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-phish/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-policy/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-protofilter/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-reporting/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-shield/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-sitefilter/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-spamassassin/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-splitd/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-spyware/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-support/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-webcache/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-webfilter/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/administration.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/email.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/system.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/systemInfo.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/upgrade.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/localDirectory.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/policyManager.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/sessionMonitor.js?s=${buildStamp}"></script>
</c:if>

<c:if test="${param['console']==1}">
    <script type="text/javascript">
        Ung.Util.maximize();
     </script>
</c:if>
    <script type="text/javascript">
        var storeWindowName='store_window_${storeWindowId}';
        var isRegistered = ${isRegistered};
        function init() {
            main=new Ung.Main({debugMode:${isDebug}});
            main.buildStamp='${buildStamp}';
            main.init();
        }
        Ext.onReady(init);
    </script>
 </head>
<body>
<div id="container" style="margin:0px 0px 0px 0px;">
<form name="exportGridSettings" id="exportGridSettings" method="post" action="gridSettings" style="display:none;">
<input type="hidden" name="gridName" value=""/>
<input type="hidden" name="gridData" value=""/>
<input type="hidden" name="type" value="export"/>
</form>
</div>
<div id="extra-div-1" style="display:none;"><span></span></div>
</body>
</html>
