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
import com.untangle.uvm.portal.Application;
import com.untangle.uvm.portal.BasePortalManager;
import com.untangle.uvm.security.LoginFailureReason;
import com.untangle.uvm.security.LogoutReason;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeStats;
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

    private final UvmContextImpl uvmContext;
    private final PortalApplicationManagerImpl appManager;

    DefaultPortalManager(UvmContextImpl uvmContext)
    {
        this.uvmContext = uvmContext;

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
