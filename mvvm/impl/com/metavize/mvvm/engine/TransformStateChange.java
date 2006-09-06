/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;


import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.logging.SyslogPriority;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.TransformState;

/**
 * Record of transform state change.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TRANSFORM_STATE_CHANGE"
 */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.TransformStateUserType"
     * column="STATE"
     * not-null="true"
     */
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
     * @hibernate.many-to-one
     * cascade="none"
     * column="TID"
     */
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

    public String getSyslogId()
    {
        return "Software_Appliance"; // XXX
    }

    // reuse default getSyslogPriority
}
