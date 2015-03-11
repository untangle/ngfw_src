<!DOCTYPE html>
<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib uri="http://java.untangle.com/jsp/uvm" prefix="uvm" %>
<html xmlns:uvm="http://java.untangle.com/jsp/uvm">
  <head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <title>Setup Wizard</title>
    <style type="text/css">
        @import "/ext5/packages/ext-theme-gray/build/resources/ext-theme-gray-all.css?s=${buildStamp}";
    </style>
    
    <uvm:skin src="adminNew.css?s=${buildStamp}" name="${skinSettings.skinName}"/>

    <script type="text/javascript" src="/ext5/ext-all-debug.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/ext5/packages/ext-theme-gray/build/ext-theme-gray.js?s=${buildStamp}"></script>
    
    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/i18n.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/country.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/wizardNew.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="script/setupNew.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/util.js?s=${buildStamp}"></script>

    <script type="text/javascript">
      var setup;
      function init() {
          setup=new Ung.Setup({});
          setup.init();
      };
      Ext.onReady(init);
    </script>
  </head>
  <body class="wizard">
    <div id="container">
      <!-- These extra divs/spans may be used as catch-alls to add extra imagery. -->
      <div id="extra-div-1"><span></span></div>
    </div>
  </body>
</html>
