<!DOCTYPE html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<c:set var="isDebug" value="true"/>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <title>${companyName}</title>
    <style type="text/css">
        @import "/ext5/packages/ext-theme-gray/build/resources/ext-theme-gray-all.css?s=${buildStamp}";
        @import "/ext5/packages/sencha-charts/build/classic/resources/sencha-charts-all-debug.css?s=${buildStamp}";
    </style>
    <script type="text/javascript">
        var Ext = Ext || {};
        Ext.manifest = {
            compatibility: {
                ext: '4.2'
            }
        }
    </script>

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
    <script type="text/javascript" src="script/mainNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/extOverridesNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/utilNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/windowNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/baseEventLogNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/eventLogNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/gridPanelNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/ruleBuilderNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/matcherWindowNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/monitorNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/componentsNew.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="/script/wizardNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/datetime.js?s=${buildStamp}"></script>
    
    <!-- static resource loading during development. -->
    <script type="text/javascript" src="script/config/aboutNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/administrationNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/emailNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/hostMonitorNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/localDirectoryNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/networkNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/sessionMonitorNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/systemNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/config/upgradeNew.js?s=${buildStamp}"></script>


    <script type="text/javascript" src="script/untangle-base-webfilter/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-base-virus/settingsNew.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="script/untangle-node-adconnector/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-bandwidth/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-boxbackup/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-branding/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-classd/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-faild/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-casing-https/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-ipsec/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-policy/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-sitefilter/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-spamblocker/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-splitd/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-support/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-virusblocker/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-webcache/settingsNew.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="script/untangle-node-adblocker/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-capture/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-clam/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-firewall/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-ips/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-openvpn/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-phish/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-protofilter/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-reporting/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-spamassassin/settingsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/untangle-node-webfilter/settingsNew.js?s=${buildStamp}"></script>
 <script type="text/javascript" src="script/untangle-node-idps/settingsNew.js?s=${buildStamp}"></script>
    
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
  <form name="exportEventLogEvents" id="exportEventLogEvents" method="post" action="/reports/eventLogExport">
    <input type="hidden" name="name" value=""/>
    <input type="hidden" name="query" value=""/>
    <input type="hidden" name="policyId" value=""/>
    <input type="hidden" name="columnList" value=""/>
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
