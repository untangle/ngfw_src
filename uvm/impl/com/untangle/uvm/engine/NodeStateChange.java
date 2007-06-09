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


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.node.NodeState;
import org.hibernate.annotations.Type;

/**
 * Record of node state change.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="node_state_change", schema="events")
class NodeStateChange extends LogEvent
{
    private Tid tid;
    private NodeState state;

    // constructors -----------------------------------------------------------

    NodeStateChange() { }

    NodeStateChange(Tid tid, NodeState state)
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
    Tid getTid()
    {
        return tid;
    }

    void setTid(Tid tid)
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
