<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>UNG</title>
    
    <style type="text/css">
        @import "ext/resources/css/ext-all.css";
    </style>
<!--
	<script type="text/javascript" src="ext/source/core/Ext.js"></script>
	<script type="text/javascript" src="ext/source/adapter/ext-base.js"></script>
	<script type="text/javascript" src="ext/ext-all-debug.js"></script>
	<script type="text/javascript" src="jsonrpc/jsonrpc.js"></script>
    <script type="text/javascript" src="firebug/firebug.js"></script>

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

<!-- Just for Test, normaly this resources are Dynamically loaded     
    
    <script type="text/javascript" src="script/untangle-node-protofilter/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-spyware/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-shield/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-webfilter/settings.js"></script>
    <script type="text/javascript" src="script/config/administration.js"></script>
-->	
    <script type="text/javascript" src="script/untangle-node-protofilter/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-shield/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-webfilter/settings.js"></script>
    <script type="text/javascript" src="script/config/administration.js"></script>
    <script type="text/javascript" src="script/config/system.js"></script>

	<script type="text/javascript">
		function init() {
			main=new Ung.Main();
			main.init();
		}
		Ext.onReady(init);
	</script>
 </head>
<body>
<div id="container"></div>
</body>
</html>
