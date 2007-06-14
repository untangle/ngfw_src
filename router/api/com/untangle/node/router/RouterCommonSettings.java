/*
 * $HeadURL:$
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

package com.untangle.node.router;

import java.io.Serializable;

import java.util.List;

import com.untangle.uvm.node.Validatable;

import com.untangle.uvm.networking.BasicNetworkSettings;
import com.untangle.uvm.networking.ServicesSettings;
import com.untangle.uvm.networking.RedirectRule;
import com.untangle.uvm.networking.SetupState;

import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;

public interface RouterCommonSettings extends ServicesSettings, Validatable
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

    /** Methods used to update the current basic network settings object.
     *  this object is only used in validation */
    public BasicNetworkSettings getNetworkSettings();
    
    public void setNetworkSettings( BasicNetworkSettings networkSettings );
}
