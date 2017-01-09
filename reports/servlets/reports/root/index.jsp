<%@ page language="java" import="com.untangle.node.reports.*,com.untangle.uvm.*,com.untangle.uvm.util.*,com.untangle.uvm.reports.*,com.untangle.uvm.node.NodeSettings,com.untangle.uvm.node.*,com.untangle.uvm.vnet.*,org.apache.log4j.helpers.AbsoluteTimeDateFormat,java.util.Properties, java.util.Map, java.net.URL, java.io.PrintWriter, javax.naming.*" 
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
    <style type="text/css">
        @import "/ext6/classic/theme-<%=extjsTheme%>/resources/theme-<%=extjsTheme%>-all.css?s=<%=buildStamp%>";
    </style>

    <script type="text/javascript" src="/highcharts/highstock.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/highcharts/highcharts-extra.js?s=<%=buildStamp%>"></script>

    <script type="text/javascript" src="/ext6/ext-all.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/ext6/classic/theme-<%=extjsTheme%>/theme-<%=extjsTheme%>.js?s=<%=buildStamp%>"></script>

    <script type="text/javascript">
        var config = {
            buildStamp: '<%=buildStamp%>'
        };
        <%
            if(request.getParameter("reportChart") != null){
        %>
            config.reportChart = '<%=request.getParameter("reportChart")%>';
            config.reportUniqueId = '<%=request.getParameter("reportUniqueId")%>';
            config.startDate = '<%=request.getParameter("startDate")%>';
            config.endDate = '<%=request.getParameter("endDate")%>';
        <%
            }
        %>
        Ext.onReady(function() {
            Ung.Main.init(config)
        });
    </script>

    <!-- global scripts -->
    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/script/i18n.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/script/tableConfig.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/script/charting.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/script/window.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/script/util.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/script/reports.js?s=<%=buildStamp%>"></script>

    <script type="text/javascript" src="script/main.js?s=<%=buildStamp%>"></script>

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
