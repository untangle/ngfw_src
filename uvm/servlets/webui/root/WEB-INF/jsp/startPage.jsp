<!DOCTYPE html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<c:set var="isDebug" value="true"/>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>${companyName}</title>
    <style type="text/css">
        @import "/ext5/packages/ext-theme-gray/build/resources/ext-theme-gray-all.css?s=${buildStamp}";
        @import "/ext5/packages/sencha-charts/build/classic/resources/sencha-charts-all-debug.css?s=${buildStamp}";
    </style>

<c:if test="${isDebug==false}">
    <script type="text/javascript" src="/ext5/ext-all.js?s=${buildStamp}"></script>
</c:if>
<c:if test="${isDebug==true}">
    <script type="text/javascript" src="/ext5/ext-all-debug.js?s=${buildStamp}"></script>
</c:if>

    <script type="text/javascript" src="/ext5/packages/ext-theme-gray/build/ext-theme-gray.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/ext5/packages/sencha-charts/build/sencha-charts-debug.js?s=${buildStamp}"></script>
    <script type="text/javascript">
        Ext.buildStamp='${buildStamp}';
    </script>
    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/i18n.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/main.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/extOverrides.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/util.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/window.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/gridPanel.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/ruleBuilder.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/matcherWindow.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/monitor.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/tableConfig.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/reports.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/reportEditor.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/components.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="/script/wizard.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/datetime.js?s=${buildStamp}"></script>
    
<c:if test="${isDebug==true}">
    <!-- static resource loading during development. -->
    <script type="text/javascript" src="script/config/about.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/administration.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/email.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/hostMonitor.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/localDirectory.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/network.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/sessionMonitor.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/system.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/upgrade.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/reportsViewer.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="script/untangle-base-webfilter/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-base-virus/settings.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="script/untangle-node-adconnector/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-bandwidth/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-boxbackup/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-branding/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-classd/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-faild/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-casing-https/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-ipsec/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-policy/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-sitefilter/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-spamblocker/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-splitd/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-support/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-virusblocker/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-webcache/settings.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="script/untangle-node-ad-blocker/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-capture/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-virus-blocker-lite/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-firewall/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-idps/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-openvpn/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-phish-blocker/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-application-control-lite/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-reporting/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-spam-blocker-lite/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-webfilter/settings.js?s=${buildStamp}"></script>
</c:if>

<%--
<c:if test="${param['expert']==1}">
    <script type="text/javascript">
        Ung.Util.hideDangerous = false;
     </script>
</c:if>
--%>

    <script type="text/javascript">
    Ext.onReady(function() {
        Ung.Main.init({debugMode:${isDebug}, buildStamp:'${buildStamp}'})
    });
    </script>
 </head>
<body>
<div id="container" style="display:none;">
  <form name="exportGridSettings" id="exportGridSettings" method="post" action="gridSettings">
    <input type="hidden" name="gridName" value=""/>
    <input type="hidden" name="gridData" value=""/>
    <input type="hidden" name="type" value="export"/>
  </form>
  <form name="downloadForm" id="downloadForm" method="post" action="download">
    <input type="hidden" name="type" value=""/>
    <input type="hidden" name="arg1" value=""/>
    <input type="hidden" name="arg2" value=""/>
    <input type="hidden" name="arg3" value=""/>
    <input type="hidden" name="arg4" value=""/>
    <input type="hidden" name="arg5" value=""/>
    <input type="hidden" name="arg6" value=""/>
  </form>
</div>
</body>
</html>
