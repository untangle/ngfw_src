/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.uvm.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

import com.untangle.uvm.LocalAppServerManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.networking.NetworkUtil;
import com.untangle.uvm.networking.internal.AccessSettingsInternal;
import com.untangle.uvm.networking.internal.AddressSettingsInternal;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.log4j.Logger;

/* the name outside valve is no longer legit, since this controls access to port 80 on the inside */
public abstract class OutsideValve extends ValveBase
{
    private static final int DEFAULT_HTTP_PORT = 80;

    private final Logger logger = Logger.getLogger(OutsideValve.class);

    protected void OutsideValve() { }

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
            response.sendError(response.SC_FORBIDDEN);
            return;
        }

        if ( logger.isDebugEnabled()) {
            logger.debug( "The request: " + request + " passed through the valve." );
        }

        /* If necessary call the next valve */
        Valve nextValve = getNext();
        if ( nextValve != null ) nextValve.invoke( request, response );
    }

    protected AccessSettingsInternal getAccessSettings()
    {
        return UvmContextFactory.context().networkManager().getAccessSettingsInternal();
    }

    protected AddressSettingsInternal getAddressSettings()
    {
        return UvmContextFactory.context().networkManager().getAddressSettingsInternal();
    }


    /* Unified way to determine if access to port 80 is allowed, override if behavior is different */
    protected boolean isInsecureAccessAllowed()
    {
        return getAccessSettings().getIsInsideInsecureEnabled();
    }

    /* Unified way to determine which parameter to check */
    protected abstract boolean isOutsideAccessAllowed();

    /* Unified way to get an error string */
    protected abstract String outsideErrorMessage();

    protected abstract String httpErrorMessage();

    private boolean isAccessAllowed(boolean isOutsideAccessAllowed, ServletRequest request)
    {
        String address = request.getRemoteAddr();

        if (isOutsideAccessAllowed && isInsecureAccessAllowed()) return true;

        try {
            if (null != address && InetAddress.getByName( address ).isLoopbackAddress()) return true;
        } catch (UnknownHostException e) {
            logger.warn( "Unable to parse the internet address: " + address );
        }

        int port = request.getLocalPort();

        /* This is insecure access on port 80 */
        if (port == DEFAULT_HTTP_PORT) return isInsecureAccessAllowed();

        /* This is secure access on the internal port */
        if (port == NetworkUtil.INTERNAL_OPEN_HTTPS_PORT) return true;

        /* This is secure access on the external port */
        if (port == getAddressSettings().getHttpsPort()) return isOutsideAccessAllowed;

        return false;
    }
}
