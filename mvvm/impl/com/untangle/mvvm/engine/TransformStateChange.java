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

package com.untangle.mvvm.engine;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.logging.SyslogBuilder;
import com.untangle.mvvm.logging.SyslogPriority;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tran.TransformState;
import org.hibernate.annotations.Type;

/**
 * Record of transform state change.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="transform_state_change", schema="events")
class TransformStateChange extends LogEvent
{
    private Tid tid;
    private TransformState state;

    // constructors -----------------------------------------------------------

    TransformStateChange() { }

    TransformStateChange(Tid tid, TransformState state)
    {
        this.tid = tid;
        this.state = state;
    }

    // accessors --------------------------------------------------------------

    /**
     * State the transform has changed into.
     *
     * @return transform state at time of log.
     */
    @Enumerated(EnumType.STRING)
    @Type(type="com.untangle.mvvm.type.TransformStateUserType")
    TransformState getState()
    {
        return state;
    }

    void setState(TransformState state)
    {
        this.state = state;
    }

    /**
     * Transform id.
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
