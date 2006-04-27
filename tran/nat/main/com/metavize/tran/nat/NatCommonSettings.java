/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;

import java.util.List;

import com.metavize.mvvm.tran.Validatable;
import java.io.Serializable;

import com.metavize.mvvm.networking.ServicesSettings;
import com.metavize.mvvm.networking.RedirectRule;
import com.metavize.mvvm.networking.SetupState;

import com.metavize.mvvm.tran.HostName;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.firewall.ip.IPDBMatcher;

public interface NatCommonSettings extends ServicesSettings, Validatable
{
    // !!!!! private static final long serialVersionUID = 4349679825783697834L;

    /** The current mode(setting the state is a package protected operation) */
    public SetupState getSetupState();

    /**
     * List of the redirect rules.
     *
     */
    public List<RedirectRule> getRedirectList();

    public void setRedirectList( List<RedirectRule> s );

    /**
     * List of the global redirects, these are redirects that require the user to specify all parameters
     */
    public List<RedirectRule> getGlobalRedirectList();
    
    public void setGlobalRedirectList( List<RedirectRule> newValue );

    /**
     * List of the local redirects, these are redirects for 'Virtual Servers or Applications'
     */
    public List<RedirectRule> getLocalRedirectList();
    
    public void setLocalRedirectList( List<RedirectRule> newValue );

    /**
     * List of all of the matchers available for local redirects
     */
    public List<IPDBMatcher> getLocalMatcherList();
}
