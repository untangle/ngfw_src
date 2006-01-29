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
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.Realm;
import org.apache.catalina.util.Base64;
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
    public boolean authenticate(HttpRequest request,
                                HttpResponse response,
                                LoginConfig config)
        throws IOException {

        // Have we already authenticated someone?
        Principal principal =
            ((HttpServletRequest) request.getRequest()).getUserPrincipal();
        String ssoId = (String) request.getNote(Constants.REQ_SSOID_NOTE);
        if (principal != null) {
            if (log.isDebugEnabled())
                log.debug("Already authenticated '" + principal.getName() + "'");
            // Associate the session with any existing SSO session
            if (ssoId != null)
                associate(ssoId, getSession(request, true));
            return (true);
        }

        // Is there an SSO session against which we can try to reauthenticate?
        if (ssoId != null) {
            if (log.isDebugEnabled())
                log.debug("SSO Id " + ssoId + " set; attempting " +
                          "reauthentication");
            /* Try to reauthenticate using data cached by SSO.  If this fails,
               either the original SSO logon was of DIGEST or SSL (which
               we can't reauthenticate ourselves because there is no
               cached username and password), or the realm denied
               the user's reauthentication for some reason.
               In either case we have to prompt the user for a logon */
            if (reauthenticateFromSSO(ssoId, request))
                return true;
        }

        // Validate any credentials already included with this request
        HttpServletRequest hreq =
            (HttpServletRequest) request.getRequest();
        HttpServletResponse hres =
            (HttpServletResponse) response.getResponse();

        // First check for the magic cookie
        String authStr = hreq.getParameter(AUTH_NONCE_FIELD_NAME);
        if (authStr != null) {
            Realm realm = context.getRealm();
            if (realm instanceof MvvmRealm) {
                log.debug("Attempting magic authentication with " + authStr);
                principal = ((MvvmRealm)realm).authenticateWithNonce(authStr);
                if (principal != null) {
                    log.debug("Succeeded for  " + principal);
                    register(request, response, principal, Constants.BASIC_METHOD,
                             principal.getName(), null);
                    return (true);
                } else {
                    log.warn("Magic authentication failed");
                }
            }
        }

        String authorization = request.getAuthorization();
        String username = parseUsername(authorization);
        String password = parsePassword(authorization);
        principal = context.getRealm().authenticate(username, password);
        if (principal != null) {
            register(request, response, principal, Constants.BASIC_METHOD,
                     username, password);
            return (true);
        }

        // Send an "unauthorized" response and an appropriate challenge
        String realmName = config.getRealmName();
        if (realmName == null)
            realmName = hreq.getServerName() + ":" + hreq.getServerPort();
    //        if (log.isDebugEnabled())
    //            log.debug("Challenging for realm '" + realmName + "'");
        hres.setHeader("WWW-Authenticate",
                       "Basic realm=\"" + realmName + "\"");
        hres.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        //      hres.flushBuffer();
        return (false);

    }
}
