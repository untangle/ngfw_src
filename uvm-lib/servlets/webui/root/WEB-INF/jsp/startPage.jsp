<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<c:set var="isDebug" value="false"/>
<head>

    <title>${bbs.companyName}</title>
    <META content="IE=7.0000" http-equiv="X-UA-Compatible"/>
    <style type="text/css">
        @import "/ext/resources/css/ext-all.css";
    </style>
<c:if test="${isDebug==false}">
    <script type="text/javascript" src="/ext/adapter/ext/ext-base.js"></script>
    <script type="text/javascript" src="/ext/ext-all.js"></script>

    <script type="text/javascript" src="/jsonrpc/jsonrpc-min.js"></script>
    <script type="text/javascript" src="/script/i18n.js"></script>
    <script type="text/javascript" src="script/components-min.js"></script>
    <script type="text/javascript" src="script/main-min.js"></script>

    <!-- todo, move this to a place where it is loaded dynamically. -->
    <script type="text/javascript" src="/script/timezone.js"></script>
    <script type="text/javascript" src="/script/country.js"></script>
    <script type="text/javascript" src="/script/wizard.js"></script>
</c:if>
<c:if test="${isDebug==true}">
    <script type="text/javascript" src="/ext/source/core/Ext.js"></script>
    <script type="text/javascript" src="/ext/source/adapter/ext-base.js"></script>
    <script type="text/javascript" src="/ext/ext-all-debug.js"></script>


    <script type="text/javascript" src="/jsonrpc/jsonrpc.js"></script>
    <script type="text/javascript" src="/script/i18n.js"></script>
    <script type="text/javascript" src="script/components.js"></script>
    <script type="text/javascript" src="script/main.js"></script>

    <!-- todo, move this to a place where it is loaded dynamically. -->
    <script type="text/javascript" src="/script/timezone.js"></script>
    <script type="text/javascript" src="/script/country.js"></script>
    <script type="text/javascript" src="/script/wizard.js"></script>

<!-- Just for Test, normaly this resources are Dynamically loaded
-->
    <script type="text/javascript" src="script/untangle-node-openvpn/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-spyware/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-protofilter/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-shield/settings.js"></script>
    <script type="text/javascript" src="script/untangle-base-webfilter/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-webfilter/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-phish/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-spamassassin/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-ips/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-firewall/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-portal/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-reporting/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-boxbackup/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-pcremote/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-policy/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-adconnector/settings.js"></script>
    <script type="text/javascript" src="script/untangle-base-virus/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-clam/settings.js"></script>
    <script type="text/javascript" src="script/config/administration.js"></script>
    <script type="text/javascript" src="script/config/email.js"></script>
    <script type="text/javascript" src="script/config/system.js"></script>
    <script type="text/javascript" src="script/config/systemInfo.js"></script>
    <script type="text/javascript" src="script/config/upgrade.js"></script>
    <script type="text/javascript" src="script/config/localDirectory.js"></script>
    <script type="text/javascript" src="script/config/policyManager.js"></script>
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
<div id="container" style="margin:0px 0px 0px 0px;"></div>
<div id="extra-div-1" style="display:none;"><span></span></div>
</body>
</html>
