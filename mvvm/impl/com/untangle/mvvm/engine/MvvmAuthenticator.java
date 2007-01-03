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

package com.untangle.mvvm.engine;

import java.io.IOException;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.Realm;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.untangle.mvvm.portal.PortalLogin;
import com.untangle.mvvm.security.MvvmPrincipal;


class MvvmAuthenticator extends BasicAuthenticator
{
    public static final String AUTH_NONCE_FIELD_NAME = "nonce";

    private static Log log = LogFactory.getLog(MvvmAuthenticator.class);

    /**
     * Descriptive information about this implementation.
     */
    protected static final String info =
        "com.untangle.mvvm.engine.MvvmAuthenticator/3.2";

    MvvmAuthenticator()
    {
        setCache(true);
    }

    // ------------------------------------------------------------- Properties

    /**
     * Return descriptive information about this Valve implementation.
     */
    public String getInfo() {
        return (info);
    }

    /**
     * Authenticate the user making this request, based on the specified
     * login configuration.  Return <code>true</code> if any specified
     * constraint has been satisfied, or <code>false</code> if we have
     * created a response challenge already.
     *
     * Special cased to work with mvvm magic cookies
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param config    Login configuration describing how authentication
     *              should be performed
     *
     * @exception IOException if an input/output error occurs
     */
    public boolean authenticate(Request request,
                                Response response,
                                LoginConfig config)
        throws IOException {
        // Have we already authenticated someone?
        Principal principal = request.getUserPrincipal();
        
        debug("Authenticating against [", principal,"]");

        if (!isValidPrincipal(principal)) {
            debug("No MvvmPrincipal, trying to find a principal from the session");
            org.apache.catalina.Session session = request.getSessionInternal(false);
            if (null != session) {
                principal = session.getPrincipal();
                debug("Found principal[", principal, "] from session: ", session);
            }
        }

        if (isValidPrincipal(principal)) {
            debug("Found principal[", principal, "] accepting session.");
                
            return true;
        } else  {
            // No -- check for the magic cookie
            String authStr = request.getParameter(AUTH_NONCE_FIELD_NAME);
            if (authStr != null) {
                Realm realm = context.getRealm();
                if (realm instanceof MvvmRealm) {
                    debug("Attempting magic authentication with ", authStr);
                    principal = ((MvvmRealm)realm).authenticateWithNonce(authStr);
                    if (principal != null) {
                        debug("Succeeded for ", principal);
                        register(request, response, principal, Constants.BASIC_METHOD,
                                 principal.getName(), null);
                        /* Cache inside of the session as well, register doesn't appear to */
                        org.apache.catalina.Session session = request.getSessionInternal(true);
                        if (null!=session) {
                            session.setPrincipal(principal);
                            debug("Registered principal[", principal, "] with session [", session, "]");
                        }

                        return true;
                    } else {
                        log.warn("Magic authentication failed");
                        return false;
                    }
                }
            } /* No magic cookie, fall back to the default methods */
        }

        return super.authenticate(request, response, config);

//         if (isAuthenticated) {
//             org.apache.catalina.Session session = request.getSessionInternal(false);
//             if (null != session) {
//                 principal = session.getPrincipal();
//                 debug("Found principal[", request.getUserPrincipal(), "] from getUserPrincipal ");
//                 debug("Found principal[", principal, "] from session: ", session);
//             }
//         }
        
//         return isAuthenticated;
    }

    private boolean isValidPrincipal(Principal principal)
    {
        return (null != principal && (principal instanceof MvvmPrincipal));
    }

    private void debug(Object ... msgArray)
    {
        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (Object msg : msgArray) sb.append(null == msg ? "null" : msg.toString());
            log.debug(sb.toString());
        }
    }
}
