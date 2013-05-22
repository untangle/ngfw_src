/**
 * $Id: AdministrationOutsideAccessValve.java,v 1.00 2012/09/12 14:13:06 dmorris Exp $
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

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.TomcatManager;

public class AdministrationValve extends ValveBase
{
    private final Logger logger = Logger.getLogger(getClass());

    public AdministrationValve() { }

    public void invoke( Request request, Response response ) throws IOException, ServletException
    {
        if ( !isAccessAllowed( request )) {
            logger.warn( "The request: " + request + " denied by AdministrationValve." );
            String msg = administrationDenied();
            request.setAttribute(TomcatManager.UVM_WEB_MESSAGE_ATTR, msg);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if ( logger.isDebugEnabled()) {
            logger.debug( "The request: " + request + " allowed by AdministrationValve." );
        }

        /* If necessary call the next valve */
        Valve nextValve = getNext();
        if ( nextValve != null ) nextValve.invoke( request, response );
    }

    private String administrationDenied()
    {
        Map<String,String> i18n_map = UvmContextFactory.context().languageManager().getTranslations("untangle-libuvm");
        return I18nUtil.tr("HTTP administration is disabled.", i18n_map);
    }

    private boolean isAccessAllowed( ServletRequest request )
    {
        String address = request.getRemoteAddr();
        boolean isHttpAllowed = UvmContextFactory.context().networkManager().getNetworkSettings().getInsideHttpEnabled();

        logger.debug("isAccessAllowed( " + request + " ) [scheme: " + request.getScheme() + " HTTP allowed: " + isHttpAllowed + "]"); 

        /**
         * Always allow HTTP from 127.0.0.1
         */
        try {
            if (address != null && InetAddress.getByName( address ).isLoopbackAddress())
                return true;
        } catch (UnknownHostException e) {
            logger.warn( "Unable to parse the internet address: " + address );
        }
        
        /**
         * Otherwise only allow HTTP if enabled
         */
        if (request.getScheme().equals("http")) {
            if (!isHttpAllowed)
                logger.warn("isAccessAllowed( " + request + " ) denied. [scheme: " + request.getScheme() + " HTTP allowed: " + isHttpAllowed + "]"); 
            return isHttpAllowed;
        }
        else
            return true; /* https always allowed */
    }
}
