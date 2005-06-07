<%@ page language="java"  %>

<HTML>
<HEAD>
<META http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<TITLE>Metavize EdgeReport</TITLE>
</HEAD>
<BODY>
<%
        ServletContext sc = getServletContext();
        if (sc.getResource("/current") == null)
%>
	    <center>
	    <i>No reports are available, please check back tomorrow morning.</i><br/>
	    <i>Reports are generated every night automatically.</i>
	    </center>
<%        
        else
            response.sendRedirect("./current");
%>
</BODY>
</HTML>

