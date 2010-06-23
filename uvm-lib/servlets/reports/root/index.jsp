<%@ page language="java" import="com.untangle.uvm.*,com.untangle.uvm.util.*,com.untangle.uvm.reports.*,com.untangle.uvm.security.Tid,com.untangle.uvm.node.*,com.untangle.uvm.vnet.*,com.untangle.uvm.util.SessionUtil,org.apache.log4j.helpers.AbsoluteTimeDateFormat,java.util.Properties, java.util.Map, java.net.URL, java.io.PrintWriter, javax.naming.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%
LocalUvmContext uvm = LocalUvmContextFactory.context();
Map<String,String> i18n_map = uvm.languageManager().getTranslations("untangle-libuvm");

String company = uvm.brandingManager().getCompanyName();
String companyUrl = uvm.brandingManager().getCompanyUrl();

RemoteReportingManager reportingManager = uvm.reportingManager();

String buildStamp = getServletContext().getInitParameter("buildStamp");

boolean reportingEnabled = reportingManager.isReportingEnabled();
boolean reportsAvailable = reportingManager.isReportsAvailable();
if (!reportsAvailable || !reportingEnabled) {
   String msg = I18nUtil.tr("No reports are available.", i18n_map);
   String disabledMsg = I18nUtil.tr("Reports is not installed into your rack or it is not turned on.<br />Reports are only generated when Reports is installed and turned on.", i18n_map);
   String emptyMsg = I18nUtil.tr("No reports currently available. Please check back in the morning for your reports.", i18n_map);

%><!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%=company%> | Reports</title>
<META content="IE=7.0000" http-equiv="X-UA-Compatible"/>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<style type="text/css">
/* <![CDATA[ */
    @import url(/images/base.css?s=<%=buildStamp%>);
/* ]]> */
</style>
</head>
<body>
<div id="main">
 <!-- Box Start -->
 <div class="main-top-left"></div><div class="main-top-right"></div><div class="main-mid-left"><div class="main-mid-right"><div class="main-mid">
 <!-- Content Start -->
    <div class="page_head">
        <a href="<%=companyUrl%>"><img src="/images/BrandingLogo.gif" alt="<%=company%> Logo" /></a> <div><%=company%> Reports</div>
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
} else {    
%>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <META content="IE=7.0000" http-equiv="X-UA-Compatible"/>
    <title>Reports</title>
    <style type="text/css">
        @import "/ext/resources/css/ext-all.css?s=<%=buildStamp%>";
    </style>
    <script type="text/javascript" src="/ext/source/core/Ext.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/ext/source/adapter/ext-base.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="/ext/ext-all-debug.js?s=<%=buildStamp%>"></script>

    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=<%=buildStamp%>"></script>

    <script type="text/javascript" src="/script/i18n.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="script/components.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="script/reports-components.js?s=<%=buildStamp%>"></script>
    <script type="text/javascript" src="script/reports.js?s=<%=buildStamp%>"></script>

    <script type="text/javascript">
        <%
        String selectedApplication = request.getParameter("aname");
        String reportsDate = request.getParameter("rdate");
        String numDays = request.getParameter("duration");
        String args = "";
        if(selectedApplication != null && reportsDate != null && numDays != null){
            args = "selectedNode:{attributes:{name:'"+selectedApplication+"',text:'Summary'}},printView:true,selectedApplication:'"+selectedApplication+ "',reportsDate:{javaclass:'java.util.Date',time:"+reportsDate +"},numDays:"+numDays+"";
        }
        %>
        Ext.onReady(function(){
            reports = new Ung.Reports({<%= args %>});
        });        
    </script>
 </head>
<body>

<div id="base">
</div>
<div id="window-container"></div>
</body>
</html>
<%
}
%>
