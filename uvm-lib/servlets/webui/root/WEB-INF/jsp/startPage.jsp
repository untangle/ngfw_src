<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>UNG</title>
    
    <style type="text/css">
        @import "ext/resources/css/ext-all.css";
        @import "ext/examples/multiselect/multiselect.css";
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
    <script type="text/javascript" src="ext/examples/multiselect/MultiSelect.js"></script>
    <script type="text/javascript" src="ext/examples/multiselect/DDView.js"></script>

	
	<script type="text/javascript" src="jsonrpc/jsonrpc.js"></script>
    <script type="text/javascript" src="script/components.js"></script>
    <script type="text/javascript" src="script/i18n.js"></script>
    <script type="text/javascript" src="script/main.js"></script>

    <!-- todo, move this to a place where it is loaded dynamically. -->
    <script type="text/javascript" src="script/timezone.js"></script>

<!-- Just for Test, normaly this resources are Dynamically loaded     
-->	
    <script type="text/javascript" src="script/untangle-node-openvpn/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-spyware/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-protofilter/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-shield/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-webfilter/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-phish/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-spamassassin/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-ips/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-firewall/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-portal/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-reporting/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-boxbackup/settings.js"></script>
    <script type="text/javascript" src="script/untangle-node-pcremote/settings.js"></script>
<!--     <script type="text/javascript" src="script/untangle-base-virus/settings.js"></script> 
    <script type="text/javascript" src="script/untangle-node-clam/settings.js"></script> --> 
    <script type="text/javascript" src="script/config/administration.js"></script>
    <script type="text/javascript" src="script/config/email.js"></script>
    <script type="text/javascript" src="script/config/system.js"></script>
    <script type="text/javascript" src="script/config/systemInfo.js"></script>
    <script type="text/javascript" src="script/config/upgrade.js"></script>
    <script type="text/javascript" src="script/config/racks.js"></script>
    <script type="text/javascript" src="script/config/userDirectory.js"></script>

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
