<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ page import="com.untangle.uvm.LocalUvmContextFactory" %>
<%@ taglib uri="http://java.untangle.com/jsp/uvm" prefix="uvm" %>

<html xmlns="http://www.w3.org/1999/xhtml">

  <head>
    <title>UNG - Setup Wizard</title>
    
    <style type="text/css">
        @import "ext/resources/css/ext-all.css";
    </style>
    
    <jsp:scriptlet>
      <![CDATA[
               request.setAttribute( "ss", LocalUvmContextFactory.context().skinManager().getSkinSettings());
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

      Ext.onReady(Ung.Setup.init);
    </script>
 </head>
<body>
<div id="container"></div>
</body>
</html>
