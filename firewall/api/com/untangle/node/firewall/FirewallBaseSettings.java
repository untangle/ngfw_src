/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/firewall/api/com/untangle/node/firewall/FirewallSettings.java $
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

package com.untangle.node.firewall;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

@Embeddable
public class FirewallBaseSettings implements Serializable
{
    private static final long serialVersionUID = 8977728688196745416L;
    private boolean quickExit = true;
    private boolean rejectSilently = true;
    private boolean isDefaultAccept = true;

    private int firewallRulesLength;

    /**
     * If true, exit on the first positive or negative match.  Otherwise, exit
     * on the first negative match.
     */
    @Column(name="is_quickexit", nullable=false)
    public boolean isQuickExit()
    {
        return this.quickExit;
    }

    public void setQuickExit(boolean b)
    {
        this.quickExit = b;
    }

    /**
     * If true, the session is rejected quietly (default), otherwise
     * the connection is rejected silently.
     */
    @Column(name="is_reject_silent", nullable=false)
    public boolean isRejectSilently()
    {
        return this.rejectSilently;
    }

    public void setRejectSilently(boolean b)
    {
        this.rejectSilently = b;
    }

    /**
     * If true, the session is accepted if it doesn't match any other rules.
     */
    @Column(name="is_default_accept", nullable=false)
    public boolean isDefaultAccept()
    {
        return this.isDefaultAccept;
    }

    public void setDefaultAccept(boolean b)
    {
        this.isDefaultAccept = b;
    }

    @Transient
    public int getFirewallRulesLength()
    {
        return this.firewallRulesLength;
    }

    public void setFirewallRulesLengh(int firewallRulesLength)
    {
        this.firewallRulesLength = firewallRulesLength;
    }
}