/**
 * $Id$
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
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SystemSettings;
import com.untangle.uvm.networking.NetworkUtil;

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

            String msg = request.getLocalPort() == DEFAULT_HTTP_PORT ? httpErrorMessage() : outsideErrorMessage();
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

    protected SystemSettings getSystemSettings()
    {
        return UvmContextFactory.context().systemManager().getSettings();
    }

    /* Unified way to determine if access to port 80 is allowed, override if behavior is different */
    protected boolean isHttpAccessAllowed()
    {
        return getSystemSettings().getInsideHttpEnabled();
    }

    /* Unified way to determine which parameter to check */
    protected abstract boolean isOutsideAccessAllowed();

    /* Unified way to get an error string */
    protected String outsideErrorMessage()
    {
        UvmContext uvm = UvmContextFactory.context();
        Map<String,String> i18n_map = uvm.languageManager().getTranslations("untangle-libuvm");
        return I18nUtil.tr("Off-site administration is disabled.", i18n_map);
    }

    protected String httpErrorMessage()
    {
        UvmContext uvm = UvmContextFactory.context();
        Map<String,String> i18n_map = uvm.languageManager().getTranslations("untangle-libuvm");
        return I18nUtil.tr("Permission denied.", i18n_map);
    }

    private boolean isAccessAllowed(boolean outsideAccessAllowed, ServletRequest request)
    {
        String address = request.getRemoteAddr();
        boolean isHttpAccessAllowed = isHttpAccessAllowed();

        if (outsideAccessAllowed && isHttpAccessAllowed) return true;

        try {
            if (null != address && InetAddress.getByName( address ).isLoopbackAddress()) return true;
        } catch (UnknownHostException e) {
            logger.warn( "Unable to parse the internet address: " + address );
        }

        int port = request.getLocalPort();

        /* This is insecure access on port 80 */
        if (port == DEFAULT_HTTP_PORT) 
            return isHttpAccessAllowed;

        /* IF traffic comes to this port its because Http admin is disable, but block pages are OK */
        if (port == SECONDARY_HTTP_PORT) 
            return true;
        
        /* This is secure access on the internal port */
        if (port == NetworkUtil.INTERNAL_OPEN_HTTPS_PORT)
            return true;

        /* This is secure access on the external port */
        if (port == getSystemSettings().getHttpsPort()) return outsideAccessAllowed;

        if (request.getScheme().equals("https")) return outsideAccessAllowed;

        return false;
    }
}
