<!DOCTYPE html>
<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib uri="http://java.untangle.com/jsp/uvm" prefix="uvm" %>
<html xmlns:uvm="http://java.untangle.com/jsp/uvm">
  <head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>Setup Wizard</title>
    <style type="text/css">
        @import "/ext5/packages/ext-theme-gray/build/resources/ext-theme-gray-all.css?s=${buildStamp}";
    </style>
    
    <uvm:skin src="admin.css?s=${buildStamp}"  name="${skinSettings.skinName}"/>

    <script type="text/javascript" src="/ext5/ext-all-debug.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/ext5/packages/ext-theme-gray/build/ext-theme-gray.js?s=${buildStamp}"></script>
    
    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/i18n.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/script/wizard.js?s=${buildStamp}"></script>
    
    <script type="text/javascript" src="script/language.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="script/util.js?s=${buildStamp}"></script>

    <script type="text/javascript">
    Ext.onReady(function(){
        Ung.Language.init({
            languageList : ${languageList},
            language : "${language}"
        });
    });
    </script>
  </head>

  <body class="wizard">
    <div id="container"></div>
  </body>
</html>