<%@ page language="java" import="com.metavize.mvvm.*, com.metavize.mvvm.security.Tid, com.metavize.mvvm.tran.*, com.metavize.mvvm.tapi.*, com.metavize.mvvm.util.SessionUtil, org.apache.log4j.helpers.AbsoluteTimeDateFormat, java.util.Properties, java.net.URL, javax.naming.*" %>

<HTML>
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<TITLE>Live Sessions</title>
</HEAD>
<BODY>
<%
  AbsoluteTimeDateFormat atdf = new AbsoluteTimeDateFormat();
  MvvmContext mc = MvvmRemoteContextFactory.localLogin();
  TransformManager tm = mc.transformManager();
  Tid[] tids = tm.transformInstances();
  StringBuffer buf;
  for (int i = 0; i < tids.length; i++) {
      TransformContext tctx = tm.transformContext(tids[i]);
      if (tctx.getRunState() != TransformState.RUNNING)
          continue;
      TransformDesc tdesc = tctx.getTransformDesc();
      IPSessionDesc[] sdescs = tctx.liveSessionDescs();
      sdescs = SessionUtil.sortDescs(sdescs);
      if (sdescs == null)
         continue;
%>
      <H1 ALIGN=CENTER> Live sessions for <%= tdesc.getName() %></H1>
      <TABLE BORDER=1 ALIGN=CENTER>
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
          IPSessionDesc sd = (IPSessionDesc) sdescs[j];
          SessionStats stats = sd.stats();
          char proto = 'N';
          if (sd instanceof UDPSessionDesc)
             proto = 'U';
          else if (sd instanceof TCPSessionDesc)
             proto = 'T';
%>
          <TR>
             <TD><%= proto %><%= sd.id() %>
             <TD><%= sd.direction() == IPSessionDesc.INBOUND ? "In" : "Out" %>
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

