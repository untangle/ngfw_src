<%@ page language="java" import="com.untangle.uvm.*, com.untangle.uvm.security.Tid, com.untangle.uvm.node.*, com.untangle.uvm.tapi.*, com.untangle.uvm.util.SessionUtil, org.apache.log4j.helpers.AbsoluteTimeDateFormat, java.util.List, java.util.Properties, java.net.URL, java.io.PrintWriter, javax.naming.*" %>

<HTML>
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<TITLE>Live Sessions</title>
</HEAD>
<BODY>
<%
  AbsoluteTimeDateFormat atdf = new AbsoluteTimeDateFormat();
  LocalUvmContext mc = LocalUvmContextFactory.context();
  LocalNodeManager tm = mc.nodeManager();
  StringBuffer buf;
  for (Tid tid : tm.nodeInstances()) {
      NodeContext tctx = tm.nodeContext(tid);
      if (tctx.getRunState() != NodeState.RUNNING)
          continue;
      NodeDesc tdesc = tctx.getNodeDesc();
%>
      <H1 ALIGN=CENTER> <%= tdesc.getName() %></H1>
<%
      // First show the node stats
      NodeStats tstats = null;
      try {
        tstats = tctx.getStats();
%>
      <TABLE BORDER=0>
      <TR>
      <TD>Start
<%
      buf = new StringBuffer(); atdf.format(tstats.startDate(), buf, null);
%>
      <TD><%= buf.toString() %>
      <TR>
      <TD>Last Configure
<%
      buf = new StringBuffer(); atdf.format(tstats.lastConfigureDate(), buf, null);
%>
      <TD><%= buf.toString() %>
      <TR>
      <TD>Last Activity
<%
      buf = new StringBuffer(); atdf.format(tstats.lastActivityDate(), buf, null);
%>
      <TD><%= buf.toString() %>
      <TR>
      <TD>Live TCP Sessions
      <TD><%= tstats.tcpSessionCount() %>
      <TR>
      <TD>Live UDP Sessions
      <TD><%= tstats.udpSessionCount() %>
      <TR>
      <TD>Total TCP Sessions
      <TD><%= tstats.tcpSessionTotal() %>
      <TR>
      <TD>Total UDP Sessions
      <TD><%= tstats.udpSessionTotal() %>
      <TR>
      <TD>Total TCP Session Requests
      <TD><%= tstats.tcpSessionRequestTotal() %>
      <TR>
      <TD>Total UDP Session Requests
      <TD><%= tstats.udpSessionRequestTotal() %>
      <TR>
      </TABLE>
      <BR>
      <P>
<%
      } catch (Exception x) {
      }
%>
<%
      com.untangle.uvm.tapi.IPSessionDesc[] sdescs = tctx.liveSessionDescs();
      sdescs = SessionUtil.sortDescs(sdescs);
      if (sdescs == null)
         continue;
%>
      <TABLE BORDER=1>
      <TR>
         <TH>ID
         <TH>Dir
         <TH>C State
         <TH>Client Addr : Port
         <TH>S State
         <TH>Server Addr : Port
         <TH>Created at
         <TH>Last Activity at
         <TH>C->T B
         <TH>T->S B
         <TH>S->T B
         <TH>T->C B
<%
      for (int j = 0; j < sdescs.length; j++) {
          com.untangle.uvm.tapi.IPSessionDesc sd = (com.untangle.uvm.tapi.IPSessionDesc) sdescs[j];
          SessionStats stats = sd.stats();
          char proto = 'N';
          if (sd instanceof com.untangle.uvm.tapi.UDPSessionDesc)
             proto = 'U';
          else if (sd instanceof com.untangle.uvm.tapi.TCPSessionDesc)
             proto = 'T';
%>
          <TR>
             <TD><%= proto %><%= sd.id() %>
             <TD><%= sd.isInbound() ? "In" : "Out" %>
             <TD><%= SessionUtil.prettyState(sd.clientState()) %>
             <TD><%= sd.clientAddr().getHostAddress() %>:<%= sd.clientPort() %>
             <TD><%= SessionUtil.prettyState(sd.serverState()) %>
             <TD><%= sd.serverAddr().getHostAddress() %>:<%= sd.serverPort() %>
<%
             buf = new StringBuffer(); atdf.format(stats.creationDate(), buf, null);
%>
             <TD><%= buf.toString() %>
<%
             buf = new StringBuffer(); atdf.format(stats.lastActivityDate(), buf, null);
%>
             <TD><%= buf.toString() %>
             <TD><%= stats.c2tBytes() %>
             <TD><%= stats.t2sBytes() %>
             <TD><%= stats.s2tBytes() %>
             <TD><%= stats.t2cBytes() %>
<%
      }
%>
      </TABLE>
      <BR>
      <P>
      <P>
<%
  }
%>
</BODY>
</HTML>

