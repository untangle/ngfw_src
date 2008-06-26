/*
 * $HeadURL: svn://chef/branch/prod/jabsorb/work/src/uvm-lib/servlets/alpaca/src/com/untangle/uvm/servlet/alpaca/ProxyServlet.java $
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
 */

package com.untangle.uvm.webui.jabsorb;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONRPCServlet;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.client.RemoteUvmContext;
import com.untangle.uvm.webui.jabsorb.serializer.EnumSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.ExtendedListSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.ExtendedSetSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.HostNameSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.IPMaddrSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.IPaddrSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.LazyInitializerSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.MimeTypeSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.RFC2253NameSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.TimeZoneSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.UserSerializer;
import com.untangle.uvm.webui.jabsorb.serializer.URLSerializer;

/**
 * Initializes the JSONRPCBridge.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UtJsonRpcServlet extends JSONRPCServlet
{
    private static final String BRIDGE_ATTRIBUTE = "JSONRPCBridge";

    // HttpServlet methods ----------------------------------------------------

    public void service(HttpServletRequest req, HttpServletResponse resp)
        throws IOException
    {
        initSessionBridge(req);
        super.service(req, resp);
    }

    // private methods --------------------------------------------------------

    private void initSessionBridge(HttpServletRequest req)
    {
        HttpSession s = req.getSession();
        JSONRPCBridge b = (JSONRPCBridge)s.getAttribute(BRIDGE_ATTRIBUTE);
        if (null == b) {
            b = new JSONRPCBridge();
            s.setAttribute(BRIDGE_ATTRIBUTE, b);
            
            try {
            	// general serializers
                b.registerSerializer(new EnumSerializer());
                b.registerSerializer(new URLSerializer());
                // uvm related serializers
                b.registerSerializer(new IPMaddrSerializer());
                b.registerSerializer(new IPaddrSerializer());
                b.registerSerializer(new HostNameSerializer());
                b.registerSerializer(new TimeZoneSerializer());
                b.registerSerializer(new MimeTypeSerializer());
                b.registerSerializer(new RFC2253NameSerializer());
                b.registerSerializer(new UserSerializer());
                // hibernate related serializers
                b.registerSerializer(new LazyInitializerSerializer());
                b.registerSerializer(new ExtendedListSerializer());
                b.registerSerializer(new ExtendedSetSerializer());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            b.setCallbackController(new UtCallbackController(b));
            System.out.println(b.getCallbackController());

            RemoteUvmContext uvm = LocalUvmContextFactory.context().remoteContext();
            b.registerObject("RemoteUvmContext", uvm, RemoteUvmContext.class);
        }
    }
}
