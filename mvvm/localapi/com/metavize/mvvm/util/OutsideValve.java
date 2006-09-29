/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.mvvm.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.networking.LocalNetworkManager;
import com.metavize.mvvm.networking.NetworkUtil;
import com.metavize.mvvm.networking.RemoteSettingsListener;
import com.metavize.mvvm.networking.internal.RemoteInternalSettings;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.log4j.Logger;

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

            request.setAttribute( "com.metavize.mvvm.util.errorpage.error-message", errorMessage());

            response.sendError( response.SC_FORBIDDEN );
            return;
        }

        if ( logger.isDebugEnabled()) {
            logger.debug( "The request: " + request + " passed through the valve." );
        }

        /* If necessary call the next valve */
        Valve nextValve = getNext();
        if ( nextValve != null ) nextValve.invoke( request, response );
    }

    protected RemoteInternalSettings getRemoteSettings()
    {
        return MvvmContextFactory.context().networkManager().getRemoteInternalSettings();

    }

    /* Unified way to determine which parameter to check */
    protected abstract boolean isOutsideAccessAllowed();

    /* Unified way to determine which parameter to check */
    protected abstract String errorMessage();

    private boolean isAccessAllowed(boolean isOutsideAccessAllowed, ServletRequest request)
    {
        String address = request.getRemoteAddr();

        if (isOutsideAccessAllowed) return true;

        try {
            if (null != address && InetAddress.getByName( address ).isLoopbackAddress()) return true;
        } catch (UnknownHostException e) {
            logger.warn( "Unable to parse the internet address: " + address );
        }


        int port = request.getLocalPort();

        if (port == DEFAULT_HTTP_PORT) return true;
        if (port == NetworkUtil.INTERNAL_OPEN_HTTPS_PORT) return true;

        return false;
    }
}
