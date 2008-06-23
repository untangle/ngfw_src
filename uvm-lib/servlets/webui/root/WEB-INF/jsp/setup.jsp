<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ page import="com.untangle.uvm.LocalUvmContextFactory" %>
<%@ page import="com.untangle.uvm.LocalUvmContext" %>
<%@ page import="com.untangle.uvm.node.HostName"%>
<%@ page import="com.untangle.uvm.networking.NetworkUtil" %>
<%@ page import="com.untangle.uvm.networking.LocalNetworkManager" %>
<%@ page import="com.untangle.uvm.networking.Interface" %>

<%@ taglib uri="http://java.untangle.com/jsp/uvm" prefix="uvm" %>

<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:uvm="http://java.untangle.com/jsp/uvm">

  <head>
    <title>UNG - Setup Wizard</title>
    
    <style type="text/css">
        @import "ext/resources/css/ext-all.css";
    </style>
    
    <jsp:scriptlet>
      <![CDATA[
               LocalUvmContext context = LocalUvmContextFactory.context();
               request.setAttribute( "ss", context.skinManager().getSkinSettings());
               request.setAttribute( "timezone", context.adminManager().getTimeZone());

               LocalNetworkManager nm = context.networkManager();
               HostName hostname = nm.getHostname();
               if ( hostname.isEmpty() || !hostname.isQualified()) {
                  hostname = NetworkUtil.DEFAULT_HOSTNAME;
               }               
               request.setAttribute( "hostname", hostname );

      ]]>
    </jsp:scriptlet>

    <uvm:skin src="ext-skin.css"  name="${ss.administrationClientSkin}"/>
    <uvm:skin src="skin.css"      name="${ss.administrationClientSkin}"/>

    <script type="text/javascript" src="ext/source/core/Ext.js"></script>
    <script type="text/javascript" src="ext/source/adapter/ext-base.js"></script>
    <script type="text/javascript" src="ext/ext-all-debug.js"></script>
    
    <script type="text/javascript" src="jsonrpc/jsonrpc.js"></script>
    <script type="text/javascript" src="script/wizard.js"></script>
    <script type="text/javascript" src="script/setup.js"></script>
    <script type="text/javascript" src="script/i18n.js"></script>
    <script type="text/javascript" src="script/timezone.js"></script>
    
    <script type="text/javascript">
      Ung.SetupWizard.currentSkin = "${ss.administrationClientSkin}";

      Ung.SetupWizard.CurrentValues = {
          timezone : "${timezone.ID}",
          hostname : "${hostname}"
      };

      Ext.onReady(Ung.Setup.init);
    </script>
 </head>
<body>
<div id="container"></div>
</body>
</html>
