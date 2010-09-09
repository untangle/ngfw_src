<%@ page language="java" import="com.untangle.uvm.*, com.untangle.uvm.security.NodeId, com.untangle.uvm.node.*, com.untangle.uvm.vnet.*, com.untangle.uvm.util.SessionUtil, org.apache.log4j.helpers.AbsoluteTimeDateFormat, java.util.List, java.util.Properties, java.net.URL, java.io.PrintWriter, javax.naming.*" %>
<%--
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
--%>

<HTML>
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<TITLE>Live Sessions</title>
    <META content="IE=7.0000" http-equiv="X-UA-Compatible"/>
</HEAD>
<BODY>
<%
  AbsoluteTimeDateFormat atdf = new AbsoluteTimeDateFormat();
  LocalUvmContext mc = LocalUvmContextFactory.context();
  NodeManager tm = mc.nodeManager();
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
      Stats tstats = null;
      try {
        tstats = tctx.getStats();
%>
      <TABLE BORDER=0>
      <TR>
      <TD>Start
<%
      buf = new StringBuffer(); atdf.format(tstats.getStartDate(), buf, null);
%>
      <TD><%= buf.toString() %>
      <TR>
      <TD>Last Configure
<%
      buf = new StringBuffer(); atdf.format(tstats.getLastConfigureDate(), buf, null);
%>
      <TD><%= buf.toString() %>
      <TR>
      <TD>Last Activity
<%
      buf = new StringBuffer(); atdf.format(tstats.getLastActivityDate(), buf, null);
%>
      <TD><%= buf.toString() %>
      <TR>
      <TD>Live TCP Sessions
      <TD><%= tstats.getTcpSessionCount() %>
      <TR>
      <TD>Live UDP Sessions
      <TD><%= tstats.getUdpSessionCount() %>
      <TR>
      <TD>Total TCP Sessions
      <TD><%= tstats.getTcpSessionTotal() %>
      <TR>
      <TD>Total UDP Sessions
      <TD><%= tstats.getUdpSessionTotal() %>
      <TR>
      <TD>Total TCP Session Requests
      <TD><%= tstats.getTcpSessionRequestTotal() %>
      <TR>
      <TD>Total UDP Session Requests
      <TD><%= tstats.getUdpSessionRequestTotal() %>
      <TR>
      </TABLE>
      <BR>
      <P>
<%
      } catch (Exception x) {
      }
%>
<%
      com.untangle.uvm.vnet.IPSessionDesc[] sdescs = tctx.liveSessionDescs();
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
          com.untangle.uvm.vnet.IPSessionDesc sd = (com.untangle.uvm.vnet.IPSessionDesc) sdescs[j];
          SessionStats stats = sd.stats();
          char proto = 'N';
          if (sd instanceof com.untangle.uvm.vnet.UDPSessionDesc)
             proto = 'U';
          else if (sd instanceof com.untangle.uvm.vnet.TCPSessionDesc)
             proto = 'T';
%>
          <TR>
             <TD><%= proto %><%= sd.id() %>
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

