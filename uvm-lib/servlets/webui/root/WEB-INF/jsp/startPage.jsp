<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Untangle rack - Prototype 4</title>
    
    <style type="text/css">
        @import "ext-2.0.1/resources/css/ext-all.css?${version}";
        @import "untangle.css?${version}";
    </style>
<!--
	<script type="text/javascript" src="ext-2.0.1/source/core/Ext.js?${version}"></script>
	<script type="text/javascript" src="ext-2.0.1/source/adapter/ext-base.js?${version}"></script>
	<script type="text/javascript" src="ext-2.0.1/ext-all-debug.js?${version}"></script>
	<script type="text/javascript" src="jsonrpc/jsonrpc.js?${version}"></script>
    <script type="text/javascript" src="firebug/firebug.js?${version}"></script>
    <script type="text/javascript" src="script/untangle-node-protofilter/settings.js?${version}"></script>
-->	
	<script type="text/javascript" src="ext-2.0.1/adapter/ext/ext-base.js?${version}"></script>
	<script type="text/javascript" src="ext-2.0.1/ext-all.js?${version}"></script>
	<script type="text/javascript" src="script/i18n.js?${version}"></script>
	<script type="text/javascript" src="jsonrpc/jsonrpc-min.js?${version}"></script>
    <script type="text/javascript" src="script/untangle.js?${version}"></script>
    <script type="text/javascript" src="script/ext-untangle.js?${version}"></script>
	<script type="text/javascript">
		MainPage.version='${version}';
		Ext.onReady(MainPage.init);
	</script>
</head>
<body>
<div id="scripts_container" style="display: none;"></div>
<div id="container">
	<div id="contentleft">
		<div id="logo"><img src="images/Logo150x96.gif?${version}"/></div>
		<div id="tabs">
		</div>
			<div id="tabLibrary" class="x-hide-display">
			    <div style="margin-left:15px;font-size: 11px;text-align:left;">Click to Learn More</div>
			    <div id="test1"></div>
			    <div id="test2"></div>
			    <div id="toolsLibrary"></div>
			</div>
			<div id="tabMyApps" class="x-hide-display">
			    <div style="margin-left:15px;font-size: 11px;text-align:left;">Click to Install into Rack</div>
			    <div id="toolsMyApps"></div>
			</div>
			<div id="tabConfig" class="x-hide-display">
               	<div style="margin-left:15px;font-size: 11px;text-align:left;">Click to Configure</div>
               	<div id="toolsConfig"></div>
			</div>
 		<div id="help"></div>
	</div>
	<div id="contentright">
		<div id="racks">
			<div id="leftBorder"></div>
			<div id="rightBorder"></div>
			<div id="rack_list"></div>
			<div id="rack_nodes">
			<div id="security_nodes"></div>
			<div id="nodes_separator" style="display:none;"><div id="nodes_separator_text"></div></div>
			<div id="other_nodes"></div>
			</div>
		</div>
	</div>
	<div id="test" style="display: none;"></div>
</div>
</body>
</html>
