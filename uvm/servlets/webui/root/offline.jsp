<!DOCTYPE html>
<%@ page import="com.untangle.uvm.util.I18nUtil" %>
<%@ page import="com.untangle.uvm.UvmContext" %>
<%@ page import="com.untangle.uvm.UvmContextFactory" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.text.MessageFormat" %>

<% 
UvmContext uvm = UvmContextFactory.context();
Map<String,String> i18n_map = uvm.languageManager().getTranslations("untangle-vm");

MessageFormat fm = new MessageFormat("");
fm.applyPattern( I18nUtil.tr("Verify the {0}network settings{1} are correct and the Connectivity Test succeeds.", i18n_map ));
String[] messageArguments = {
    "<a href=\"#\" onclick=\"return openNetworkSettings()\">",
    "</a>"
};
%>
<html>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <title><%= I18nUtil.tr("Congratulations! Welcome", i18n_map ) %></title>
    <link rel="stylesheet" href="/skins/welcome/css/blueprint/screen.css" type="text/css" media="screen, projection" />
    <link rel="stylesheet" href="/skins/welcome/css/blueprint/print.css" type="text/css" media="print" /> 
    <!--[if IE]>
    <link rel="stylesheet" href="/skins/welcome/css/blueprint/ie.css" type="text/css" media="screen, projection" />
    <![endif]-->
    <link rel="stylesheet" href="/skins/welcome/css/include.css" type="text/css" media="screen, print, projection" />
    <style type="text/css">
        .logo{
            background-image:url('/images/BrandingLogo.png');
        }
    </style>
    <script type="text/javascript">
        function getMain(){
            return window.parent.Ung == null ? window.opener.Ung.Main : window.parent.Ung.Main;
        }
        function openNetworkSettings(){
            var main = getMain();
            if(main!=null){
                main.closeIframe();
                main.openConfig(main.configMap["network"]);
            } 
            return false;
        }
        function closeWindow(){
            var main = getMain();
            if(main!=null){
                main.closeIframe();
            }
        }
    </script>
</head>
<body>
    <div class="container full">
      <div class="span-5 box height-1 ">
        <div class="logo"></div>
      </div>
      <div class="full-width-1 height-1 box last">
        <h1 class="no-bottom-margin"><%= I18nUtil.tr("Congratulations!", i18n_map ) %></h1>
        <h2><%= I18nUtil.tr("Installation Complete", i18n_map ) %></h2>
      </div>
      <div class="full-width-2 push-05 last">
        <p><%= I18nUtil.tr("Welcome!", i18n_map ) %></p>
        <p><%= I18nUtil.tr("The installation is complete and ready for deployment. The next step is installing apps from the App Store.", i18n_map ) %></p>
        <p class="red"><%= I18nUtil.tr("Unfortunately, Your server was unable to contact the App Store.", i18n_map ) %></p>
        <p><%= I18nUtil.tr("Before Installing apps, this must be resolved.", i18n_map ) %></p>
        <span class="bold"><%= I18nUtil.tr("Possible Resolutions", i18n_map ) %></span>
        <ol>
          <li><%= fm.format( messageArguments  ) %></li>
          <li><%= I18nUtil.tr("Verify that there are no upstream firewalls blocking HTTP access to the internet.", i18n_map ) %></li>
          <li><%= I18nUtil.tr("Verify the external interface has the correct IP and DNS settings.", i18n_map ) %></li>
        </ol>
      </div>
      <div class="bottom">
        <a href="#" class="link" onclick="return closeWindow()" class="right"><%= I18nUtil.tr("Close this Window", i18n_map ) %></a>
      </div>
    </div>
</body>
</html>
