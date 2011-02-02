/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */
package com.untangle.uvm.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.log4j.Logger;

import com.untangle.uvm.LocalAppServerManager;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.networking.NetworkUtil;
import com.untangle.uvm.networking.AccessSettings;
import com.untangle.uvm.networking.AddressSettings;

/* the name outside valve is no longer legit, since this controls access to port 80 on the inside */
public abstract class OutsideValve extends ValveBase
{
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int SECONDARY_HTTP_PORT = 64156;

    private final Logger logger = Logger.getLogger(OutsideValve.class);

    protected OutsideValve() { }

    public void invoke( Request request, Response response )
        throws IOException, ServletException
    {
        if ( !isAccessAllowed( isOutsideAccessAllowed(), request )) {
            /* Block the session here */
            if ( logger.isDebugEnabled()) {
                logger.debug( "The request: " + request + " caught by OutsideValve." );
            }

            String msg = request.getLocalPort() == DEFAULT_HTTP_PORT
                ? httpErrorMessage() : outsideErrorMessage();
            request.setAttribute(LocalAppServerManager.UVM_WEB_MESSAGE_ATTR, msg);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if ( logger.isDebugEnabled()) {
            logger.debug( "The request: " + request + " passed through the valve." );
        }

        /* If necessary call the next valve */
        Valve nextValve = getNext();
        if ( nextValve != null ) nextValve.invoke( request, response );
    }

    protected AccessSettings getAccessSettings()
    {
        return LocalUvmContextFactory.context().networkManager().getAccessSettings();
    }

    protected AddressSettings getAddressSettings()
    {
        return LocalUvmContextFactory.context().networkManager().getAddressSettings();
    }


    /* Unified way to determine if access to port 80 is allowed, override if behavior is different */
    protected boolean isInsecureAccessAllowed()
    {
        return getAccessSettings().getIsInsideInsecureEnabled();
    }

    /* Unified way to determine which parameter to check */
    protected abstract boolean isOutsideAccessAllowed();

    /* Unified way to get an error string */
    protected String outsideErrorMessage()
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        Map<String,String> i18n_map = uvm.languageManager().getTranslations("untangle-libuvm");
        return I18nUtil.tr("Off-site administration is disabled.", i18n_map);
    }

    protected String httpErrorMessage()
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        Map<String,String> i18n_map = uvm.languageManager().getTranslations("untangle-libuvm");
        return I18nUtil.tr("standard access", i18n_map);
    }

    private boolean isAccessAllowed(boolean isOutsideAccessAllowed, ServletRequest request)
    {
        String address = request.getRemoteAddr();
        boolean isInsecureAccessAllowed = isInsecureAccessAllowed();

        if (isOutsideAccessAllowed && isInsecureAccessAllowed) return true;

        try {
            if (null != address && InetAddress.getByName( address ).isLoopbackAddress()) return true;
        } catch (UnknownHostException e) {
            logger.warn( "Unable to parse the internet address: " + address );
        }

        int port = request.getLocalPort();

        /* This is insecure access on port 80 */
        if (port == DEFAULT_HTTP_PORT) {
            return isInsecureAccessAllowed;
        }
        if (port == SECONDARY_HTTP_PORT) {
            return isInsecureAccessAllowed;
        }

        int blockPagePort = LocalUvmContextFactory.context().networkManager().getAccessSettings().getBlockPagePort();
        
        if ( port == blockPagePort ) {
            return isInsecureAccessAllowed;
        }
        
        /* This is secure access on the internal port */
        if (port == NetworkUtil.INTERNAL_OPEN_HTTPS_PORT) return true;

        /* This is secure access on the external port */
        if (port == getAddressSettings().getHttpsPort()) return isOutsideAccessAllowed;

        if (request.getScheme().equals("https")) return isOutsideAccessAllowed;

        return false;
    }
}
