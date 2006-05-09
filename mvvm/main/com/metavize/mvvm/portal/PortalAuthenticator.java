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

package com.metavize.mvvm.portal;

import java.io.IOException;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PortalAuthenticator extends BasicAuthenticator
{
    private static Log log = LogFactory.getLog(PortalAuthenticator.class);

    protected static final String info =
        "com.metavize.mvvm.engine.PortalAuthenticator/4.0";


    public String getInfo() {
        return info;
    }

    public boolean authenticate(HttpRequest request, HttpResponse response,
                                LoginConfig config)
        throws IOException {

        HttpServletRequest req = (HttpServletRequest)request.getRequest();
        HttpServletResponse resp = (HttpServletResponse)response.getResponse();
        Principal principal = req.getUserPrincipal();

        String ssoId = (String)request.getNote(Constants.REQ_SSOID_NOTE);

        if (null != principal) {
            if (log.isDebugEnabled()) {
                log.debug("Already authenticated '" + principal.getName()
                          + "'");
            }
            if (ssoId != null)
                associate(ssoId, getSession(request, true));
            return true;
        } else if (null != ssoId) {
            if (log.isDebugEnabled()) {
                log.debug("SSO Id " + ssoId + " set; attempting " +
                          "reauthentication");
            }
            if (reauthenticateFromSSO(ssoId, request)) {
                return true;
            }
        }

        String authorization = request.getAuthorization();
        String username = parseUsername(authorization);
        String password = parsePassword(authorization);

        MvvmLocalContext mctx = MvvmContextFactory.context();
        LocalPortalManager pm = mctx.portalManager();

        String credentials = password + "," + req.getRemoteAddr();

        principal = context.getRealm().authenticate(username, credentials);

        if (null != principal) {
            register(request, response, principal, Constants.BASIC_METHOD,
                     username, password);
            return true;
        }

        String realmName = config.getRealmName();
        if (null == realmName) {
            realmName = req.getServerName() + ":" + req.getServerPort();
        }

        resp.setHeader("WWW-Authenticate",
                       "Basic realm=\"" + realmName + "\"");
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}
