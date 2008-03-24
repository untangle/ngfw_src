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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.ServiceUnavailableException;

import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeStats;
import com.untangle.uvm.portal.Application;
import com.untangle.uvm.portal.BasePortalManager;
import com.untangle.uvm.security.LoginFailureReason;
import com.untangle.uvm.security.LogoutReason;
import com.untangle.uvm.util.TransactionWork;
import jcifs.smb.NtlmPasswordAuthentication;
import org.apache.catalina.Realm;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.realm.RealmBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Default Implementation of the PortalManager for use in open-source land.
 */
class DefaultPortalManager implements BasePortalManager
{
    private static final Application[] protoArr = new Application[] { };

    private final PortalApplicationManagerImpl appManager;

    DefaultPortalManager()
    {
        appManager = PortalApplicationManagerImpl.applicationManager();
    }

    // public methods ---------------------------------------------------------

    // BasePortalManager methods ---------------------------------------------

    public PortalApplicationManagerImpl applicationManager()
    {
        return appManager;
    }

    public boolean isLive(Principal p)
    {
        return false;
    }

    public String getUid(Principal p)
    {
        return null;
    }

    public void destroy()
    {
    }
}
