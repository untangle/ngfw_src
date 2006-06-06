/*
 * Copyright 1999-2001,2004-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.metavize.mvvm.engine;


import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Realm;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.authenticator.*;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.CharChunk;


/**
 * Sits in front of the Tomcat SingleSignOn to allow query string to contain
 * sso id, since we can't get cookies through jnlp.
 */

public class OurSingleSignOn extends ValveBase {


    // ----------------------------------------------------- Instance Variables


    public static final String SSO_SESSION_PARAMETER_NAME = "jsessionidsso";

    /**
     * Descriptive information about this Valve implementation.
     */
    protected static String info =
        "com.metavize.mvvm.engine.OurSingleSignOn";


    // ---------------------------------------------------------- Valve Methods

    /**
     * Perform single-sign-on support processing for this request.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public void invoke(Request request, Response response)
        throws IOException, ServletException {

        // Has a valid user already been authenticated?
        if (containerLog.isDebugEnabled())
            containerLog.debug("Process request for '" + request.getRequestURI() + "'");
        if (request.getUserPrincipal() != null) {
            if (containerLog.isDebugEnabled())
                containerLog.debug(" Principal '" + request.getUserPrincipal().getName() +
                    "' has already been authenticated");
            getNext().invoke(request, response);
            return;
        }

        // Check for the single sign on cookie
        if (containerLog.isDebugEnabled())
            containerLog.debug(" Checking for SSO cookie");
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

        // Invoke the next Valve in our pipeline
        getNext().invoke(request, response);

    }


    // ------------------------------------------------------ Protected Methods

    /**
     * Get sso session id from query string.
     */
    protected String getSsoSessionId(Request req) {
        return req.getParameter(SSO_SESSION_PARAMETER_NAME);
    }
}
