<%@page language="java"%>
<%@page import="com.untangle.uvm.LocalUvmContext"%>
<%@page import="com.untangle.uvm.LocalUvmContextFactory"%>
<%@page import="com.untangle.node.cpd.CPD"%>
<%@page import="org.json.JSONObject" %>
<%--
 * $HeadURL: svn://chef/work/src/cpd/servlets/users/root/authenticate.jsp $
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
<%
response.setContentType( "application/json" );
LocalUvmContext uvm = LocalUvmContextFactory.context();
CPD cpd = (CPD)uvm.nodeManager().node("untangle-node-cpd");

boolean isAuthenticated = false;

if ( cpd != null ) {
    String username = request.getParameter("username");
    String password = request.getParameter("password");
    if ( username != null && password != null ) {
        username = username.trim();
        password = password.trim();
        isAuthenticated = cpd.authenticate( request.getRemoteAddr(), username, password, null );
    }
}

JSONObject js = new JSONObject();
js.put( "status", "success" );
js.put( "authenticate", isAuthenticated );

%>

<%= js.toString() %>
