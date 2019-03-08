<%@ page language="java" import="com.untangle.app.reports.*,com.untangle.uvm.*,com.untangle.uvm.util.*,com.untangle.uvm.reports.*,com.untangle.uvm.app.AppSettings,com.untangle.uvm.app.*,com.untangle.uvm.vnet.*,org.apache.log4j.helpers.AbsoluteTimeDateFormat,java.util.Properties, java.util.Map, java.net.URL, java.io.PrintWriter, javax.naming.*"
%><!DOCTYPE html>

<%
String buildStamp = getServletContext().getInitParameter("buildStamp");
UvmContext uvm = UvmContextFactory.context();
String company = uvm.brandingManager().getCompanyName();
String extjsTheme = uvm.skinManager().getSkinInfo().getExtjsTheme();

%>
<html>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title><%=company%> | Reports</title>

    <!-- JsonRPC -->
    <script src="/jsonrpc/jsonrpc.js"></script>

    <!-- Highchart lib, map -->
    <script src="/highcharts-6.0.2/highstock.js"></script>
    <script src="/highcharts-6.0.2/highcharts-3d.js"></script>
    <script src="/highcharts-6.0.2/highcharts-more.js"></script>
    <script src="/highcharts-6.0.2/exporting.js"></script>
    <script src="/highcharts-6.0.2/export-data.js"></script>
    <script src="/highcharts-6.0.2/no-data-to-display.js"></script>

    <!-- ExtJS lib & theme -->
    <script src="/ext6.2/ext-all-debug.js"></script>
    <script src="/ext6.2/classic/theme-<%=extjsTheme%>/theme-<%=extjsTheme%>.js"></script>
    <link href="/ext6.2/classic/theme-<%=extjsTheme%>/resources/theme-<%=extjsTheme%>-all.css" rel="stylesheet" />

    <!-- FontAwesome -->
    <link href="/ext6.2/fonts/font-awesome/css/font-awesome.min.css" rel="stylesheet" />

    <%-- Import custom fonts (see sass/_vars.scss) --%>
    <link href="/ext6.2/fonts/source-sans-pro/css/fonts.css" rel="stylesheet" />
    <link href="/ext6.2/fonts/roboto-condensed/css/fonts.css" rel="stylesheet" />

    <%-- Import reports css --%>
    <link href="/script/common/reports-all.css?s=<%=buildStamp%>" rel="stylesheet" />

    <%-- Import bootstrap --%>
    <script src="/script/common/bootstrap.js?s=<%=buildStamp%>"></script>
    <script>
        Ext.onReady(function () {
            // setups all initializations and load required scrips
            Bootstrap.load([
                '/script/common/util-all.js', // include custom grid module
                '/script/common/reports-all.js', // include reports module
                '/script/common/ungrid-all.js', // include custom grid module
                'script/app.js' // include this standalone reports app
            ], 'REPORTS', function (ex) {
                if (ex) { console.error(ex); return; };
                // if everything is initialized just launch the application
                var chartReport = Ext.Object.fromQueryString(window.location.search.substring(1));
                if(chartReport.reportChart == 1){
                    Ext.application({
                        extend: 'Ung.ChartApplication',
                        namespace: 'Ung',
                        servletContext: 'chart'
                    });
                }else{
                    Ext.application({
                        extend: 'Ung.Application',
                        namespace: 'Ung',
                        servletContext: 'reports'
                    });
                }
            });
        });
    </script>
 </head>
<body>
<div id="container" style="display:none;">
  <form name="downloadForm" id="downloadForm" method="post" action="csv">
    <input type="hidden" name="type" value=""/>
    <input type="hidden" name="arg1" value=""/>
    <input type="hidden" name="arg2" value=""/>
    <input type="hidden" name="arg3" value=""/>
    <input type="hidden" name="arg4" value=""/>
    <input type="hidden" name="arg5" value=""/>
    <input type="hidden" name="arg6" value=""/>
  </form>
</div>
</body>
</html>
