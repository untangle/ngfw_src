/*
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
 */

package com.untangle.uvm.webui.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.MarshallException;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.networking.AddressSettings;
import com.untangle.uvm.networking.BasicNetworkSettings;
import com.untangle.uvm.networking.LocalNetworkManager;
import com.untangle.uvm.networking.NetworkUtil;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.security.RegistrationInfo;
import com.untangle.uvm.servlet.ServletUtils;
import com.untangle.uvm.toolbox.UpgradeSettings;
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
import com.untangle.uvm.webui.jabsorb.serializer.URLSerializer;

/**
 * A servlet which will display the start page
 *
 * @author Catalin Matei <cmatei@untangle.com>
 */
@SuppressWarnings("serial")
public class SetupSettingsServlet extends HttpServlet
{
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        LocalUvmContext context = LocalUvmContextFactory.context();

        JSONSerializer js = new JSONSerializer();
        try {
            ServletUtils.getInstance().registerSerializers(js);
        } catch ( Exception e ) {
            throw new ServletException( "Unable to load the default serializer", e );
        }

        LocalNetworkManager nm = context.localNetworkManager();
        AddressSettings addressSettings = nm.getAddressSettings();
        HostName hostname = addressSettings.getHostName();
        if ( hostname.isEmpty() || !hostname.isQualified()) {
            addressSettings.setHostName( NetworkUtil.DEFAULT_HOSTNAME );
        }

        RegistrationInfo ri = new RegistrationInfo();
        ri.setMisc( new java.util.Hashtable<String,String>());

        // pick a random time.
        UpgradeSettings upgrade = context.toolboxManager().getUpgradeSettings();

        BasicNetworkSettings networkSettings = nm.getBasicSettings();
        try {
            request.setAttribute( "addressSettings", js.toJSON( addressSettings ));
            request.setAttribute( "interfaceArray", js.toJSON( nm.getInterfaceList( true )));
            request.setAttribute( "registrationInfo", js.toJSON( ri ));
            request.setAttribute( "users", js.toJSON( context.adminManager().getAdminSettings()));
            request.setAttribute( "upgradeSettings", js.toJSON( upgrade ));

            request.setAttribute( "mailSettings", js.toJSON( context.mailSender().getMailSettings()));
            request.setAttribute( "networkSettings", js.toJSON( networkSettings ));
        } catch ( MarshallException e ) {
            throw new ServletException( "Unable to serializer JSON", e );
        }

        String url="/WEB-INF/jsp/setupSettings.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        response.setContentType("text/javascript");
        rd.forward(request, response);
    }
}
