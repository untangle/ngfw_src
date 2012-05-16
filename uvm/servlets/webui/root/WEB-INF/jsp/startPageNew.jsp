<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<c:set var="isDebug" value="true"/>
<head>

    <title>${companyName}</title>
    <META content="IE=9.0000" http-equiv="X-UA-Compatible"/>
    <style type="text/css">
     @import "/ext4/resources/css/ext-all-gray.css?s=${buildStamp}";
	 @import "/ext4/examples/ux/css/CheckHeader.css?s=${buildStamp}";
    </style>
<c:if test="${isDebug==false}">
    <script type="text/javascript" src="/ext4/ext-all.js?s=${buildStamp}"></script>
	<script type="text/javascript" src="/ext4/examples/ux/data/PagingMemoryProxy.js?s=${buildStamp}"></script>
	<script type="text/javascript" src="/ext4/examples/ux/CheckColumn.js?s=${buildStamp}"></script>


    <script type="text/javascript" src="/jsonrpc/jsonrpc-min.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/i18n.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/componentsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/rulevalidator.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/mainNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/rulebuilder.js?s=${buildStamp}"></script>


    <!-- todo, move this to a place where it is loaded dynamically. -->
    <script type="text/javascript" src="/script/timezone.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/country.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/wizardNew.js?s=${buildStamp}"></script>
</c:if>
<c:if test="${isDebug==true}">
    <script type="text/javascript" src="/ext4/ext-all-debug.js?s=${buildStamp}"></script>
	<script type="text/javascript" src="/ext4/examples/ux/data/PagingMemoryProxy.js?s=${buildStamp}"></script>
	<script type="text/javascript" src="/ext4/examples/ux/CheckColumn.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/i18nNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/componentsNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/rulevalidator.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/mainNew.js?s=${buildStamp}"></script>
   
   <script type="text/javascript" src="script/rulebuilder.js?s=${buildStamp}"></script>
 
    <!-- todo, move this to a place where it is loaded dynamically. -->
    <script type="text/javascript" src="/script/timezone.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/country.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/wizardNew.js?s=${buildStamp}"></script>
  
</c:if>

<c:if test="${param['console']==1}">
    <script type="text/javascript">
        Ung.Util.maximize();
     </script>
</c:if>
    <script type="text/javascript">
        var storeWindowName='store_window_${storeWindowId}';
		<c:if test="${isCompat==true}">		
			Ext.Compat.showErrors=true;
		</c:if>
        var isWizardComplete = ${isWizardComplete};
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
<form name="exportEventLogEvents" id="exportEventLogEvents" method="post" action="eventLogExport" style="display:none;">
<input type="hidden" name="name" value=""/>
<input type="hidden" name="data" value=""/>
</form>
</div>
<div id="extra-div-1" style="display:none;"><span></span></div>
</body>
</html>
