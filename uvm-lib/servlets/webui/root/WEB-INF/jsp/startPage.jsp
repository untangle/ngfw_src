<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>UNG</title>
    
    <style type="text/css">
        @import "ext/resources/css/ext-all.css";
		@import "css/main.css";
		@import "skins/default/css/ext-skin.css";
		@import "skins/default/css/skin.css";
    </style>
<!--
	<script type="text/javascript" src="ext/source/core/Ext.js"></script>
	<script type="text/javascript" src="ext/source/adapter/ext-base.js"></script>
	<script type="text/javascript" src="ext/ext-all-debug.js"></script>
	<script type="text/javascript" src="jsonrpc/jsonrpc.js"></script>
    <script type="text/javascript" src="firebug/firebug.js"></script>
    <script type="text/javascript" src="script/untangle-node-protofilter/settings.js"></script>


	<script type="text/javascript" src="ext/adapter/ext/ext-base.js"></script>
	<script type="text/javascript" src="ext/ext-all.js"></script>
-->	
	<script type="text/javascript" src="ext/source/core/Ext.js"></script>
	<script type="text/javascript" src="ext/source/adapter/ext-base.js"></script>
	<script type="text/javascript" src="ext/ext-all-debug.js"></script>

	
	<script type="text/javascript" src="jsonrpc/jsonrpc-min.js"></script>
    <script type="text/javascript" src="script/main.js"></script>
	<script type="text/javascript" src="script/i18n.js"></script>
    <script type="text/javascript" src="script/components.js"></script>
<!--     
    <script type="text/javascript" src="script/untangle-node-protofilter/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-spyware/settings.js"></script>
-->	
	<script type="text/javascript">
		function init() {
			main=new Ung.Main();
			//main.version='${version}';
			main.init();
		}
		Ext.onReady(init);
	</script>
 </head>
<body>
<div id="scripts_container" style="display: none;"></div>
<div id="container">
	<div id="contentleft">
		<div id="logo"><img src="images/Logo150x96.gif"/></div>
		<div id="tabs">
		</div>
			<div id="tabLibrary" class="x-hide-display">
			    <div style="margin-left:15px;font-size: 11px;text-align:left;">Click to Learn More</div>
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
