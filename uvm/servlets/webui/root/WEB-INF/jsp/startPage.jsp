<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<c:set var="isDebug" value="true"/>
<head>
    <title>${companyName}</title>
    <style type="text/css">
     @import "/ext4/resources/css/ext-all-gray.css?s=${buildStamp}";
     @import "/ext4/examples/ux/css/CheckHeader.css?s=${buildStamp}";
     @import "/ext4/examples/ux/grid/css/GridFilters.css?s=${buildStamp}";
     @import "/ext4/examples/ux/grid/css/RangeMenu.css?s=${buildStamp}";
     
    </style>
<c:if test="${isDebug==false}">
    <script type="text/javascript" src="/ext4/ext-all.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=${buildStamp}"></script>
</c:if>
<c:if test="${isDebug==true}">
    <script type="text/javascript" src="/ext4/ext-all-debug.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=${buildStamp}"></script>
</c:if>
    <script type="text/javascript" src="/script/i18n.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/components.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/main.js?s=${buildStamp}"></script>

    <!-- todo, move this to a place where it is loaded dynamically. -->
    <script type="text/javascript" src="/script/country.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/wizard.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/datetime.js?s=${buildStamp}"></script>

    <script type="text/javascript">
        var storeWindowName='store_window_${storeWindowId}';
        var isWizardComplete = ${isWizardComplete};
        var main;
        function init() {
            main=new Ung.Main({debugMode:${isDebug},buildStamp:'${buildStamp}'});
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
<form name="exportEventLogEvents" id="exportEventLogEvents" method="post" action="/reports/eventLogExport" style="display:none;">
<input type="hidden" name="name" value=""/>
<input type="hidden" name="query" value=""/>
<input type="hidden" name="policyId" value=""/>
<input type="hidden" name="columnList" value=""/>
</form>
<form name="downloadForm" id="downloadForm" method="post" action="download" style="display:none;">
<input type="hidden" name="type" value=""/>
<input type="hidden" name="arg1" value=""/>
<input type="hidden" name="arg2" value=""/>
<input type="hidden" name="arg3" value=""/>
<input type="hidden" name="arg4" value=""/>
</form>
</div>
<div id="extra-div-1" style="display:none;"><span></span></div>
</body>
</html>
