<%@ page language="java" import="com.untangle.node.reporting.*,com.untangle.uvm.*,com.untangle.uvm.util.*,com.untangle.uvm.reports.*,com.untangle.uvm.node.NodeSettings,com.untangle.uvm.node.*,com.untangle.uvm.vnet.*,org.apache.log4j.helpers.AbsoluteTimeDateFormat,java.util.Properties, java.util.Map, java.net.URL, java.io.PrintWriter, javax.naming.*" 
%><!DOCTYPE html>

<%
String buildStamp = getServletContext().getInitParameter("buildStamp");

UvmContext uvm = UvmContextFactory.context();
Map<String,String> i18n_map = uvm.languageManager().getTranslations("untangle-libuvm");

String company = uvm.brandingManager().getCompanyName();
String companyUrl = uvm.brandingManager().getCompanyUrl();

ReportingNode node = (ReportingNode) UvmContextFactory.context().nodeManager().node("untangle-node-reporting");
ReportingManager reportingManager = null ;
boolean reportingEnabled = false;
boolean reportsAvailable = false;

if (node != null) {
   reportingManager = node.getReportingManager();
   reportingEnabled = reportingManager.isReportingEnabled();
   reportsAvailable = reportingManager.isReportsAvailable();
}

if (node == null || !reportsAvailable || !reportingEnabled) {
   String msg = I18nUtil.tr("No reports are available.", i18n_map);
   String disabledMsg = I18nUtil.tr("Reports is not installed into your rack or it is not turned on.<br />Reports are only generated when Reports is installed and turned on.", i18n_map);
   String emptyMsg = I18nUtil.tr("No reports have been generated.", i18n_map);

%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%=company%> | Reports</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<style type="text/css">
/* <![CDATA[ */
    @import url(/images/base.css?s=<%=buildStamp%>);
/* ]]> */
</style>
</head>
<body>
<div id="main" style="width: 500px; margin: 50px auto 0 auto;">
 <!-- Box Start -->
 <div class="main-top-left"></div><div class="main-top-right"></div><div class="main-mid-left"><div class="main-mid-right"><div class="main-mid">
 <!-- Content Start -->
    <div class="page_head">
        <a href="<%=companyUrl%>"><img src="/images/BrandingLogo.png" alt="<%=company%> Logo" /></a> <div><%=company%> Reports</div>
    </div>
    <hr />
    <center>
    <div style="padding: 10px 0; margin: 0 auto; width: 440px; text-align: left;">
        <b><i><%=msg%></i></b>
        <br /><br />

        <% if(!reportingEnabled){ %>
        <%=disabledMsg%>
        <% } else{ %>
        <%=emptyMsg%>
        <% } %>
    </div>
    </center>
    <hr />

 <!-- Content End -->
 </div></div></div><div class="main-bot-left"></div><div class="main-bot-right"></div>
 <!-- Box End -->
</div>
</body>
</html>
<%
} else if(request.getParameter("old")!=null) {
%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Reports</title>
    <style type="text/css">
      @import "/ext4/resources/css/ext-all-gray.css?s=<%=buildStamp%>";
      @import "/ext4/examples/ux/grid/css/GridFilters.css?s=<%=buildStamp%>";
      @import "/ext4/examples/ux/grid/css/RangeMenu.css?s=<%=buildStamp%>";
    </style>
    <script type="text/javascript" src="/ext4/ext-all-debug.js?s=<%=buildStamp%>"></script>

    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/script/i18n.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="script/reportsOld.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="script/utilOld.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="script/baseEventLogOld.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="script/eventLogOld.js?s=<%=buildStamp%>"></script>

    <script type="text/javascript">
<%
    String selectedApplication = request.getParameter("aname");
    String reportsDate = request.getParameter("rdate");
    String numDays = request.getParameter("duration");
    String drillType=request.getParameter("drillType");
    String drillValue= request.getParameter("drillValue");
    String args = "";
    if(selectedApplication != null && reportsDate != null && numDays != null){
        args = "selectedNode:{data:{id:'"+selectedApplication+"',text:'Summary'}},printView:true,selectedApplication:'"+selectedApplication+ "',reportsDate:{javaClass:'java.util.Date',time:"+reportsDate +"},numDays:"+numDays+",drillType:'" + drillType +"',drillValue:'" + drillValue +"'";
    }
%>
        Ext.onReady(function(){
            reports = new Ung.Reports({<%= args %>});
        });
    </script>
    
</head>
<body>
<div id="base">
<form name="downloadForm" id="downloadForm" method="post" action="csv" style="display:none;">
<input type="hidden" name="app" value=""/>
<input type="hidden" name="section" value=""/>
<input type="hidden" name="numDays" value=""/>
<input type="hidden" name="date" value=""/>
<input type="hidden" name="type" value=""/>
<input type="hidden" name="value" value=""/>
<input type="hidden" name="colList" value=""/>
        
</form>
</div>
<div id="window-container"></div>
</body>
</html>
<%
} else {
%>
<html>
<head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>Reports</title>
    <style type="text/css">
        @import "/ext5/packages/ext-theme-gray/build/resources/ext-theme-gray-all.css?s=<%=buildStamp%>";
        @import "/ext5/packages/sencha-charts/build/classic/resources/sencha-charts-all-debug.css?s=<%=buildStamp%>";
    </style>
    <script type="text/javascript" src="/ext5/ext-all-debug.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/ext5/packages/ext-theme-gray/build/ext-theme-gray.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/ext5/packages/sencha-charts/build/sencha-charts-debug.js?s=<%=buildStamp%>"></script>

    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/script/i18n.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="script/util.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/script/reports.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="script/main.js?s=<%=buildStamp%>"></script>

    <script type="text/javascript">
        Ext.onReady(function() {
            Ung.Main.init({buildStamp:'<%=buildStamp%>'})
        });
    </script>
 </head>
<body>
<div id="container" style="display:none;">
  <form name="downloadForm" id="downloadForm" method="post" action="download">
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
<%
}
%>