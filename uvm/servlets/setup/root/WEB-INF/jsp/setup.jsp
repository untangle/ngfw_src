<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" %>

<%@ taglib uri="http://java.untangle.com/jsp/uvm" prefix="uvm" %>

<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:uvm="http://java.untangle.com/jsp/uvm">
  <head>
    <title>Setup Wizard</title>
    <META content="IE=7.0000" http-equiv="X-UA-Compatible"/>    
    <style type="text/css">
      @import "/ext/resources/css/ext-all.css";
    </style>
    
    <uvm:skin src="ext-skin.css"  name="${ss.administrationClientSkin}"/>
    <uvm:skin src="admin.css"      name="${ss.administrationClientSkin}"/>

    <script type="text/javascript" src="/ext/source/core/Ext.js"></script>
    <script type="text/javascript" src="/ext/source/adapter/ext-base.js"></script>
    <script type="text/javascript" src="/ext/ext-all-debug.js"></script>
    
    <script type="text/javascript" src="/jsonrpc/jsonrpc.js"></script>
    <script type="text/javascript" src="/script/i18n.js"></script>
    <script type="text/javascript" src="/script/timezone.js"></script>
    <script type="text/javascript" src="/script/country.js"></script>
    <script type="text/javascript" src="/script/wizard.js"></script>

    <script type="text/javascript" src="script/setup.js"></script>

<c:if test="${param['console']==1}">
    <script type="text/javascript">
    if("http:"==window.location.protocol) {
        top.window.moveTo(1,1);
        if(Ext.isIE) {
            top.window.resizeTo(screen.availWidth,screen.availHeight);
        } else {
            top.window.outerHeight = top.screen.availHeight-30;
            top.window.outerWidth = top.screen.availWidth-30;
        }
    }
     </script>
</c:if>
    
    <script type="text/javascript">
      Ung.SetupWizard.currentSkin = "${ss.administrationClientSkin}";

      Ung.SetupWizard.CurrentValues = {
          timezone : "${timezone.ID}",
          languageMap : ${languageMap},
      };

      Ext.onReady(Ung.Setup.init);
    </script>
  </head>
  <body class="wizard">
    <div id="container">
      <!-- These extra divs/spans may be used as catch-alls to add extra imagery. -->
      <div id="extra-div-1"><span></span></div>
      <div id="extra-div-2"><span></span></div>
      <div id="extra-div-3"><span></span></div>
      <div id="extra-div-4"><span></span></div>
      <div id="extra-div-5"><span></span></div>
      <div id="extra-div-6"><span></span></div>
    </div>
  </body>
</html>
