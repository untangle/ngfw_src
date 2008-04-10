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

package com.untangle.uvm.engine;

import java.io.IOException;
import java.security.Principal;

import com.untangle.uvm.security.UvmPrincipal;
import org.apache.catalina.Realm;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.Cookie;


/**
 * BasicAuthenticator for the UVMRealm.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class UvmAuthenticator extends BasicAuthenticator
{
    public static final String AUTH_NONCE_FIELD_NAME = "nonce";

    private static final String SSO_SESSION_PARAMETER_NAME = "jsessionidsso";

    private static Log log = LogFactory.getLog(UvmAuthenticator.class);

    /**
     * Descriptive information about this implementation.
     */
    protected static final String info =
        "com.untangle.uvm.engine.UvmAuthenticator/3.2";

    UvmAuthenticator()
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
     * Special cased to work with uvm magic cookies
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

        // Check for the single sign on cookie
        Cookie cookie = null;
        String ssoVal = null;
        Cookie cookies[] = request.getCookies();
        if (cookies == null)
            cookies = new Cookie[0];
        for (int i = 0; i < cookies.length; i++) {
            if (Constants.SINGLE_SIGN_ON_COOKIE.equals(cookies[i].getName())) {
                cookie = cookies[i];
                break;
            }
        }

        if (cookie == null) {
            // Check for the single sign on URL header
            ssoVal = getSsoSessionId(request);
            if (ssoVal != null) {
                cookie = new Cookie(Constants.SINGLE_SIGN_ON_COOKIE, ssoVal);
                cookie.setMaxAge(-1);
                cookie.setPath("/");
                request.addCookie(cookie);
            }
        }

        // Have we already authenticated someone?
        Principal principal = request.getUserPrincipal();

        debug("Authenticating against [", principal,"]");

        if (!isValidPrincipal(principal)) {
            debug("No UvmPrincipal, trying to find a principal from the session");
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
                if (realm instanceof UvmRealm) {
                    debug("Attempting magic authentication with ", authStr);
                    principal = ((UvmRealm)realm).authenticateWithNonce(authStr);
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
                } else {
                    debug("realm is not UvmRealm");
                }
            } else { /* No magic cookie, fall back to the default methods */
                debug("authStr is null, nonce not found");
            }
        }

        return super.authenticate(request, response, config);
    }

    protected String getSsoSessionId(Request req) {
        return req.getParameter(SSO_SESSION_PARAMETER_NAME);
    }

    private boolean isValidPrincipal(Principal principal)
    {
        return (null != principal && (principal instanceof UvmPrincipal));
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
