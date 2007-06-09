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

package com.untangle.mvvm;

import java.net.InetAddress;

import java.util.List;

import com.untangle.mvvm.ArgonException;

import com.untangle.mvvm.localapi.LocalIntfManager;
import com.untangle.mvvm.localapi.SessionMatcher;

import com.untangle.mvvm.tran.firewall.InterfaceRedirect;

public interface ArgonManager
{    
    /** Set the list of interface overrides */
    public void setInterfaceOverrideList( List<InterfaceRedirect>overrideList );

    /** Clear the list of interface overrides */
    public void clearInterfaceOverrideList();

    /** Get the outgoing argon interface for an IP address */
    public byte getOutgoingInterface( InetAddress destination ) throws ArgonException;

    /** Get the interface manager */
    public LocalIntfManager getIntfManager();
    
    /** Get the number of sessions from the VectronTable */
    public int getSessionCount();
    
    /** Shutdown all of the sessions that match <code>matcher</code> */
    public void shutdownMatches( SessionMatcher matcher );
}
