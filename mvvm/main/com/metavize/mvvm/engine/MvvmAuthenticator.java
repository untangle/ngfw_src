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

package com.metavize.mvvm.engine;

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


class MvvmAuthenticator extends BasicAuthenticator
{
    public static final String AUTH_NONCE_FIELD_NAME = "nonce";

    private static Log log = LogFactory.getLog(MvvmAuthenticator.class);

    /**
     * Descriptive information about this implementation.
     */
    protected static final String info =
        "com.metavize.mvvm.engine.MvvmAuthenticator/3.2";

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

        if (principal == null) {
            // No -- check for the magic cookie
            String authStr = request.getParameter(AUTH_NONCE_FIELD_NAME);
            if (authStr != null) {
                Realm realm = context.getRealm();
                if (realm instanceof MvvmRealm) {
                    log.debug("Attempting magic authentication with " + authStr);
                    principal = ((MvvmRealm)realm).authenticateWithNonce(authStr);
                    if (principal != null) {
                        log.debug("Succeeded for  " + principal);
                        register(request, response, principal, Constants.BASIC_METHOD,
                                 principal.getName(), null);
                        return true;
                    } else {
                        log.warn("Magic authentication failed");
                        return false;
                    }
                }
            }
        }
        return super.authenticate(request, response, config);
    }
}
