/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.policy;

import java.util.*;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.node.IPSessionDesc;

/**
 * System Policy Rules.  These are the "fallback" matchers in the
 * policy selector, rows are created automatically by the system when
 * interfaces are added and cannot be added or deleted by the user.
 *
 * @author
 * @version 1.0
 */
@Entity
@Table(name="u_system_policy_rule", schema="settings")
public class SystemPolicyRule extends PolicyRule
{
    /* settings */
    protected byte clientIntf;
    protected byte serverIntf;

    // constructors -----------------------------------------------------------

    SystemPolicyRule() { }

    public SystemPolicyRule(byte clientIntf, byte serverIntf, Policy policy,
                            boolean inbound) {
        super(true, policy, inbound);
        this.clientIntf = clientIntf;
        this.serverIntf = serverIntf;
    }

    @Transient
    public boolean isSameRow(SystemPolicyRule pr)
    {
        return getId().equals(pr.getId());
    }

    // accessors --------------------------------------------------------------

    /**
     * Returns the client interface
     *
     * @return the interface the client must be on to match this rule
     */
    @Column(name="client_intf", nullable=false)
    public byte getClientIntf()
    {
        return clientIntf;
    }

    public void setClientIntf(byte clientIntf)
    {
        this.clientIntf = clientIntf;
    }

    /**
     * Returns the server interface
     *
     * @return the interface the server must be on to match this rule
     */
    @Column(name="server_intf", nullable=false)
    public byte getServerIntf()
    {
        return serverIntf;
    }

    public void setServerIntf(byte serverIntf)
    {
        this.serverIntf = serverIntf;
    }

    // PolicyRule methods -----------------------------------------------------

    public boolean matches(IPSessionDesc sd)
    {
        return clientIntf == sd.clientIntf()
            && serverIntf == sd.serverIntf();
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof SystemPolicyRule)) {
            return false;
        } else {
            SystemPolicyRule pr = (SystemPolicyRule)o;
            return ((policy == null ? pr.policy == null : policy.equals(pr.policy)) &&
                    clientIntf == pr.clientIntf &&
                    serverIntf == pr.serverIntf &&
                    inbound == pr.inbound);
        }
    }

    public int hashCode()
    {
        return (null == policy ? 0 : policy.hashCode()) + clientIntf * 7 + serverIntf * 5;
    }
}
