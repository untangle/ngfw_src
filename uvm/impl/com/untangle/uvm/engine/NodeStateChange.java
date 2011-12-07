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


import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.security.NodeId;

/**
 * Record of node state change.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
@Entity
@Table(name="u_node_state_change", schema="events")
class NodeStateChange extends LogEvent
{
    private NodeId tid;
    private NodeState state;

    // constructors -----------------------------------------------------------

    NodeStateChange() { }

    NodeStateChange(NodeId tid, NodeState state)
    {
        this.tid = tid;
        this.state = state;
    }

    // accessors --------------------------------------------------------------

    /**
     * State the node has changed into.
     *
     * @return node state at time of log.
     */
    @Enumerated(EnumType.STRING)
    @Type(type="com.untangle.uvm.type.NodeStateUserType")
    NodeState getState()
    {
        return state;
    }

    void setState(NodeState state)
    {
        this.state = state;
    }

    /**
     * Node id.
     *
     * @return tid for this instance.
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    NodeId getNodeId()
    {
        return tid;
    }

    void setNodeId(NodeId tid)
    {
        this.tid = tid;
    }

    // LogEvent methods -------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("tid", tid.toString());
        sb.addField("state", state.toString());
    }

    @Transient
    public String getSyslogId()
    {
        return "Software_Appliance"; // XXX
    }

    // reuse default getSyslogPriority
}
