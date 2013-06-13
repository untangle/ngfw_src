<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" %>

<%@ taglib uri="http://java.untangle.com/jsp/uvm" prefix="uvm" %>

<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:uvm="http://java.untangle.com/jsp/uvm">
  <head>
    <title>Setup Wizard</title>
    <style type="text/css">
        @import "/ext4/resources/css/ext-all-gray.css?s=${buildStamp}";
        @import "/ext4/examples/ux/css/CheckHeader.css?s=${buildStamp}";
    </style>
    
    <uvm:skin src="admin.css?s=${buildStamp}" name="${skinSettings.skinName}"/>

    <script type="text/javascript" src="/ext4/ext-all-debug.js?s=${buildStamp}"></script>
	<script type="text/javascript" src="/ext4/examples/ux/data/PagingMemoryProxy.js?s=${buildStamp}"></script>
	<script type="text/javascript" src="/ext4/examples/ux/CheckColumn.js?s=${buildStamp}"></script>
    
    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/i18n.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/country.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/wizard.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="script/setup.js?s=${buildStamp}"></script>

    <script type="text/javascript">
      Ung.SetupWizard.currentSkin = "${skinSettings.skinName}";
      Ung.SetupWizard.CurrentValues = {
          timezone : "${timezone.ID}",
          languageMap : ${languageMap}
      };
      Ext.onReady(Ung.Setup.init);
    </script>
  </head>
  <body class="wizard">
    <div id="container">
      <!-- These extra divs/spans may be used as catch-alls to add extra imagery. -->
      <div id="extra-div-1"><span></span></div>
    </div>
  </body>
</html>
