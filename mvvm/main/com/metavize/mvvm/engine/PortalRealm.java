/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *

 * $Id$
 */

package com.metavize.mvvm.engine;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.portal.LocalPortalManager;
import com.metavize.mvvm.portal.PortalLoginKey;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.apache.log4j.Logger;

class PortalRealm extends RealmBase
{
    private static final Logger logger = Logger.getLogger(PortalRealm.class);

    public Principal authenticate(String username, String credentials)
    {
        String[] creds = credentials.split(",");
        String password = creds[0];
        InetAddress addr;
        try {
            addr = InetAddress.getByName(creds[1]);
        } catch (UnknownHostException exn) {
            addr = null;
        }

        MvvmLocalContext mctx = MvvmContextFactory.context();
        LocalPortalManager pm = mctx.portalManager();

        PortalLoginKey plk = pm.login(username, password, addr);

        if (null == plk) {
            return null;
        } else {
            List roles = new LinkedList();
            roles.add("user");

            return new GenericPrincipal(this, username, credentials, roles);
        }
    }

    // protected methods ------------------------------------------------------

    protected String getPassword(String username) { return null; }
    protected Principal getPrincipal(String username) { return null; }
    protected String getName() { return "PortalRealm"; }
}
