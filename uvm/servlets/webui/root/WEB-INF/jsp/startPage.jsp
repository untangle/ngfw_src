<!DOCTYPE html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<c:set var="isDebug" value="true"/>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"/>
    <title>${companyName}</title>
    <style type="text/css">
        @import "/ext6/classic/theme-${extjsTheme}/resources/theme-${extjsTheme}-all.css?s=${buildStamp}";
    </style>
    <script type="text/javascript" src="/highcharts/proj4.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/highcharts/highstock.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/highcharts/highcharts-extra.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/highcharts/map.js?s=${buildStamp}"></script>

    <link href="/images/fonts/source-sans-pro/source-sans-pro.css" rel="stylesheet"/>

<c:if test="${isDebug==false}">
    <script type="text/javascript" src="/ext6/ext-all.js?s=${buildStamp}"></script>
</c:if>
<c:if test="${isDebug==true}">
    <script type="text/javascript" src="/ext6/ext-all-debug.js?s=${buildStamp}"></script>
</c:if>

    <script type="text/javascript" src="/ext6/classic/theme-${extjsTheme}/theme-${extjsTheme}.js?s=${buildStamp}"></script>
    <script type="text/javascript">
        Ext.buildStamp='${buildStamp}';
    </script>
    <!-- global scripts -->
    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/i18n.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/util.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/window.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/charting.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/tableConfig.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/reports.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/wizard.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/datetime.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="script/gridPanel.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/ruleBuilder.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/matcherWindow.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/monitor.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/reportEditor.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/dashboardWidgets.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/components.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/main.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/extOverrides.js?s=${buildStamp}"></script>

<c:if test="${isDebug==true}">
    <!-- static resource loading during development. -->
    <script type="text/javascript" src="script/config/about.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/administration.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/email.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/hostMonitor.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/deviceMonitor.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/localDirectory.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/network.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/sessionMonitor.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/system.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/upgrade.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/dashboardManager.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/accountRegistration.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/offline.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="script/untangle-base-web-filter/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-base-virus-blocker/settings.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="script/directory-connector/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/bandwidth-control/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/configuration-backup/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/branding-manager/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/application-control/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/wan-failover/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/ssl-inspector/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/ipsec-vpn/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/policy-manager/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/web-filter/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/spam-blocker/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/wan-balancer/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/live-support/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/virus-blocker/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/web-cache/settings.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="script/ad-blocker/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/captive-portal/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/virus-blocker-lite/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/firewall/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/intrusion-prevention/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/openvpn/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/phish-blocker/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/application-control-lite/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/reports/settings.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/spam-blocker-lite/settings.js?s=${buildStamp}"></script>
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
